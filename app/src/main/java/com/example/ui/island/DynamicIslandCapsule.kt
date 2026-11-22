package com.example.ui.island

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.IslandSnapshot
import com.example.model.HardwareAlert
import com.example.model.IslandDisplayMode
import com.example.model.IslandSettings
import com.example.model.LiveActivity
import com.example.model.RingerMode
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.IslandBlack
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DynamicIslandCapsule(
    snapshot: IslandSnapshot,
    settings: IslandSettings,
    callbacks: IslandCardCallbacks,
    onExpandActivity: (String) -> Unit,
    onSwapSplit: () -> Unit,
    dragRowListState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    if (snapshot.displayMode == IslandDisplayMode.HIDDEN && !snapshot.dragState.isDragActive) {
        return
    }

    val stiffness = (380f * settings.springSpeedFactor).coerceIn(220f, 650f)
    val damping = if (snapshot.displayMode == IslandDisplayMode.IDLE_PILL) {
        Spring.DampingRatioNoBouncy
    } else {
        0.68f
    }

    val targetWidth: Dp
    val targetHeight: Dp
    val targetCorner: Dp

    when (snapshot.displayMode) {
        IslandDisplayMode.HIDDEN, IslandDisplayMode.IDLE_PILL -> {
            targetWidth = if (snapshot.dragState.isDragActive) 156.dp else settings.baseWidthDp.dp
            targetHeight = if (snapshot.dragState.isDragActive) 40.dp else settings.baseHeightDp.dp
            targetCorner = settings.cornerRadiusDp.dp
        }
        IslandDisplayMode.COMPACT_SINGLE -> {
            targetWidth = 224.dp
            targetHeight = 38.dp
            targetCorner = 22.dp
        }
        IslandDisplayMode.COMPACT_SPLIT -> {
            targetWidth = 200.dp
            targetHeight = 38.dp
            targetCorner = 22.dp
        }
        IslandDisplayMode.TRANSIENT_POPUP -> {
            targetWidth = 246.dp
            targetHeight = 46.dp
            targetCorner = 24.dp
        }
        IslandDisplayMode.EXPANDED_CARD -> {
            targetWidth = 356.dp
            targetHeight = when (snapshot.expandedActivity) {
                is LiveActivity.MediaPlayback -> 178.dp
                is LiveActivity.DownloadActivity, is LiveActivity.UploadActivity -> 144.dp
                is LiveActivity.DeliveryTracker -> 104.dp
                else -> 96.dp
            }
            targetCorner = 34.dp
        }
        IslandDisplayMode.DRAG_AND_GO_DOCK -> {
            targetWidth = 364.dp
            targetHeight = 134.dp
            targetCorner = 32.dp
        }
    }

    val animWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
        label = "island_width",
    )
    val animHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
        label = "island_height",
    )
    val animCorner by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
        label = "island_corner",
    )

    val borderColor = when {
        snapshot.displayMode == IslandDisplayMode.DRAG_AND_GO_DOCK -> AccentCyan.copy(alpha = 0.75f)
        snapshot.dragState.isDragActive -> AccentCyan.copy(alpha = 0.65f)
        snapshot.displayMode == IslandDisplayMode.EXPANDED_CARD -> BorderHairline
        else -> Color.White.copy(alpha = 0.08f)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Main OLED True-Black Capsule
        Box(
            modifier = Modifier
                .width(animWidth)
                .height(animHeight)
                .clip(RoundedCornerShape(animCorner))
                .background(IslandBlack)
                .border(1.dp, borderColor, RoundedCornerShape(animCorner))
                .pointerInput(snapshot.displayMode) {
                    if (snapshot.displayMode == IslandDisplayMode.EXPANDED_CARD) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -18f) {
                                callbacks.onCollapse()
                            }
                        }
                    }
                }
                .clickable(
                    enabled = snapshot.primaryActivity != null || snapshot.displayMode == IslandDisplayMode.EXPANDED_CARD,
                ) {
                    when (snapshot.displayMode) {
                        IslandDisplayMode.COMPACT_SINGLE, IslandDisplayMode.COMPACT_SPLIT -> {
                            snapshot.primaryActivity?.let { onExpandActivity(it.id) }
                        }
                        IslandDisplayMode.EXPANDED_CARD -> callbacks.onCollapse()
                        else -> Unit
                    }
                }
                .testTag("dynamic_island_capsule"),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = snapshot.displayMode,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.94f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.94f))
                },
                label = "island_mode_content",
            ) { mode ->
                when (mode) {
                    IslandDisplayMode.HIDDEN, IslandDisplayMode.IDLE_PILL -> {
                        IdleCapsuleContent(
                            isDragActive = snapshot.dragState.isDragActive,
                            micActive = snapshot.privacyMicActive,
                            cameraActive = snapshot.privacyCameraActive,
                        )
                    }
                    IslandDisplayMode.COMPACT_SINGLE, IslandDisplayMode.COMPACT_SPLIT -> {
                        val primary = snapshot.primaryActivity
                        if (primary != null) {
                            CompactCapsuleContent(activity = primary)
                        }
                    }
                    IslandDisplayMode.TRANSIENT_POPUP -> {
                        snapshot.activeAlert?.let { alert ->
                            TransientPopupContent(alert = alert)
                        }
                    }
                    IslandDisplayMode.EXPANDED_CARD -> {
                        snapshot.expandedActivity?.let { expanded ->
                            ExpandedActivityCard(activity = expanded, callbacks = callbacks)
                        }
                    }
                    IslandDisplayMode.DRAG_AND_GO_DOCK -> {
                        DragAndGoDockContent(
                            dragState = snapshot.dragState,
                            edgeScrollSpeed = settings.edgeScrollSpeed,
                            hapticsEnabled = settings.hapticFeedbackEnabled,
                            listState = dragRowListState,
                        )
                    }
                }
            }
        }

        // Detached Split-Bubble for Dual Simultaneous Live Activities (iOS signature animation)
        AnimatedVisibility(
            visible = snapshot.displayMode == IslandDisplayMode.COMPACT_SPLIT && snapshot.secondaryActivity != null,
            enter = fadeIn() + scaleIn(
                initialScale = 0.4f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
            ),
            exit = fadeOut() + scaleOut(targetScale = 0.4f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(IslandBlack)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .clickable { onSwapSplit() }
                        .testTag("dynamic_island_split_bubble"),
                    contentAlignment = Alignment.Center,
                ) {
                    snapshot.secondaryActivity?.let { sec ->
                        SplitBubbleGlyph(activity = sec)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleCapsuleContent(
    isDragActive: Boolean,
    micActive: Boolean,
    cameraActive: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (isDragActive) {
            Text(
                text = "Drop here",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AccentCyan,
            )
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = "Drag & Go Ready",
                tint = AccentCyan,
                modifier = Modifier.size(14.dp),
            )
        } else {
            Spacer(modifier = Modifier.width(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (micActive) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AccentAmber),
                    )
                }
                if (cameraActive) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactCapsuleContent(activity: LiveActivity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        when (activity) {
            is LiveActivity.MediaPlayback -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (activity.artworkBitmap != null) {
                        Image(
                            bitmap = activity.artworkBitmap.asImageBitmap(),
                            contentDescription = activity.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_sample_album_1795341179874),
                            contentDescription = activity.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                    }
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IslandMediaVisualizer(isPlaying = activity.isPlaying, barColor = AccentViolet, maxBarHeight = 13.dp)
            }

            is LiveActivity.TimerActivity -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Timer, contentDescription = "Timer", tint = AccentAmber, modifier = Modifier.size(18.dp))
                    Text(activity.label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                }
                Text(
                    text = formatSecondsMmSs(activity.remainingSeconds),
                    style = TabularMonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = AccentAmber,
                )
            }

            is LiveActivity.StopwatchActivity -> {
                Icon(Icons.Filled.Timer, contentDescription = "Stopwatch", tint = AccentAmber, modifier = Modifier.size(18.dp))
                Text(
                    text = formatMillisStopwatch(activity.elapsedMillis),
                    style = TabularMonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = AccentAmber,
                )
            }

            is LiveActivity.CallActivity -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Call, contentDescription = "Call", tint = AccentEmerald, modifier = Modifier.size(16.dp))
                    Text(activity.callerName, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1)
                }
                Text(
                    text = if (activity.isIncoming) "Incoming" else formatSecondsMmSs(activity.elapsedSeconds),
                    style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = AccentEmerald,
                )
            }

            is LiveActivity.NavigationActivity -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Navigation, contentDescription = "Nav", tint = AccentEmerald, modifier = Modifier.size(18.dp))
                    Text(activity.distanceText, style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = TextPrimary)
                }
                Text(activity.etaText, style = TabularMonoStyle.copy(fontSize = 12.sp), color = AccentEmerald)
            }

            is LiveActivity.VoiceRecording -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = "Recording", tint = AccentCoral, modifier = Modifier.size(18.dp))
                    IslandMediaVisualizer(isPlaying = !activity.isPaused, barColor = AccentCoral, barCount = 4, maxBarHeight = 12.dp)
                }
                Text(
                    text = formatSecondsMmSs(activity.elapsedSeconds),
                    style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = AccentCoral,
                )
            }

            is LiveActivity.DownloadActivity -> {
                val color = if (activity.isCompleted) AccentEmerald else AccentCyan
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RadialProgressRing(progress = activity.progressFraction, ringColor = color, size = 20.dp) {
                        Icon(
                            imageVector = if (activity.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Download,
                            contentDescription = "Download",
                            tint = color,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    Text(
                        text = activity.itemName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(84.dp),
                    )
                }
                Text(
                    text = if (activity.isCompleted) "Done" else "${(activity.progressFraction * 100).toInt()}%",
                    style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = color,
                )
            }

            is LiveActivity.UploadActivity -> {
                val color = if (activity.isCompleted) AccentEmerald else AccentViolet
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RadialProgressRing(progress = activity.progressFraction, ringColor = color, size = 20.dp) {
                        Icon(
                            imageVector = if (activity.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.CloudUpload,
                            contentDescription = "Upload",
                            tint = color,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    Text(
                        text = activity.itemName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(84.dp),
                    )
                }
                Text(
                    text = if (activity.isCompleted) "Sent" else "${(activity.progressFraction * 100).toInt()}%",
                    style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = color,
                )
            }

            is LiveActivity.HotspotActivity -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HotspotTetheringIcon(tint = AccentEmerald, size = 18.dp, pulse = true)
                    Text(
                        text = "Hotspot",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                    )
                }
                Text(
                    text = "${activity.connectedDevices} dev",
                    style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = AccentEmerald,
                )
            }

            is LiveActivity.DeliveryTracker -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = "Delivery", tint = AccentAmber, modifier = Modifier.size(16.dp))
                    Text(activity.merchant, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1, modifier = Modifier.width(90.dp), overflow = TextOverflow.Ellipsis)
                }
                Text("${activity.etaMinutes}m", style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = AccentAmber)
            }

            is LiveActivity.RideshareTracker -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = "Ride", tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Text(activity.licensePlate, style = TabularMonoStyle.copy(fontSize = 11.sp), color = TextPrimary)
                }
                Text("${activity.etaMinutes} min", style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = AccentCyan)
            }

            is LiveActivity.FlightTracker -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.FlightTakeoff, contentDescription = "Flight", tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Text(activity.flightCode, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                }
                Text("Gate ${activity.gate}", style = TabularMonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = AccentEmerald)
            }

            is LiveActivity.SportsScore -> {
                Text("${activity.homeTeam} ${activity.homeScore}-${activity.awayScore} ${activity.awayTeam}", style = TabularMonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = TextPrimary)
                Text(activity.clockLabel, style = TabularMonoStyle.copy(fontSize = 11.sp), color = AccentEmerald)
            }

            is LiveActivity.FileTransfer -> {
                val color = if (activity.isCompleted) AccentEmerald else AccentCyan
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RadialProgressRing(progress = activity.progressFraction, ringColor = color, size = 20.dp)
                    Text(activity.peerName, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1, modifier = Modifier.width(86.dp), overflow = TextOverflow.Ellipsis)
                }
                Text("${(activity.progressFraction * 100).toInt()}%", style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = color)
            }

            is LiveActivity.SystemNotification -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notification", tint = AccentBlue, modifier = Modifier.size(16.dp))
                    Text(activity.appName, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1)
                }
                Text("New", style = MaterialTheme.typography.labelSmall, color = AccentBlue)
            }
        }
    }
}

@Composable
private fun SplitBubbleGlyph(activity: LiveActivity) {
    when (activity) {
        is LiveActivity.TimerActivity -> {
            RadialProgressRing(
                progress = if (activity.totalSeconds > 0) activity.remainingSeconds.toFloat() / activity.totalSeconds else 0f,
                ringColor = AccentAmber,
                size = 24.dp,
            ) {
                Icon(Icons.Filled.Timer, contentDescription = "Timer", tint = AccentAmber, modifier = Modifier.size(12.dp))
            }
        }
        is LiveActivity.DownloadActivity -> {
            val c = if (activity.isCompleted) AccentEmerald else AccentCyan
            RadialProgressRing(progress = activity.progressFraction, ringColor = c, size = 24.dp) {
                Icon(Icons.Filled.Download, contentDescription = "Download", tint = c, modifier = Modifier.size(12.dp))
            }
        }
        is LiveActivity.UploadActivity -> {
            val c = if (activity.isCompleted) AccentEmerald else AccentViolet
            RadialProgressRing(progress = activity.progressFraction, ringColor = c, size = 24.dp) {
                Icon(Icons.Filled.CloudUpload, contentDescription = "Upload", tint = c, modifier = Modifier.size(12.dp))
            }
        }
        is LiveActivity.HotspotActivity -> HotspotTetheringIcon(tint = AccentEmerald, size = 18.dp, pulse = true)
        is LiveActivity.MediaPlayback -> IslandMediaVisualizer(isPlaying = activity.isPlaying, barColor = AccentViolet, barCount = 3, maxBarHeight = 14.dp)
        is LiveActivity.CallActivity -> Icon(Icons.Filled.Call, contentDescription = "Call", tint = AccentEmerald, modifier = Modifier.size(16.dp))
        is LiveActivity.NavigationActivity -> Icon(Icons.Filled.Navigation, contentDescription = "Nav", tint = AccentEmerald, modifier = Modifier.size(18.dp))
        is LiveActivity.VoiceRecording -> Icon(Icons.Filled.GraphicEq, contentDescription = "Rec", tint = AccentCoral, modifier = Modifier.size(16.dp))
        is LiveActivity.FlightTracker -> Icon(Icons.Filled.FlightTakeoff, contentDescription = "Flight", tint = AccentCyan, modifier = Modifier.size(16.dp))
        is LiveActivity.SportsScore -> Icon(Icons.Filled.SportsSoccer, contentDescription = "Sports", tint = AccentEmerald, modifier = Modifier.size(16.dp))
        is LiveActivity.DeliveryTracker -> Icon(Icons.Filled.LocalShipping, contentDescription = "Delivery", tint = AccentAmber, modifier = Modifier.size(16.dp))
        is LiveActivity.RideshareTracker -> Icon(Icons.Filled.DirectionsCar, contentDescription = "Ride", tint = AccentCyan, modifier = Modifier.size(16.dp))
        is LiveActivity.FileTransfer -> RadialProgressRing(progress = activity.progressFraction, ringColor = AccentCyan, size = 24.dp)
        is LiveActivity.StopwatchActivity -> Icon(Icons.Filled.Timer, contentDescription = "Stopwatch", tint = AccentAmber, modifier = Modifier.size(16.dp))
        is LiveActivity.SystemNotification -> Icon(Icons.Filled.Notifications, contentDescription = "Notif", tint = AccentBlue, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TransientPopupContent(alert: HardwareAlert) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        when (alert) {
            is HardwareAlert.ChargingStatus -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Bolt, contentDescription = "Charging", tint = AccentEmerald, modifier = Modifier.size(20.dp))
                    Text(
                        text = if (alert.isFastCharging) "Fast Charging" else "Charging",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                }
                Text(
                    text = "${alert.batteryPercent}%",
                    style = TabularMonoStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = AccentEmerald,
                )
            }

            is HardwareAlert.LowBattery -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.BatteryAlert, contentDescription = "Low Battery", tint = AccentCoral, modifier = Modifier.size(20.dp))
                    Text("Low Battery", style = MaterialTheme.typography.titleSmall, color = AccentCoral)
                }
                Text("${alert.batteryPercent}%", style = TabularMonoStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = AccentCoral)
            }

            is HardwareAlert.RingerModeChanged -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RingerBellBadge(mode = alert.mode)
                    Text(
                        text = when (alert.mode) {
                            RingerMode.SILENT -> "Silent Mode"
                            RingerMode.VIBRATE -> "Vibrate"
                            RingerMode.NORMAL -> "Ring On"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                }
                Text(
                    text = if (alert.mode == RingerMode.SILENT) "Muted" else "Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (alert.mode == RingerMode.SILENT) AccentCoral else AccentEmerald,
                )
            }

            is HardwareAlert.HotspotToggled -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HotspotTetheringIcon(
                        tint = if (alert.isEnabled) AccentEmerald else TextSecondary,
                        size = 20.dp,
                        pulse = alert.isEnabled,
                    )
                    Text(
                        text = if (alert.isEnabled) "Personal Hotspot On" else "Personal Hotspot Off",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                }
                Text(
                    text = if (alert.isEnabled) "${alert.connectedDevices} dev" else "Off",
                    style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = if (alert.isEnabled) AccentEmerald else TextSecondary,
                )
            }

            is HardwareAlert.BluetoothDevice -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Bluetooth, contentDescription = "Bluetooth", tint = AccentCyan, modifier = Modifier.size(18.dp))
                    Text(alert.deviceName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    text = if (alert.isConnected) "${alert.batteryPercent}%" else "Disconnected",
                    style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = if (alert.isConnected) AccentCyan else TextSecondary,
                )
            }

            is HardwareAlert.FocusMode -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.DoNotDisturbOn, contentDescription = "Focus", tint = AccentViolet, modifier = Modifier.size(18.dp))
                    Text(alert.modeName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                }
                Text(if (alert.isEnabled) "On" else "Off", style = MaterialTheme.typography.labelSmall, color = AccentViolet)
            }

            is HardwareAlert.PrivacyIndicator -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (alert.micActive) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AccentAmber))
                    }
                    if (alert.cameraActive) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AccentEmerald))
                    }
                    Text(
                        text = when {
                            alert.micActive && alert.cameraActive -> "Camera & Mic Active"
                            alert.cameraActive -> "Camera Active"
                            alert.micActive -> "Microphone Active"
                            else -> "Privacy Idle"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}
