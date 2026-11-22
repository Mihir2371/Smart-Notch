package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.media.session.MediaController
import android.os.Debug
import android.provider.Settings
import com.example.model.DragAndGoState
import com.example.model.DragPayload
import com.example.model.DragTargetApp
import com.example.model.EdgeScrollDirection
import com.example.model.HardwareAlert
import com.example.model.IslandDisplayMode
import com.example.model.IslandSettings
import com.example.model.LiveActivity
import com.example.model.NavDirection
import com.example.model.RingerMode
import com.example.model.TelemetryStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IslandSnapshot(
    val displayMode: IslandDisplayMode = IslandDisplayMode.IDLE_PILL,
    val primaryActivity: LiveActivity? = null,
    val secondaryActivity: LiveActivity? = null,
    val expandedActivity: LiveActivity? = null,
    val activeAlert: HardwareAlert? = null,
    val dragState: DragAndGoState = DragAndGoState(),
    val privacyMicActive: Boolean = false,
    val privacyCameraActive: Boolean = false,
)

class DynamicIslandCoordinator(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _activities = MutableStateFlow<List<LiveActivity>>(emptyList())
    val activities: StateFlow<List<LiveActivity>> = _activities.asStateFlow()

    private val _expandedActivityId = MutableStateFlow<String?>(null)
    private val _activeAlert = MutableStateFlow<HardwareAlert?>(null)
    private val _dragState = MutableStateFlow(DragAndGoState())
    val dragState: StateFlow<DragAndGoState> = _dragState.asStateFlow()

    private val _settings = MutableStateFlow(IslandSettings())
    val settings: StateFlow<IslandSettings> = _settings.asStateFlow()

    private val _privacyMic = MutableStateFlow(false)
    private val _privacyCamera = MutableStateFlow(false)

    private val _isScreenOn = MutableStateFlow(true)
    private val _isOverlayRunning = MutableStateFlow(false)
    private val _telemetry = MutableStateFlow(TelemetryStats())
    val telemetry: StateFlow<TelemetryStats> = _telemetry.asStateFlow()

    private var activeMediaController: MediaController? = null
    private var tickerJob: Job? = null
    private var alertDismissJob: Job? = null
    private var autoCollapseJob: Job? = null

    val snapshot: StateFlow<IslandSnapshot> = combine(
        _activities,
        _expandedActivityId,
        _activeAlert,
        _dragState,
        _settings,
    ) { list, expandedId, alert, drag, cfg ->
        val sorted = list.sortedByDescending { it.priority }
        val primary = sorted.firstOrNull()
        val secondary = sorted.getOrNull(1)
        val expanded = expandedId?.let { id -> sorted.firstOrNull { it.id == id } }

        val mode = when {
            drag.isHoveringIsland && cfg.dragAndGoEnabled -> IslandDisplayMode.DRAG_AND_GO_DOCK
            alert != null -> IslandDisplayMode.TRANSIENT_POPUP
            expanded != null -> IslandDisplayMode.EXPANDED_CARD
            primary != null && secondary != null -> IslandDisplayMode.COMPACT_SPLIT
            primary != null -> IslandDisplayMode.COMPACT_SINGLE
            cfg.showIdlePill -> IslandDisplayMode.IDLE_PILL
            else -> IslandDisplayMode.HIDDEN
        }

        IslandSnapshot(
            displayMode = mode,
            primaryActivity = primary,
            secondaryActivity = secondary,
            expandedActivity = expanded,
            activeAlert = alert,
            dragState = drag,
            privacyMicActive = cfg.privacyDotsEnabled && _privacyMic.value,
            privacyCameraActive = cfg.privacyDotsEnabled && _privacyCamera.value,
        )
    }.stateIn(scope, SharingStarted.Eagerly, IslandSnapshot())

    fun applySettings(newSettings: IslandSettings) {
        _settings.value = newSettings
        refreshTelemetry()
    }

    fun setScreenOn(screenOn: Boolean) {
        _isScreenOn.value = screenOn
        if (screenOn) {
            ensureTickerIfNeeded()
        } else {
            tickerJob?.cancel()
            tickerJob = null
        }
        refreshTelemetry()
    }

    fun setOverlayServiceRunning(running: Boolean) {
        _isOverlayRunning.value = running
        refreshTelemetry()
    }

    fun refreshTelemetry() {
        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)
        val pssMb = memInfo.totalPss / 1024f
        val runtime = Runtime.getRuntime()
        val javaHeapMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)

        val overlayGranted = Settings.canDrawOverlays(appContext)
        val enabledListeners = Settings.Secure.getString(
            appContext.contentResolver,
            "enabled_notification_listeners",
        ) ?: ""
        val notifAccessGranted = enabledListeners.contains(appContext.packageName)

        val postNotifGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            appContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val btGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            appContext.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        _telemetry.value = TelemetryStats(
            pssMemoryMb = pssMb,
            javaHeapMb = javaHeapMb,
            isScreenOn = _isScreenOn.value,
            isTickerRunning = tickerJob?.isActive == true,
            isOverlayRunning = _isOverlayRunning.value,
            isOverlayPermissionGranted = overlayGranted,
            isNotificationAccessGranted = notifAccessGranted,
            isPostNotificationsGranted = postNotifGranted,
            isBluetoothPermissionGranted = btGranted,
        )
    }

    // --- Activity lifecycle helpers ---

    private fun upsertActivity(activity: LiveActivity, expandImmediately: Boolean = false) {
        _activities.update { list ->
            val filtered = list.filterNot { it.id == activity.id }
            filtered + activity
        }
        if (expandImmediately) {
            expandActivity(activity.id)
        }
        ensureTickerIfNeeded()
    }

    fun dismissActivity(id: String) {
        _activities.update { list -> list.filterNot { it.id == id } }
        if (_expandedActivityId.value == id) {
            _expandedActivityId.value = null
        }
        ensureTickerIfNeeded()
    }

    fun clearAll() {
        _activities.value = emptyList()
        _expandedActivityId.value = null
        _activeAlert.value = null
        _privacyMic.value = false
        _privacyCamera.value = false
        tickerJob?.cancel()
        tickerJob = null
        refreshTelemetry()
    }

    fun expandActivity(id: String) {
        _activeAlert.value = null
        _expandedActivityId.value = id
        scheduleAutoCollapse()
    }

    fun collapseIsland() {
        _expandedActivityId.value = null
        autoCollapseJob?.cancel()
    }

    fun swapSplitActivities() {
        val current = _activities.value.sortedByDescending { it.priority }
        if (current.size >= 2) {
            // Re-prioritize secondary above primary by bumping secondary via expanded preview or rotating order
            val second = current[1]
            expandActivity(second.id)
        }
    }

    private fun scheduleAutoCollapse() {
        autoCollapseJob?.cancel()
        val seconds = _settings.value.autoCollapseSeconds
        if (seconds <= 0) return
        autoCollapseJob = scope.launch {
            delay(seconds * 1000L)
            if (!_dragState.value.isHoveringIsland) {
                _expandedActivityId.value = null
            }
        }
    }

    private fun showAlert(alert: HardwareAlert, durationMs: Long = 2600L) {
        _activeAlert.value = alert
        alertDismissJob?.cancel()
        alertDismissJob = scope.launch {
            delay(durationMs)
            if (_activeAlert.value?.id == alert.id) {
                _activeAlert.value = null
            }
        }
    }

    // --- Screen-aware zero-wakeup coroutine ticker ---

    private fun needsTicker(list: List<LiveActivity>): Boolean = list.any {
        when (it) {
            is LiveActivity.MediaPlayback -> it.isPlaying
            is LiveActivity.TimerActivity -> !it.isPaused && it.remainingSeconds > 0
            is LiveActivity.StopwatchActivity -> it.isRunning
            is LiveActivity.CallActivity -> !it.isIncoming
            is LiveActivity.VoiceRecording -> !it.isPaused
            is LiveActivity.DownloadActivity -> !it.isPaused && !it.isCompleted
            is LiveActivity.UploadActivity -> !it.isPaused && !it.isCompleted
            is LiveActivity.HotspotActivity -> true
            is LiveActivity.FileTransfer -> !it.isCompleted
            is LiveActivity.DeliveryTracker -> it.progressFraction < 1f
            else -> false
        }
    }

    private fun ensureTickerIfNeeded() {
        if (!_isScreenOn.value || !needsTicker(_activities.value)) {
            tickerJob?.cancel()
            tickerJob = null
            refreshTelemetry()
            return
        }
        if (tickerJob?.isActive == true) return

        tickerJob = scope.launch {
            var tickCount = 0
            while (_isScreenOn.value && needsTicker(_activities.value)) {
                val intervalMs = if (_settings.value.batterySaverMode) 1000L else 500L
                delay(intervalMs)
                tickCount++
                val isFullSecond = _settings.value.batterySaverMode || tickCount % 2 == 0
                val deltaSec = if (isFullSecond) 1 else 0
                val deltaMs = intervalMs

                var completedTransferIds = emptyList<String>()

                _activities.update { current ->
                    current.mapNotNull { act ->
                        when (act) {
                            is LiveActivity.MediaPlayback -> if (act.isPlaying) {
                                val nextMs = (act.progressMs + deltaMs).coerceAtMost(act.durationMs)
                                act.copy(progressMs = if (nextMs >= act.durationMs) 0L else nextMs)
                            } else act

                            is LiveActivity.TimerActivity -> if (!act.isPaused && deltaSec > 0) {
                                val next = (act.remainingSeconds - deltaSec).coerceAtLeast(0)
                                act.copy(remainingSeconds = next)
                            } else act

                            is LiveActivity.StopwatchActivity -> if (act.isRunning) {
                                act.copy(elapsedMillis = act.elapsedMillis + deltaMs)
                            } else act

                            is LiveActivity.CallActivity -> if (!act.isIncoming && deltaSec > 0) {
                                act.copy(elapsedSeconds = act.elapsedSeconds + deltaSec)
                            } else act

                            is LiveActivity.VoiceRecording -> if (!act.isPaused && deltaSec > 0) {
                                val pulse = 0.35f + ((tickCount % 5) * 0.13f)
                                act.copy(
                                    elapsedSeconds = act.elapsedSeconds + deltaSec,
                                    amplitudeLevel = pulse,
                                )
                            } else act

                            is LiveActivity.HotspotActivity -> if (deltaSec > 0) {
                                act.copy(elapsedSeconds = act.elapsedSeconds + deltaSec)
                            } else act

                            is LiveActivity.DownloadActivity -> if (!act.isPaused && !act.isCompleted) {
                                val next = (act.progressFraction + 0.035f).coerceAtMost(1f)
                                val done = next >= 1f
                                if (done) completedTransferIds = completedTransferIds + act.id
                                val totalMb = 148.0
                                val doneMb = (totalMb * next)
                                act.copy(
                                    progressFraction = next,
                                    transferredText = String.format("%.1f MB", doneMb),
                                    speedText = if (done) "Installed" else "14.8 MB/s",
                                    isCompleted = done,
                                )
                            } else act

                            is LiveActivity.UploadActivity -> if (!act.isPaused && !act.isCompleted) {
                                val next = (act.progressFraction + 0.04f).coerceAtMost(1f)
                                val done = next >= 1f
                                if (done) completedTransferIds = completedTransferIds + act.id
                                val totalMb = 64.0
                                val doneMb = (totalMb * next)
                                act.copy(
                                    progressFraction = next,
                                    transferredText = String.format("%.1f MB", doneMb),
                                    speedText = if (done) "Uploaded" else "8.4 MB/s",
                                    isCompleted = done,
                                )
                            } else act

                            is LiveActivity.FileTransfer -> if (!act.isCompleted) {
                                val next = (act.progressFraction + 0.06f).coerceAtMost(1f)
                                val done = next >= 1f
                                if (done) completedTransferIds = completedTransferIds + act.id
                                act.copy(progressFraction = next, isCompleted = done)
                            } else act

                            else -> act
                        }
                    }
                }

                if (completedTransferIds.isNotEmpty()) {
                    completedTransferIds.forEach { doneId ->
                        scope.launch {
                            delay(2800L)
                            dismissActivity(doneId)
                        }
                    }
                }

                if (!needsTicker(_activities.value)) break
            }
            tickerJob = null
            refreshTelemetry()
        }
        refreshTelemetry()
    }

    // --- Media & System MediaSession Hooks ---

    fun bindSystemMediaController(controller: MediaController?, title: String?, artist: String?, isPlaying: Boolean, positionMs: Long, durationMs: Long, artwork: Bitmap?) {
        activeMediaController = controller
        if (controller == null || title.isNullOrBlank()) {
            val existing = _activities.value.firstOrNull { it.id == "media_active" } as? LiveActivity.MediaPlayback
            if (existing?.isSystemSession == true) {
                dismissActivity("media_active")
            }
            return
        }
        upsertActivity(
            LiveActivity.MediaPlayback(
                title = title,
                artist = artist ?: "Unknown Artist",
                isPlaying = isPlaying,
                progressMs = positionMs.coerceAtLeast(0L),
                durationMs = durationMs.coerceAtLeast(1000L),
                artworkBitmap = artwork,
                isSystemSession = true,
            ),
        )
    }

    fun triggerSandboxMedia() {
        activeMediaController = null
        upsertActivity(
            LiveActivity.MediaPlayback(
                title = "Midnight Horizon",
                artist = "Chromatics & Aura",
                album = "Obsidian Waves",
                isPlaying = true,
                progressMs = 42_000L,
                durationMs = 218_000L,
                isSystemSession = false,
            ),
        )
    }

    fun toggleMediaPlayPause() {
        val current = _activities.value.firstOrNull { it.id == "media_active" } as? LiveActivity.MediaPlayback ?: return
        if (current.isSystemSession && activeMediaController != null) {
            if (current.isPlaying) {
                activeMediaController?.transportControls?.pause()
            } else {
                activeMediaController?.transportControls?.play()
            }
        }
        upsertActivity(current.copy(isPlaying = !current.isPlaying))
        scheduleAutoCollapse()
    }

    fun mediaSkipNext() {
        val current = _activities.value.firstOrNull { it.id == "media_active" } as? LiveActivity.MediaPlayback ?: return
        if (current.isSystemSession && activeMediaController != null) {
            activeMediaController?.transportControls?.skipToNext()
        } else {
            val tracks = listOf(
                "Midnight Horizon" to "Chromatics & Aura",
                "Neon Drift" to "Kavinsky",
                "Glass Pulse" to "ODESZA",
            )
            val idx = (tracks.indexOfFirst { it.first == current.title } + 1) % tracks.size
            upsertActivity(current.copy(title = tracks[idx].first, artist = tracks[idx].second, progressMs = 0L, isPlaying = true))
        }
        scheduleAutoCollapse()
    }

    fun mediaSkipPrev() {
        val current = _activities.value.firstOrNull { it.id == "media_active" } as? LiveActivity.MediaPlayback ?: return
        if (current.isSystemSession && activeMediaController != null) {
            activeMediaController?.transportControls?.skipToPrevious()
        } else {
            upsertActivity(current.copy(progressMs = 0L, isPlaying = true))
        }
        scheduleAutoCollapse()
    }

    fun mediaSeekTo(fraction: Float) {
        val current = _activities.value.firstOrNull { it.id == "media_active" } as? LiveActivity.MediaPlayback ?: return
        val targetMs = (current.durationMs * fraction.coerceIn(0f, 1f)).toLong()
        if (current.isSystemSession && activeMediaController != null) {
            activeMediaController?.transportControls?.seekTo(targetMs)
        }
        upsertActivity(current.copy(progressMs = targetMs))
        scheduleAutoCollapse()
    }

    // --- Timers & Stopwatch ---

    fun startTimer(seconds: Int = 300, label: String = "Focus Timer") {
        upsertActivity(
            LiveActivity.TimerActivity(
                label = label,
                remainingSeconds = seconds,
                totalSeconds = seconds,
                isPaused = false,
            ),
        )
    }

    fun toggleTimerPause(id: String) {
        val current = _activities.value.firstOrNull { it.id == id } as? LiveActivity.TimerActivity ?: return
        upsertActivity(current.copy(isPaused = !current.isPaused))
        scheduleAutoCollapse()
    }

    fun addTimerMinute(id: String) {
        val current = _activities.value.firstOrNull { it.id == id } as? LiveActivity.TimerActivity ?: return
        upsertActivity(
            current.copy(
                remainingSeconds = current.remainingSeconds + 60,
                totalSeconds = current.totalSeconds + 60,
            ),
        )
        scheduleAutoCollapse()
    }

    fun startStopwatch() {
        upsertActivity(LiveActivity.StopwatchActivity(elapsedMillis = 0L, isRunning = true, lapCount = 1))
    }

    fun toggleStopwatch(id: String) {
        val current = _activities.value.firstOrNull { it.id == id } as? LiveActivity.StopwatchActivity ?: return
        upsertActivity(current.copy(isRunning = !current.isRunning))
        scheduleAutoCollapse()
    }

    fun lapStopwatch(id: String) {
        val current = _activities.value.firstOrNull { it.id == id } as? LiveActivity.StopwatchActivity ?: return
        upsertActivity(current.copy(lapCount = current.lapCount + 1))
        scheduleAutoCollapse()
    }

    // --- Calls & Voice Memo ---

    fun triggerCall(incoming: Boolean) {
        upsertActivity(
            LiveActivity.CallActivity(
                callerName = if (incoming) "Alex Rivera" else "Maya Lin",
                subtitle = if (incoming) "iPhone Audio" else "Mobile HD",
                elapsedSeconds = if (incoming) 0 else 18,
                isIncoming = incoming,
            ),
            expandImmediately = incoming,
        )
    }

    fun answerCall(id: String) {
        val current = _activities.value.firstOrNull { it.id == id } as? LiveActivity.CallActivity ?: return
        upsertActivity(current.copy(isIncoming = false, subtitle = "Connected"))
    }

    fun toggleCallMute(id: String) {
        val current = _activities.value.firstOrNull { it.id == id } as? LiveActivity.CallActivity ?: return
        upsertActivity(current.copy(isMuted = !current.isMuted))
    }

    fun toggleCallSpeaker(id: String) {
        val current = _activities.value.firstOrNull { it.id == id } as? LiveActivity.CallActivity ?: return
        upsertActivity(current.copy(isSpeakerOn = !current.isSpeakerOn))
    }

    fun triggerVoiceRecording() {
        _privacyMic.value = true
        upsertActivity(
            LiveActivity.VoiceRecording(
                label = "Voice Memo #04",
                elapsedSeconds = 0,
                isPaused = false,
            ),
        )
    }

    fun toggleRecordingPause(id: String) {
        val current = _activities.value.firstOrNull { it.id == id } as? LiveActivity.VoiceRecording ?: return
        val nextPaused = !current.isPaused
        _privacyMic.value = !nextPaused
        upsertActivity(current.copy(isPaused = nextPaused))
    }

    // --- Navigation ---

    fun triggerNavigation() {
        upsertActivity(
            LiveActivity.NavigationActivity(
                direction = NavDirection.RIGHT,
                instruction = "Turn right onto Shoreline Blvd",
                streetName = "Shoreline Blvd",
                distanceText = "320 m",
                etaText = "12 min",
                remainingMinutes = 12,
            ),
        )
    }

    // --- File & App Downloading / Uploading Live Activities ---

    fun triggerFileAppDownload(isAppInstall: Boolean = true) {
        upsertActivity(
            LiveActivity.DownloadActivity(
                itemName = if (isAppInstall) "Figma Design 2.4" else "Project_Specs_v3.zip",
                sourceLabel = if (isAppInstall) "Play Store" else "Chrome Download",
                isAppInstall = isAppInstall,
                progressFraction = 0.14f,
                speedText = "14.8 MB/s",
                transferredText = "20.7 MB",
                totalText = "148.0 MB",
            ),
        )
    }

    fun triggerFileAppUpload() {
        upsertActivity(
            LiveActivity.UploadActivity(
                itemName = "Design_Reel_4K.mp4",
                targetDestination = "Cloud Drive",
                progressFraction = 0.12f,
                speedText = "8.4 MB/s",
                transferredText = "7.7 MB",
                totalText = "64.0 MB",
            ),
        )
    }

    fun toggleDownloadPause(id: String) {
        val current = _activities.value.firstOrNull { it.id == id } as? LiveActivity.DownloadActivity ?: return
        upsertActivity(current.copy(isPaused = !current.isPaused))
        scheduleAutoCollapse()
    }

    fun toggleUploadPause(id: String) {
        val current = _activities.value.firstOrNull { it.id == id } as? LiveActivity.UploadActivity ?: return
        upsertActivity(current.copy(isPaused = !current.isPaused))
        scheduleAutoCollapse()
    }

    fun updateFromSystemDownloadNotification(key: String, title: String, sourceApp: String, progress: Int, max: Int, isUpload: Boolean) {
        if (!_settings.value.downloadAutoTrack) return
        val fraction = if (max > 0) (progress.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0.5f
        val isDone = max > 0 && progress >= max
        if (isUpload) {
            upsertActivity(
                LiveActivity.UploadActivity(
                    id = "sys_upload_$key",
                    itemName = title,
                    targetDestination = sourceApp,
                    progressFraction = fraction,
                    speedText = if (isDone) "Uploaded" else "Active",
                    transferredText = "$progress%",
                    totalText = "100%",
                    isCompleted = isDone,
                ),
            )
        } else {
            upsertActivity(
                LiveActivity.DownloadActivity(
                    id = "sys_download_$key",
                    itemName = title,
                    sourceLabel = sourceApp,
                    isAppInstall = sourceApp.contains("Play", ignoreCase = true),
                    progressFraction = fraction,
                    speedText = if (isDone) "Complete" else "Active",
                    transferredText = "$progress%",
                    totalText = "100%",
                    isCompleted = isDone,
                ),
            )
        }
        if (isDone) {
            val targetId = if (isUpload) "sys_upload_$key" else "sys_download_$key"
            scope.launch {
                delay(2800L)
                dismissActivity(targetId)
            }
        }
    }

    // --- iOS-style Personal Hotspot Live Activity (On / Off) ---

    fun setHotspotActive(enabled: Boolean, connectedDevices: Int = 2, fromSystem: Boolean = false) {
        if (fromSystem && !_settings.value.hotspotAutoTrack) return
        if (enabled) {
            upsertActivity(
                LiveActivity.HotspotActivity(
                    networkName = "Personal Hotspot",
                    connectedDevices = connectedDevices.coerceAtLeast(1),
                    elapsedSeconds = 0,
                    bandLabel = "5 GHz",
                    isSystemSynced = fromSystem,
                ),
            )
            showAlert(HardwareAlert.HotspotToggled(isEnabled = true, connectedDevices = connectedDevices), 2200L)
        } else {
            dismissActivity("hotspot_active")
            showAlert(HardwareAlert.HotspotToggled(isEnabled = false, connectedDevices = 0), 2200L)
        }
    }

    fun isHotspotActive(): Boolean = _activities.value.any { it.id == "hotspot_active" }

    // --- Remaining iOS Live Activity Trackers & Notifications ---

    fun triggerDeliveryTracker() {
        upsertActivity(
            LiveActivity.DeliveryTracker(
                merchant = "Blue Bottle Coffee",
                stageLabel = "Courier is 2 stops away",
                stageIndex = 2,
                etaMinutes = 6,
                progressFraction = 0.72f,
            ),
        )
    }

    fun triggerRideshareTracker() {
        upsertActivity(
            LiveActivity.RideshareTracker(
                driverName = "Marcus • 4.9★",
                vehicleModel = "Midnight Model Y",
                licensePlate = "8AUR42",
                etaMinutes = 3,
                statusLabel = "Arriving shortly",
            ),
        )
    }

    fun triggerFlightTracker() {
        upsertActivity(
            LiveActivity.FlightTracker(
                flightCode = "UA 418",
                originCode = "SFO",
                destinationCode = "NRT",
                gate = "G14",
                statusLabel = "Boarding Now",
                boardingInMinutes = 14,
            ),
        )
    }

    fun triggerSportsScore() {
        upsertActivity(
            LiveActivity.SportsScore(
                league = "UCL • Live",
                homeTeam = "ARS",
                awayTeam = "FCB",
                homeScore = 2,
                awayScore = 2,
                clockLabel = "78'",
            ),
        )
    }

    fun triggerAirDropTransfer() {
        upsertActivity(
            LiveActivity.FileTransfer(
                fileName = "Aurora_RAW_09.dng",
                peerName = "Elena's Pixel",
                progressFraction = 0.1f,
            ),
        )
    }

    fun triggerDualActivityPreset() {
        setHotspotActive(enabled = true, connectedDevices = 2, fromSystem = false)
        triggerFileAppDownload(isAppInstall = true)
    }

    fun pushSystemNotification(packageName: String, appName: String, title: String, message: String) {
        upsertActivity(
            LiveActivity.SystemNotification(
                id = "notif_${packageName}_${System.currentTimeMillis() / 10000}",
                packageName = packageName,
                appName = appName,
                title = title,
                message = message,
                timestampMillis = System.currentTimeMillis(),
            ),
            expandImmediately = false,
        )
    }

    // --- Hardware & System Alerts ---

    fun triggerChargingAlert(percent: Int, isCharging: Boolean, isFast: Boolean) {
        if (isCharging) {
            showAlert(
                HardwareAlert.ChargingStatus(
                    batteryPercent = percent,
                    isCharging = true,
                    isFastCharging = isFast,
                ),
                2600L,
            )
        } else if (percent <= 20) {
            showAlert(HardwareAlert.LowBattery(batteryPercent = percent), 2800L)
        }
    }

    fun triggerLowBatteryAlert(percent: Int = 15) {
        showAlert(HardwareAlert.LowBattery(batteryPercent = percent), 2800L)
    }

    fun triggerRingerModeAlert(mode: RingerMode) {
        showAlert(HardwareAlert.RingerModeChanged(mode = mode), 2200L)
    }

    fun triggerBluetoothAlert(deviceName: String = "Aura Buds Pro", batteryPercent: Int = 88, connected: Boolean = true) {
        showAlert(
            HardwareAlert.BluetoothDevice(
                deviceName = deviceName,
                batteryPercent = batteryPercent,
                isConnected = connected,
            ),
            2600L,
        )
    }

    fun triggerFocusModeAlert(modeName: String = "Do Not Disturb", enabled: Boolean = true) {
        showAlert(HardwareAlert.FocusMode(modeName = modeName, isEnabled = enabled), 2400L)
    }

    fun triggerPrivacyDots(mic: Boolean, camera: Boolean) {
        _privacyMic.value = mic
        _privacyCamera.value = camera
        showAlert(HardwareAlert.PrivacyIndicator(micActive = mic, cameraActive = camera), 2200L)
    }

    // --- OriginOS Drag & Go State Transitions ---

    fun onDragStarted(payload: DragPayload, targets: List<DragTargetApp>) {
        _dragState.value = DragAndGoState(
            isDragActive = true,
            isHoveringIsland = false,
            payload = payload,
            resolvedTargets = targets,
            hoveredTargetIndex = null,
            edgeScrollDirection = EdgeScrollDirection.NONE,
        )
    }

    fun onDragHoverIsland(hovering: Boolean) {
        _dragState.update { state ->
            state.copy(
                isHoveringIsland = hovering,
                hoveredTargetIndex = if (hovering) state.hoveredTargetIndex else null,
                edgeScrollDirection = if (hovering) state.edgeScrollDirection else EdgeScrollDirection.NONE,
            )
        }
    }

    fun onDragTargetsResolved(targets: List<DragTargetApp>) {
        _dragState.update { it.copy(resolvedTargets = targets) }
    }

    fun onDragPointerUpdate(hoveredIndex: Int?, edgeDirection: EdgeScrollDirection) {
        _dragState.update {
            it.copy(
                hoveredTargetIndex = hoveredIndex,
                edgeScrollDirection = edgeDirection,
            )
        }
    }

    fun onDragCompleted(statusMessage: String?) {
        _dragState.value = DragAndGoState(
            isDragActive = false,
            isHoveringIsland = false,
            lastActionStatus = statusMessage,
        )
    }
}
