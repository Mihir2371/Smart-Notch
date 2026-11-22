package com.example.ui.island

import com.example.core.DynamicIslandCoordinator

class CoordinatorIslandCallbacks(
    private val coordinator: DynamicIslandCoordinator,
) : IslandCardCallbacks {
    override fun onCollapse() = coordinator.collapseIsland()
    override fun onDismiss(id: String) = coordinator.dismissActivity(id)
    override fun onMediaPlayPause() = coordinator.toggleMediaPlayPause()
    override fun onMediaNext() = coordinator.mediaSkipNext()
    override fun onMediaPrev() = coordinator.mediaSkipPrev()
    override fun onMediaSeek(fraction: Float) = coordinator.mediaSeekTo(fraction)
    override fun onTimerTogglePause(id: String) = coordinator.toggleTimerPause(id)
    override fun onTimerAddMinute(id: String) = coordinator.addTimerMinute(id)
    override fun onStopwatchToggle(id: String) = coordinator.toggleStopwatch(id)
    override fun onStopwatchLap(id: String) = coordinator.lapStopwatch(id)
    override fun onCallAnswer(id: String) = coordinator.answerCall(id)
    override fun onCallMute(id: String) = coordinator.toggleCallMute(id)
    override fun onCallSpeaker(id: String) = coordinator.toggleCallSpeaker(id)
    override fun onRecordingTogglePause(id: String) = coordinator.toggleRecordingPause(id)
    override fun onDownloadTogglePause(id: String) = coordinator.toggleDownloadPause(id)
    override fun onUploadTogglePause(id: String) = coordinator.toggleUploadPause(id)
}
