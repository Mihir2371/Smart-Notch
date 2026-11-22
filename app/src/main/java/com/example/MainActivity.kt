package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CapsuleStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.island.CoordinatorIslandCallbacks
import com.example.ui.island.DynamicIslandCapsule
import com.example.ui.island.computeDragPointerTargets
import com.example.ui.screens.DragAndGoStudioScreen
import com.example.ui.screens.IslandHubScreen
import com.example.ui.screens.LiveLabScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AuraIslandTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceCarbon
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private const val ROUTE_HUB = "hub"
private const val ROUTE_LIVE_LAB = "live_lab"
private const val ROUTE_DRAG_GO = "drag_go"
private const val ROUTE_SETTINGS = "settings"

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        viewModel.handleIncomingShareIntent(intent)

        setContent {
            AuraIslandTheme {
                AuraIslandAppShell(
                    activity = this,
                    viewModel = viewModel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleIncomingShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTelemetry()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AuraIslandAppShell(
    activity: ComponentActivity,
    viewModel: MainViewModel,
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val sampleImageUri by viewModel.sampleImageUri.collectAsStateWithLifecycle()
    val samplePdfUri by viewModel.samplePdfUri.collectAsStateWithLifecycle()
    val pickedPhotoUri by viewModel.pickedPhotoUri.collectAsStateWithLifecycle()

    val coordinator = viewModel.coordinator
    val resolver = viewModel.dragResolver
    val callbacks = remember(coordinator) { CoordinatorIslandCallbacks(coordinator) }
    val dragRowListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    var islandContainerWidthPx by remember { mutableFloatStateOf(0f) }

    // In-app Sandbox Drag & Go Target (mirrors the exact overlay OnDragListener behavior)
    val sandboxDragTarget = remember(coordinator, resolver) {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                if (!coordinator.settings.value.dragAndGoEnabled) return
                val androidEvent = event.toAndroidDragEvent()
                val payload = resolver.classifyClipData(androidEvent.clipDescription, null)
                coordinator.onDragStarted(payload, emptyList())
                scope.launch {
                    val targets = resolver.resolveTargets(payload)
                    coordinator.onDragTargetsResolved(targets)
                }
            }

            override fun onEntered(event: DragAndDropEvent) {
                if (!coordinator.settings.value.dragAndGoEnabled) return
                coordinator.onDragHoverIsland(true)
            }

            override fun onMoved(event: DragAndDropEvent) {
                if (!coordinator.settings.value.dragAndGoEnabled) return
                val androidEvent = event.toAndroidDragEvent()
                val (hoveredIdx, edgeDir) = computeDragPointerTargets(
                    localX = androidEvent.x,
                    containerWidthPx = islandContainerWidthPx,
                    density = density,
                    listState = dragRowListState,
                )
                coordinator.onDragPointerUpdate(hoveredIdx, edgeDir)
            }

            override fun onExited(event: DragAndDropEvent) {
                coordinator.onDragHoverIsland(false)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                if (!coordinator.settings.value.dragAndGoEnabled) return false
                val androidEvent = event.toAndroidDragEvent()
                val permissions = runCatching { activity.requestDragAndDropPermissions(androidEvent) }.getOrNull()
                val fullPayload = resolver.classifyClipData(androidEvent.clipDescription, androidEvent.clipData)
                val state = coordinator.dragState.value
                val idx = state.hoveredTargetIndex
                val target = if (idx != null) state.resolvedTargets.getOrNull(idx) else state.resolvedTargets.firstOrNull()
                val resultMsg = if (target != null) {
                    resolver.dispatchDrop(target, fullPayload).getOrElse { "Could not launch ${target.label}" }
                } else {
                    "No compatible app selected"
                }
                permissions?.release()
                coordinator.onDragCompleted(resultMsg)
                return true
            }

            override fun onEnded(event: DragAndDropEvent) {
                if (coordinator.dragState.value.isDragActive) {
                    coordinator.onDragCompleted(coordinator.dragState.value.lastActionStatus)
                }
            }
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HUB

    val tabs = remember {
        listOf(
            NavTab(ROUTE_HUB, "Hub", Icons.Filled.Home, Icons.Outlined.Home, "nav_hub"),
            NavTab(ROUTE_LIVE_LAB, "Live Lab", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_live_lab"),
            NavTab(ROUTE_DRAG_GO, "Drag & Go", Icons.Filled.DragIndicator, Icons.Outlined.DragIndicator, "nav_drag_go"),
            NavTab(ROUTE_SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings"),
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = SurfaceCarbon,
                        windowInsets = WindowInsets.navigationBars,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentCyan,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = SurfaceElevated,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = SurfaceCarbon,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.statusBars),
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = AccentCyan,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = SurfaceElevated,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }

                val isNotchMode = settings.capsuleStyle == CapsuleStyle.IPHONE_13_NOTCH
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isNotchMode) Modifier else Modifier.windowInsetsPadding(WindowInsets.statusBars)),
                ) {
                    // Top Live Interactive Dynamic Island Sandbox & Drag-and-Go Target Zone
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (isNotchMode) 0.dp else 6.dp, bottom = 10.dp)
                            .onSizeChanged { islandContainerWidthPx = it.width.toFloat() }
                            .dragAndDropTarget(
                                shouldStartDragAndDrop = { true },
                                target = sandboxDragTarget,
                            ),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (telemetry.isOverlayRunning) {
                            Text(
                                text = "System Overlay Active Above",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 10.dp),
                            )
                        } else {
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

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HUB,
                        modifier = Modifier.weight(1f),
                    ) {
                        composable(ROUTE_HUB) {
                            IslandHubScreen(
                                settings = settings,
                                telemetry = telemetry,
                                onToggleOverlay = viewModel::toggleOverlay,
                                onUpdateSettings = viewModel::updateSettings,
                                onRefreshTelemetry = viewModel::refreshTelemetry,
                            )
                        }
                        composable(ROUTE_LIVE_LAB) {
                            LiveLabScreen(
                                coordinator = coordinator,
                                snapshot = snapshot,
                            )
                        }
                        composable(ROUTE_DRAG_GO) {
                            DragAndGoStudioScreen(
                                dragState = snapshot.dragState,
                                sampleImageUri = sampleImageUri,
                                samplePdfUri = samplePdfUri,
                                pickedPhotoUri = pickedPhotoUri,
                                onPhotoPicked = viewModel::setPickedPhotoUri,
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                onUpdateSettings = viewModel::updateSettings,
                                onResetDefaults = viewModel::resetSettings,
                            )
                        }
                    }
                }
            }
        }
    }
}
