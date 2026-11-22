package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IslandSettings
import com.example.model.TelemetryStats
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.IslandBlack
import com.example.ui.theme.SurfaceCarbon
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun IslandHubScreen(
    settings: IslandSettings,
    telemetry: TelemetryStats,
    onToggleOverlay: (Boolean) -> Unit,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onRefreshTelemetry: () -> Unit,
) {
    val context = LocalContext.current

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onRefreshTelemetry() }

    val btPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onRefreshTelemetry() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                OverlayMasterCard(
                    overlayEnabled = settings.overlayEnabled,
                    isOverlayRunning = telemetry.isOverlayRunning,
                    isOverlayPermGranted = telemetry.isOverlayPermissionGranted,
                    onToggleOverlay = { enabled ->
                        if (enabled && !telemetry.isOverlayPermissionGranted) {
                            openOverlayPermission(context)
                        } else {
                            onToggleOverlay(enabled)
                        }
                    },
                )
            }

            item {
                SectionHeader("System Permissions & Readiness")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PermissionRow(
                        icon = Icons.Filled.Layers,
                        title = "Display Over Other Apps",
                        subtitle = "Required to float the True-Black pill across all apps",
                        granted = telemetry.isOverlayPermissionGranted,
                        tag = "perm_overlay_row",
                        onClick = { openOverlayPermission(context) },
                    )
                    PermissionRow(
                        icon = Icons.Filled.NotificationsActive,
                        title = "Notification & Media Sync",
                        subtitle = "Mirrors Now Playing, downloads, uploads & system alerts",
                        granted = telemetry.isNotificationAccessGranted,
                        tag = "perm_notif_listener_row",
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
                            icon = Icons.Filled.NotificationsActive,
                            title = "Foreground Service Notice",
                            subtitle = "Keeps overlay service resilient in background",
                            granted = telemetry.isPostNotificationsGranted,
                            tag = "perm_post_notif_row",
                            onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PermissionRow(
                            icon = Icons.Filled.Bluetooth,
                            title = "Bluetooth Earbuds Pop-ups",
                            subtitle = "Detects audio device connects & battery",
                            granted = telemetry.isBluetoothPermissionGranted,
                            tag = "perm_bluetooth_row",
                            onClick = { btPermLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                        )
                    }
                }
            }

            item {
                PerformanceTelemetryCard(
                    telemetry = telemetry,
                    batterySaverMode = settings.batterySaverMode,
                    onRefresh = onRefreshTelemetry,
                )
            }

            item {
                CutoutCalibrationCard(
                    settings = settings,
                    onUpdateSettings = onUpdateSettings,
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun OverlayMasterCard(
    overlayEnabled: Boolean,
    isOverlayRunning: Boolean,
    isOverlayPermGranted: Boolean,
    onToggleOverlay: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderHairline, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCarbon),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            AccentCyan.copy(alpha = 0.12f),
                            Color.Transparent,
                            AccentEmerald.copy(alpha = 0.08f),
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isOverlayRunning) AccentEmerald else AccentAmber),
                        )
                        Text(
                            text = if (isOverlayRunning) "OVERLAY ACTIVE" else "SANDBOX READY",
                            style = TabularMonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = if (isOverlayRunning) AccentEmerald else AccentAmber,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "System-Wide Dynamic Island",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!isOverlayPermGranted) {
                            "Tap switch to grant Display Over Other Apps, or test all features directly in the top Sandbox."
                        } else {
                            "True-Black OLED pill & Drag & Go dock floating above all apps."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = overlayEnabled && isOverlayPermGranted,
                    onCheckedChange = onToggleOverlay,
                    modifier = Modifier.testTag("overlay_master_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = IslandBlack,
                        checkedTrackColor = AccentEmerald,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceElevated,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCarbon)
            .border(1.dp, if (granted) AccentEmerald.copy(alpha = 0.35f) else BorderHairline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (granted) AccentEmerald.copy(alpha = 0.16f) else SurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (granted) AccentEmerald else AccentCyan,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (granted) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Granted", tint = AccentEmerald, modifier = Modifier.size(20.dp))
        } else {
            Text(
                text = "Enable",
                style = MaterialTheme.typography.labelLarge,
                color = AccentCyan,
            )
        }
    }
}

@Composable
private fun PerformanceTelemetryCard(
    telemetry: TelemetryStats,
    batterySaverMode: Boolean,
    onRefresh: () -> Unit,
) {
    val budgetRatio = (telemetry.pssMemoryMb / 80f).coerceIn(0.05f, 1f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderHairline, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCarbon),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Memory, contentDescription = "Memory", tint = AccentCyan)
                    Text("Efficiency & Power Monitor", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_telemetry_btn")) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh telemetry", tint = TextSecondary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Process Memory (PSS)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    text = String.format("%.1f MB / 80.0 MB", telemetry.pssMemoryMb),
                    style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = if (telemetry.pssMemoryMb < 80f) AccentEmerald else AccentAmber,
                )
            }
            LinearProgressIndicator(
                progress = { budgetRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (telemetry.pssMemoryMb < 80f) AccentEmerald else AccentAmber,
                trackColor = SurfaceElevated,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TelemetryPill(
                    icon = Icons.Filled.Speed,
                    label = "GPU Compositing",
                    value = "Hardware",
                    color = AccentViolet,
                    modifier = Modifier.weight(1f),
                )
                TelemetryPill(
                    icon = Icons.Filled.BatteryChargingFull,
                    label = "Idle Wakeups",
                    value = if (telemetry.isTickerRunning) "Active Tick" else "0 FPS (<10mAh)",
                    color = if (batterySaverMode || !telemetry.isTickerRunning) AccentEmerald else AccentCyan,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TelemetryPill(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = TabularMonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = color)
        }
    }
}

@Composable
private fun CutoutCalibrationCard(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderHairline, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCarbon),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Tune, contentDescription = "Calibration", tint = AccentCyan)
                Text("Cutout Alignment & Size", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }

            CalibrationStepperRow(
                label = "Horizontal Offset (X)",
                valueText = "${settings.offsetXdp} dp",
                onMinus = { onUpdateSettings { it.copy(offsetXdp = (it.offsetXdp - 2).coerceIn(-80, 80)) } },
                onPlus = { onUpdateSettings { it.copy(offsetXdp = (it.offsetXdp + 2).coerceIn(-80, 80)) } },
            )
            CalibrationStepperRow(
                label = "Top Margin (Y)",
                valueText = "${settings.offsetYdp} dp",
                onMinus = { onUpdateSettings { it.copy(offsetYdp = (it.offsetYdp - 2).coerceIn(0, 64)) } },
                onPlus = { onUpdateSettings { it.copy(offsetYdp = (it.offsetYdp + 2).coerceIn(0, 64)) } },
            )
            CalibrationStepperRow(
                label = "Idle Pill Width",
                valueText = "${settings.baseWidthDp} dp",
                onMinus = { onUpdateSettings { it.copy(baseWidthDp = (it.baseWidthDp - 4).coerceIn(96, 180)) } },
                onPlus = { onUpdateSettings { it.copy(baseWidthDp = (it.baseWidthDp + 4).coerceIn(96, 180)) } },
            )
            CalibrationStepperRow(
                label = "Idle Pill Height",
                valueText = "${settings.baseHeightDp} dp",
                onMinus = { onUpdateSettings { it.copy(baseHeightDp = (it.baseHeightDp - 2).coerceIn(28, 44)) } },
                onPlus = { onUpdateSettings { it.copy(baseHeightDp = (it.baseHeightDp + 2).coerceIn(28, 44)) } },
            )
        }
    }
}

@Composable
private fun CalibrationStepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onMinus,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceCarbon),
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease $label", tint = TextPrimary, modifier = Modifier.size(16.dp))
            }
            Text(
                text = valueText,
                style = TabularMonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = AccentCyan,
            )
            IconButton(
                onClick = onPlus,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceCarbon),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase $label", tint = TextPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        letterSpacing = 0.8.sp,
    )
}

private fun openOverlayPermission(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
