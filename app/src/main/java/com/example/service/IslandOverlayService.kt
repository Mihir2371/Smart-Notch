package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.AuraIslandApplication
import com.example.MainActivity
import com.example.R
import com.example.model.CapsuleStyle
import com.example.model.DragPayload
import com.example.model.EdgeScrollDirection
import com.example.ui.island.CoordinatorIslandCallbacks
import com.example.ui.island.DynamicIslandCapsule
import com.example.ui.island.computeDragPointerTargets
import com.example.ui.theme.AuraIslandTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IslandOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var windowManager: WindowManager? = null
    private var rootContainer: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val dragRowListState = LazyListState()

    private var activeDragPayload: DragPayload? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        startForegroundSafely()

        val app = application as AuraIslandApplication
        app.coordinator.setOverlayServiceRunning(true)

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        attachOverlayWindow(app)
    }

    private fun startForegroundSafely() {
        val channelId = "aura_island_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                channelId,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = getString(R.string.overlay_channel_description)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Dynamic Island & Drag & Go active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(launchIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun attachOverlayWindow(app: AuraIslandApplication) {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val coordinator = app.coordinator
        val resolver = app.dragResolver
        val callbacks = CoordinatorIslandCallbacks(coordinator)

        val overlayFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            overlayFlags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            val initialCfg = coordinator.settings.value
            y = if (initialCfg.capsuleStyle == CapsuleStyle.IPHONE_13_NOTCH) {
                0
            } else {
                (initialCfg.offsetYdp * resources.displayMetrics.density).toInt()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        layoutParams = params

        val container = FrameLayout(this).apply {
            setViewTreeLifecycleOwner(this@IslandOverlayService)
            setViewTreeViewModelStoreOwner(this@IslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@IslandOverlayService)
        }

        // Native system-wide Drag & Go listener on the overlay window
        container.setOnDragListener { view, event ->
            if (!coordinator.settings.value.dragAndGoEnabled) return@setOnDragListener false
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    val payload = resolver.classifyClipData(event.clipDescription, null)
                    activeDragPayload = payload
                    coordinator.onDragStarted(payload, emptyList())
                    serviceScope.launch {
                        val targets = resolver.resolveTargets(payload)
                        coordinator.onDragTargetsResolved(targets)
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    coordinator.onDragHoverIsland(true)
                    true
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    val (hoveredIdx, edgeDir) = computeDragPointerTargets(
                        localX = event.x,
                        containerWidthPx = view.width.toFloat(),
                        density = resources.displayMetrics.density,
                        listState = dragRowListState,
                    )
                    coordinator.onDragPointerUpdate(hoveredIdx, edgeDir)
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    coordinator.onDragHoverIsland(false)
                    true
                }
                DragEvent.ACTION_DROP -> {
                    val fullPayload = resolver.classifyClipData(event.clipDescription, event.clipData)
                    val state = coordinator.dragState.value
                    val idx = state.hoveredTargetIndex
                    val target = if (idx != null) state.resolvedTargets.getOrNull(idx) else state.resolvedTargets.firstOrNull()
                    val statusMsg = if (target != null) {
                        resolver.dispatchDrop(target, fullPayload).getOrElse { "Could not open ${target.label}" }
                    } else {
                        "No compatible app selected"
                    }
                    coordinator.onDragCompleted(statusMsg)
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    activeDragPayload = null
                    if (coordinator.dragState.value.isDragActive) {
                        coordinator.onDragCompleted(coordinator.dragState.value.lastActionStatus)
                    }
                    true
                }
                else -> false
            }
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                AuraIslandTheme {
                    val snapshot by coordinator.snapshot.collectAsState()
                    val settings by coordinator.settings.collectAsState()
                    val isNotch = settings.capsuleStyle == CapsuleStyle.IPHONE_13_NOTCH
                    Box(
                        modifier = Modifier.padding(
                            start = 8.dp,
                            end = 8.dp,
                            top = if (isNotch) 0.dp else 2.dp,
                            bottom = 2.dp,
                        ),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        DynamicIslandCapsule(
                            snapshot = snapshot,
                            settings = settings,
                            callbacks = callbacks,
                            onExpandActivity = coordinator::expandActivity,
                            onSwapSplit = coordinator::swapSplitActivities,
                            dragRowListState = dragRowListState,
                        )
                    }
                }
            }
        }
        container.addView(composeView)
        rootContainer = container

        runCatching {
            wm.addView(container, params)
        }.onFailure {
            stopSelf()
            return
        }

        // Reactively sync X/Y cutout calibration changes to WindowManager params
        serviceScope.launch {
            coordinator.settings.collectLatest { cfg ->
                val p = layoutParams ?: return@collectLatest
                val c = rootContainer ?: return@collectLatest
                val density = resources.displayMetrics.density
                p.x = (cfg.offsetXdp * density).toInt()
                p.y = if (cfg.capsuleStyle == CapsuleStyle.IPHONE_13_NOTCH) {
                    0
                } else {
                    (cfg.offsetYdp * density).toInt()
                }
                runCatching { windowManager?.updateViewLayout(c, p) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as? AuraIslandApplication)?.coordinator?.setOverlayServiceRunning(false)
        serviceScope.cancel()
        rootContainer?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        rootContainer = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, IslandOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, IslandOverlayService::class.java))
        }
    }
}
