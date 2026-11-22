package com.example.ui.island

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LiveActivity
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

interface IslandCardCallbacks {
    fun onCollapse()
    fun onDismiss(id: String)
    fun onMediaPlayPause()
    fun onMediaNext()
    fun onMediaPrev()
    fun onMediaSeek(fraction: Float)
    fun onTimerTogglePause(id: String)
    fun onTimerAddMinute(id: String)
    fun onStopwatchToggle(id: String)
    fun onStopwatchLap(id: String)
    fun onCallAnswer(id: String)
    fun onCallMute(id: String)
    fun onCallSpeaker(id: String)
    fun onRecordingTogglePause(id: String)
    fun onDownloadTogglePause(id: String)
    fun onUploadTogglePause(id: String)
}

@Composable
fun ExpandedActivityCard(
    activity: LiveActivity,
    callbacks: IslandCardCallbacks,
) {
    when (activity) {
        is LiveActivity.MediaPlayback -> ExpandedMediaCard(activity, callbacks)
        is LiveActivity.TimerActivity -> ExpandedTimerCard(activity, callbacks)
        is LiveActivity.StopwatchActivity -> ExpandedStopwatchCard(activity, callbacks)
        is LiveActivity.CallActivity -> ExpandedCallCard(activity, callbacks)
        is LiveActivity.NavigationActivity -> ExpandedNavigationCard(activity, callbacks)
        is LiveActivity.VoiceRecording -> ExpandedVoiceRecordingCard(activity, callbacks)
        is LiveActivity.DownloadActivity -> ExpandedDownloadCard(activity, callbacks)
        is LiveActivity.UploadActivity -> ExpandedUploadCard(activity, callbacks)
        is LiveActivity.HotspotActivity -> ExpandedHotspotCard(activity, callbacks)
        is LiveActivity.DeliveryTracker -> ExpandedDeliveryCard(activity, callbacks)
        is LiveActivity.RideshareTracker -> ExpandedRideshareCard(activity, callbacks)
        is LiveActivity.FlightTracker -> ExpandedFlightCard(activity, callbacks)
        is LiveActivity.SportsScore -> ExpandedSportsCard(activity, callbacks)
        is LiveActivity.FileTransfer -> ExpandedAirDropCard(activity, callbacks)
        is LiveActivity.SystemNotification -> ExpandedNotificationCard(activity, callbacks)
    }
}

@Composable
private fun ExpandedMediaCard(
    media: LiveActivity.MediaPlayback,
    callbacks: IslandCardCallbacks,
) {
    val progressFraction = if (media.durationMs > 0L) {
        (media.progressMs.toFloat() / media.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (media.artworkBitmap != null) {
                Image(
                    bitmap = media.artworkBitmap.asImageBitmap(),
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_sample_album_1795341179874),
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = media.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IslandMediaVisualizer(isPlaying = media.isPlaying, barColor = AccentViolet)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatSecondsMmSs((media.progressMs / 1000L).toInt()),
                style = TabularMonoStyle.copy(fontSize = 11.sp),
                color = TextSecondary,
            )
            Slider(
                value = progressFraction,
                onValueChange = callbacks::onMediaSeek,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = TextPrimary,
                    activeTrackColor = AccentViolet,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                ),
            )
            Text(
                text = formatSecondsMmSs((media.durationMs / 1000L).toInt()),
                style = TabularMonoStyle.copy(fontSize = 11.sp),
                color = TextSecondary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = callbacks::onMediaPrev,
                modifier = Modifier.testTag("island_media_prev"),
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = TextPrimary)
            }
            IconButton(
                onClick = callbacks::onMediaPlayPause,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .testTag("island_media_play_pause"),
            ) {
                Icon(
                    imageVector = if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (media.isPlaying) "Pause" else "Play",
                    tint = TextPrimary,
                )
            }
            IconButton(
                onClick = callbacks::onMediaNext,
                modifier = Modifier.testTag("island_media_next"),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = TextPrimary)
            }
            IconButton(
                onClick = { callbacks.onDismiss(media.id) },
                modifier = Modifier.testTag("island_dismiss_media"),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun ExpandedTimerCard(
    timer: LiveActivity.TimerActivity,
    callbacks: IslandCardCallbacks,
) {
    val progress = if (timer.totalSeconds > 0) {
        timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat()
    } else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadialProgressRing(
            progress = progress,
            ringColor = AccentAmber,
            size = 52.dp,
            strokeWidth = 4.dp,
        ) {
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = timer.label,
                tint = AccentAmber,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = timer.label,
                style = MaterialTheme.typography.bodySmall,
                color = AccentAmber,
            )
            Text(
                text = formatSecondsMmSs(timer.remainingSeconds),
                style = TabularMonoStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
        }
        CircleActionBtn(
            icon = if (timer.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            tint = AccentAmber,
            bg = AccentAmber.copy(alpha = 0.2f),
            desc = if (timer.isPaused) "Resume timer" else "Pause timer",
            tag = "island_timer_pause",
            onClick = { callbacks.onTimerTogglePause(timer.id) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        CircleActionBtn(
            icon = Icons.Filled.Add,
            tint = TextPrimary,
            bg = Color.White.copy(alpha = 0.14f),
            desc = "Add 1 minute",
            tag = "island_timer_add_min",
            onClick = { callbacks.onTimerAddMinute(timer.id) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        CircleActionBtn(
            icon = Icons.Filled.Close,
            tint = AccentCoral,
            bg = AccentCoral.copy(alpha = 0.18f),
            desc = "Cancel timer",
            tag = "island_timer_cancel",
            onClick = { callbacks.onDismiss(timer.id) },
        )
    }
}

@Composable
private fun ExpandedStopwatchCard(
    sw: LiveActivity.StopwatchActivity,
    callbacks: IslandCardCallbacks,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "STOPWATCH • LAP ${sw.lapCount}",
                style = MaterialTheme.typography.labelSmall,
                color = AccentAmber,
            )
            Text(
                text = formatMillisStopwatch(sw.elapsedMillis),
                style = TabularMonoStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
        }
        CircleActionBtn(
            icon = if (sw.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            tint = AccentAmber,
            bg = AccentAmber.copy(alpha = 0.2f),
            desc = "Toggle stopwatch",
            tag = "island_sw_toggle",
            onClick = { callbacks.onStopwatchToggle(sw.id) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        CircleActionBtn(
            icon = Icons.Filled.Flag,
            tint = TextPrimary,
            bg = Color.White.copy(alpha = 0.14f),
            desc = "Lap",
            tag = "island_sw_lap",
            onClick = { callbacks.onStopwatchLap(sw.id) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        CircleActionBtn(
            icon = Icons.Filled.Close,
            tint = AccentCoral,
            bg = AccentCoral.copy(alpha = 0.18f),
            desc = "Close stopwatch",
            tag = "island_sw_close",
            onClick = { callbacks.onDismiss(sw.id) },
        )
    }
}

@Composable
private fun ExpandedCallCard(
    call: LiveActivity.CallActivity,
    callbacks: IslandCardCallbacks,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(AccentEmerald.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Call, contentDescription = "Call", tint = AccentEmerald)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.callerName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
            )
            Text(
                text = if (call.isIncoming) call.subtitle else "${call.subtitle} • ${formatSecondsMmSs(call.elapsedSeconds)}",
                style = TabularMonoStyle.copy(fontSize = 12.sp),
                color = AccentEmerald,
            )
        }
        if (call.isIncoming) {
            CircleActionBtn(
                icon = Icons.Filled.CallEnd,
                tint = Color.White,
                bg = AccentCoral,
                desc = "Decline call",
                tag = "island_call_decline",
                onClick = { callbacks.onDismiss(call.id) },
            )
            Spacer(modifier = Modifier.width(10.dp))
            CircleActionBtn(
                icon = Icons.Filled.Call,
                tint = Color.Black,
                bg = AccentEmerald,
                desc = "Answer call",
                tag = "island_call_answer",
                onClick = { callbacks.onCallAnswer(call.id) },
            )
        } else {
            CircleActionBtn(
                icon = if (call.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                tint = if (call.isMuted) AccentCoral else TextPrimary,
                bg = Color.White.copy(alpha = 0.14f),
                desc = "Mute call",
                tag = "island_call_mute",
                onClick = { callbacks.onCallMute(call.id) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            CircleActionBtn(
                icon = Icons.Filled.VolumeUp,
                tint = if (call.isSpeakerOn) AccentCyan else TextPrimary,
                bg = Color.White.copy(alpha = 0.14f),
                desc = "Speaker",
                tag = "island_call_speaker",
                onClick = { callbacks.onCallSpeaker(call.id) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            CircleActionBtn(
                icon = Icons.Filled.CallEnd,
                tint = Color.White,
                bg = AccentCoral,
                desc = "End call",
                tag = "island_call_end",
                onClick = { callbacks.onDismiss(call.id) },
            )
        }
    }
}

@Composable
private fun ExpandedNavigationCard(
    nav: LiveActivity.NavigationActivity,
    callbacks: IslandCardCallbacks,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AccentEmerald.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Navigation,
                contentDescription = nav.instruction,
                tint = AccentEmerald,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nav.distanceText,
                style = TabularMonoStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
            Text(
                text = nav.instruction,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = nav.etaText,
                style = TabularMonoStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = AccentEmerald,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dismiss",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { callbacks.onDismiss(nav.id) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun ExpandedVoiceRecordingCard(
    rec: LiveActivity.VoiceRecording,
    callbacks: IslandCardCallbacks,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AccentCoral.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.GraphicEq, contentDescription = "Recording", tint = AccentCoral)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rec.label,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = formatSecondsMmSs(rec.elapsedSeconds),
                    style = TabularMonoStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = AccentCoral,
                )
                IslandMediaVisualizer(isPlaying = !rec.isPaused, barColor = AccentCoral, barCount = 6, maxBarHeight = 12.dp)
            }
        }
        CircleActionBtn(
            icon = if (rec.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            tint = TextPrimary,
            bg = Color.White.copy(alpha = 0.14f),
            desc = "Pause recording",
            tag = "island_rec_pause",
            onClick = { callbacks.onRecordingTogglePause(rec.id) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        CircleActionBtn(
            icon = Icons.Filled.Close,
            tint = Color.White,
            bg = AccentCoral,
            desc = "Stop recording",
            tag = "island_rec_stop",
            onClick = { callbacks.onDismiss(rec.id) },
        )
    }
}

@Composable
private fun ExpandedDownloadCard(
    dl: LiveActivity.DownloadActivity,
    callbacks: IslandCardCallbacks,
) {
    val accent = if (dl.isCompleted) AccentEmerald else AccentCyan
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadialProgressRing(
                progress = dl.progressFraction,
                ringColor = accent,
                size = 44.dp,
                strokeWidth = 3.5.dp,
            ) {
                Icon(
                    imageVector = if (dl.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dl.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${dl.sourceLabel} • ${dl.speedText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                )
            }
            Text(
                text = "${(dl.progressFraction * 100).toInt()}%",
                style = TabularMonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
        }

        LinearProgressIndicator(
            progress = { dl.progressFraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accent,
            trackColor = Color.White.copy(alpha = 0.14f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${dl.transferredText} / ${dl.totalText}",
                style = TabularMonoStyle.copy(fontSize = 11.sp),
                color = TextSecondary,
            )
            if (!dl.isCompleted) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleActionBtn(
                        icon = if (dl.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        tint = AccentCyan,
                        bg = AccentCyan.copy(alpha = 0.18f),
                        desc = "Pause download",
                        tag = "island_dl_pause",
                        onClick = { callbacks.onDownloadTogglePause(dl.id) },
                    )
                    CircleActionBtn(
                        icon = Icons.Filled.Close,
                        tint = AccentCoral,
                        bg = AccentCoral.copy(alpha = 0.18f),
                        desc = "Cancel download",
                        tag = "island_dl_cancel",
                        onClick = { callbacks.onDismiss(dl.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedUploadCard(
    ul: LiveActivity.UploadActivity,
    callbacks: IslandCardCallbacks,
) {
    val accent = if (ul.isCompleted) AccentEmerald else AccentViolet
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadialProgressRing(
                progress = ul.progressFraction,
                ringColor = accent,
                size = 44.dp,
                strokeWidth = 3.5.dp,
            ) {
                Icon(
                    imageVector = if (ul.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.CloudUpload,
                    contentDescription = "Upload",
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ul.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Uploading to ${ul.targetDestination} • ${ul.speedText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                )
            }
            Text(
                text = "${(ul.progressFraction * 100).toInt()}%",
                style = TabularMonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
        }

        LinearProgressIndicator(
            progress = { ul.progressFraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accent,
            trackColor = Color.White.copy(alpha = 0.14f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${ul.transferredText} / ${ul.totalText}",
                style = TabularMonoStyle.copy(fontSize = 11.sp),
                color = TextSecondary,
            )
            if (!ul.isCompleted) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleActionBtn(
                        icon = if (ul.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        tint = AccentViolet,
                        bg = AccentViolet.copy(alpha = 0.18f),
                        desc = "Pause upload",
                        tag = "island_ul_pause",
                        onClick = { callbacks.onUploadTogglePause(ul.id) },
                    )
                    CircleActionBtn(
                        icon = Icons.Filled.Close,
                        tint = AccentCoral,
                        bg = AccentCoral.copy(alpha = 0.18f),
                        desc = "Cancel upload",
                        tag = "island_ul_cancel",
                        onClick = { callbacks.onDismiss(ul.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedHotspotCard(
    hotspot: LiveActivity.HotspotActivity,
    callbacks: IslandCardCallbacks,
) {
    // Pure iOS-style Personal Hotspot Live Activity status indicator (no quick-control toggle button inside)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AccentEmerald.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            HotspotTetheringIcon(tint = AccentEmerald, size = 26.dp, pulse = true)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hotspot.networkName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = "${hotspot.connectedDevices} ${if (hotspot.connectedDevices == 1) "Device" else "Devices"} Connected • ${hotspot.bandLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = AccentEmerald,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentEmerald.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "ACTIVE",
                    style = TabularMonoStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = AccentEmerald,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatSecondsMmSs(hotspot.elapsedSeconds),
                style = TabularMonoStyle.copy(fontSize = 12.sp),
                color = TextSecondary,
                modifier = Modifier.clickable { callbacks.onCollapse() },
            )
        }
    }
}

@Composable
private fun ExpandedDeliveryCard(
    delivery: LiveActivity.DeliveryTracker,
    callbacks: IslandCardCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalShipping, contentDescription = "Delivery", tint = AccentAmber)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(delivery.merchant, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(delivery.stageLabel, style = MaterialTheme.typography.bodySmall, color = AccentAmber)
            }
            Text(
                text = "${delivery.etaMinutes}m",
                style = TabularMonoStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = AccentAmber,
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = { callbacks.onDismiss(delivery.id) }) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = TextSecondary)
            }
        }
        LinearProgressIndicator(
            progress = { delivery.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = AccentAmber,
            trackColor = Color.White.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun ExpandedRideshareCard(
    ride: LiveActivity.RideshareTracker,
    callbacks: IslandCardCallbacks,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentCyan.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = "Rideshare", tint = AccentCyan)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${ride.vehicleModel} • ${ride.licensePlate}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text("${ride.driverName} • ${ride.statusLabel}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Text(
            text = "${ride.etaMinutes} min",
            style = TabularMonoStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = AccentCyan,
        )
        IconButton(onClick = { callbacks.onDismiss(ride.id) }) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = TextSecondary)
        }
    }
}

@Composable
private fun ExpandedFlightCard(
    flight: LiveActivity.FlightTracker,
    callbacks: IslandCardCallbacks,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.FlightTakeoff, contentDescription = "Flight", tint = AccentCyan, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${flight.originCode} → ${flight.destinationCode}",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Text(
                text = "${flight.flightCode} • Gate ${flight.gate} • ${flight.statusLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = AccentEmerald,
            )
        }
        Text(
            text = "${flight.boardingInMinutes}m",
            style = TabularMonoStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = AccentAmber,
        )
        IconButton(onClick = { callbacks.onDismiss(flight.id) }) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = TextSecondary)
        }
    }
}

@Composable
private fun ExpandedSportsCard(
    sports: LiveActivity.SportsScore,
    callbacks: IslandCardCallbacks,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.SportsSoccer, contentDescription = "Sports", tint = AccentEmerald)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(sports.league, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                text = "${sports.homeTeam}  ${sports.homeScore} — ${sports.awayScore}  ${sports.awayTeam}",
                style = TabularMonoStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
        }
        Text(
            text = sports.clockLabel,
            style = TabularMonoStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = AccentEmerald,
        )
        IconButton(onClick = { callbacks.onDismiss(sports.id) }) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = TextSecondary)
        }
    }
}

@Composable
private fun ExpandedAirDropCard(
    transfer: LiveActivity.FileTransfer,
    callbacks: IslandCardCallbacks,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadialProgressRing(
            progress = transfer.progressFraction,
            ringColor = if (transfer.isCompleted) AccentEmerald else AccentCyan,
            size = 46.dp,
            strokeWidth = 3.5.dp,
        ) {
            Icon(
                imageVector = if (transfer.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Share,
                contentDescription = "File transfer",
                tint = if (transfer.isCompleted) AccentEmerald else AccentCyan,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transfer.fileName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(
                text = if (transfer.isCompleted) "Received from ${transfer.peerName}" else "Receiving from ${transfer.peerName}",
                style = MaterialTheme.typography.bodySmall,
                color = AccentCyan,
            )
        }
        IconButton(onClick = { callbacks.onDismiss(transfer.id) }) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = TextSecondary)
        }
    }
}

@Composable
private fun ExpandedNotificationCard(
    notif: LiveActivity.SystemNotification,
    callbacks: IslandCardCallbacks,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = notif.appName, tint = AccentBlue)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${notif.appName} • ${notif.title}",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = notif.message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { callbacks.onDismiss(notif.id) }) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = TextSecondary)
        }
    }
}

@Composable
private fun CircleActionBtn(
    icon: ImageVector,
    tint: Color,
    bg: Color,
    desc: String,
    tag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}
