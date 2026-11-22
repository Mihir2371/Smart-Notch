package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.DynamicIslandCoordinator
import com.example.core.IslandSnapshot
import com.example.model.RingerMode
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.IslandBlack
import com.example.ui.theme.SurfaceCarbon
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveLabScreen(
    coordinator: DynamicIslandCoordinator,
    snapshot: IslandSnapshot,
) {
    val isHotspotOn = snapshot.primaryActivity?.id == "hotspot_active" ||
        snapshot.secondaryActivity?.id == "hotspot_active" ||
        coordinator.isHotspotActive()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                // Hero Dual-Pill Preset + Clear All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { coordinator.triggerDualActivityPreset() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("lab_dual_preset_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan,
                            contentColor = IslandBlack,
                        ),
                    ) {
                        Icon(Icons.Filled.Splitscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Split-Pill Combo", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { coordinator.clearAll() },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("lab_clear_all_btn"),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.ClearAll, contentDescription = "Clear all", tint = TextPrimary)
                    }
                }
            }

            item {
                SectionHeader("Hotspot & File / App Transfers")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2,
                ) {
                    LabTriggerTile(
                        icon = Icons.Filled.WifiTethering,
                        title = if (isHotspotOn) "Hotspot: ON (Turn Off)" else "Hotspot: OFF (Turn On)",
                        subtitle = "iOS Personal Hotspot Live Activity",
                        accent = AccentEmerald,
                        isHighlighted = isHotspotOn,
                        tag = "lab_hotspot_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.setHotspotActive(enabled = !isHotspotOn, connectedDevices = 2) },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.Download,
                        title = "App Downloading",
                        subtitle = "Play Store progress ring & MB/s",
                        accent = AccentCyan,
                        tag = "lab_app_download_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerFileAppDownload(isAppInstall = true) },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.Download,
                        title = "File Downloading",
                        subtitle = "Archive download with speed & pause",
                        accent = AccentBlue,
                        tag = "lab_file_download_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerFileAppDownload(isAppInstall = false) },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.CloudUpload,
                        title = "File Uploading",
                        subtitle = "Cloud Drive upload live tracker",
                        accent = AccentViolet,
                        tag = "lab_file_upload_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerFileAppUpload() },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.Share,
                        title = "AirDrop Transfer",
                        subtitle = "Peer-to-peer radial ring",
                        accent = AccentCyan,
                        tag = "lab_airdrop_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerAirDropTransfer() },
                    )
                }
            }

            item {
                SectionHeader("Core iOS Dynamic Island Activities")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2,
                ) {
                    LabTriggerTile(
                        icon = Icons.Filled.MusicNote,
                        title = "Now Playing",
                        subtitle = "Artwork, waveform & seek bar",
                        accent = AccentViolet,
                        tag = "lab_media_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerSandboxMedia() },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.Timer,
                        title = "5m Countdown Timer",
                        subtitle = "Live ring +1m & pause",
                        accent = AccentAmber,
                        tag = "lab_timer_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.startTimer(300, "Focus") },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.Timer,
                        title = "Stopwatch",
                        subtitle = "Tenths precision + laps",
                        accent = AccentAmber,
                        tag = "lab_stopwatch_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.startStopwatch() },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.CallReceived,
                        title = "Incoming Call",
                        subtitle = "Answer / Decline expanded card",
                        accent = AccentEmerald,
                        tag = "lab_incoming_call_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerCall(incoming = true) },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.PhoneInTalk,
                        title = "Ongoing Call",
                        subtitle = "Mute, speaker & call timer",
                        accent = AccentEmerald,
                        tag = "lab_active_call_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerCall(incoming = false) },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.Navigation,
                        title = "Turn Navigation",
                        subtitle = "Next turn arrow & ETA",
                        accent = AccentEmerald,
                        tag = "lab_nav_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerNavigation() },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.GraphicEq,
                        title = "Voice Recording",
                        subtitle = "Live waveform + mic dot",
                        accent = AccentCoral,
                        tag = "lab_recording_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerVoiceRecording() },
                    )
                }
            }

            item {
                SectionHeader("Live Trackers & Alerts")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2,
                ) {
                    LabTriggerTile(
                        icon = Icons.Filled.LocalShipping,
                        title = "Delivery Tracker",
                        subtitle = "Courier stops & live progress",
                        accent = AccentAmber,
                        tag = "lab_delivery_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerDeliveryTracker() },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.DirectionsCar,
                        title = "Rideshare Pickup",
                        subtitle = "Plate, driver & arrival ETA",
                        accent = AccentCyan,
                        tag = "lab_ride_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerRideshareTracker() },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.FlightTakeoff,
                        title = "Flight Boarding",
                        subtitle = "Route, gate & countdown",
                        accent = AccentCyan,
                        tag = "lab_flight_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerFlightTracker() },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.SportsSoccer,
                        title = "Live Sports Score",
                        subtitle = "Real-time score ticker",
                        accent = AccentEmerald,
                        tag = "lab_sports_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerSportsScore() },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.Notifications,
                        title = "Notification Banner",
                        subtitle = "Expandable message pill",
                        accent = AccentBlue,
                        tag = "lab_notif_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            coordinator.pushSystemNotification(
                                packageName = "com.example.chat",
                                appName = "Messages",
                                title = "Elena",
                                message = "Just dropped the Aurora photo via Drag & Go!",
                            )
                        },
                    )
                }
            }

            item {
                SectionHeader("Hardware & System Pop-ups")
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2,
                ) {
                    LabTriggerTile(
                        icon = Icons.Filled.Bolt,
                        title = "Fast Charging",
                        subtitle = "Bolt & battery percentage",
                        accent = AccentEmerald,
                        tag = "lab_charging_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerChargingAlert(86, isCharging = true, isFast = true) },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.BatteryAlert,
                        title = "Low Battery",
                        subtitle = "15% alert pulse",
                        accent = AccentCoral,
                        tag = "lab_low_battery_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerLowBatteryAlert(15) },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.NotificationsOff,
                        title = "Silent Switch",
                        subtitle = "Shaking red mute bell",
                        accent = AccentCoral,
                        tag = "lab_silent_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerRingerModeAlert(RingerMode.SILENT) },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.Vibration,
                        title = "Vibrate / Ring",
                        subtitle = "Ringer state feedback",
                        accent = AccentAmber,
                        tag = "lab_vibrate_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerRingerModeAlert(RingerMode.VIBRATE) },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.Bluetooth,
                        title = "Earbuds Connected",
                        subtitle = "Bluetooth audio + battery",
                        accent = AccentCyan,
                        tag = "lab_bluetooth_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerBluetoothAlert("Aura Buds Pro", 92, true) },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.DoNotDisturbOn,
                        title = "Focus / DND",
                        subtitle = "Do Not Disturb indicator",
                        accent = AccentViolet,
                        tag = "lab_focus_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerFocusModeAlert("Do Not Disturb", true) },
                    )
                    LabTriggerTile(
                        icon = Icons.Filled.Videocam,
                        title = "Privacy Mic & Cam",
                        subtitle = "Orange & green privacy dots",
                        accent = AccentEmerald,
                        tag = "lab_privacy_trigger",
                        modifier = Modifier.weight(1f),
                        onClick = { coordinator.triggerPrivacyDots(mic = true, camera = true) },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LabTriggerTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    isHighlighted: Boolean = false,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isHighlighted) {
                    Brush.verticalGradient(listOf(accent.copy(alpha = 0.22f), SurfaceCarbon))
                } else {
                    Brush.verticalGradient(listOf(SurfaceElevated, SurfaceCarbon))
                },
            )
            .border(
                width = 1.dp,
                color = if (isHighlighted) accent else BorderHairline,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(18.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 2,
        )
    }
}
