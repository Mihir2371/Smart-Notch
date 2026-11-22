package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CapsuleStyle
import com.example.model.IslandSettings
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.IslandBlack
import com.example.ui.theme.SurfaceCarbon
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onResetDefaults: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionHeader("Behavior & System Sync")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsToggleRow(
                        title = "iPhone 13 Notch Mode",
                        subtitle = "Top-edge flat-top notch for devices without a punch-hole camera",
                        checked = settings.capsuleStyle == CapsuleStyle.IPHONE_13_NOTCH,
                        tag = "setting_notch_mode",
                        onCheckedChange = { enabled ->
                            onUpdateSettings {
                                it.copy(
                                    capsuleStyle = if (enabled) CapsuleStyle.IPHONE_13_NOTCH else CapsuleStyle.DYNAMIC_ISLAND,
                                )
                            }
                        },
                    )
                    SettingsToggleRow(
                        title = "OriginOS Drag & Go Dock",
                        subtitle = "Expand pill to matching apps when dragging items",
                        checked = settings.dragAndGoEnabled,
                        tag = "setting_drag_go",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(dragAndGoEnabled = v) } },
                    )
                    SettingsToggleRow(
                        title = "Show Idle OLED Pill",
                        subtitle = "Keep sleek pure-black capsule visible around camera cutout",
                        checked = settings.showIdlePill,
                        tag = "setting_idle_pill",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(showIdlePill = v) } },
                    )
                    SettingsToggleRow(
                        title = "Auto-Track Personal Hotspot",
                        subtitle = "Show iOS Hotspot Live Activity when system tethering turns on",
                        checked = settings.hotspotAutoTrack,
                        tag = "setting_hotspot_auto",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(hotspotAutoTrack = v) } },
                    )
                    SettingsToggleRow(
                        title = "Auto-Track Downloads & Uploads",
                        subtitle = "Mirror Play Store & browser transfer progress notifications",
                        checked = settings.downloadAutoTrack,
                        tag = "setting_download_auto",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(downloadAutoTrack = v) } },
                    )
                    SettingsToggleRow(
                        title = "Privacy Mic & Camera Dots",
                        subtitle = "Show subtle indicator dots inside idle capsule",
                        checked = settings.privacyDotsEnabled,
                        tag = "setting_privacy_dots",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(privacyDotsEnabled = v) } },
                    )
                    SettingsToggleRow(
                        title = "Haptic Feedback",
                        subtitle = "Tactile ticks when hovering Drag & Go app icons",
                        checked = settings.hapticFeedbackEnabled,
                        tag = "setting_haptics",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(hapticFeedbackEnabled = v) } },
                    )
                }
            }

            item {
                SectionHeader("Motion & Battery Tuning")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderHairline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCarbon),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SettingsToggleRow(
                            title = "Ultra Low-Power Mode",
                            subtitle = "1Hz ticker cadence & minimized background wakeups (<10 mAh)",
                            checked = settings.batterySaverMode,
                            tag = "setting_battery_saver",
                            onCheckedChange = { v -> onUpdateSettings { it.copy(batterySaverMode = v) } },
                        )
                        SettingsSliderRow(
                            label = "Spring Physics Speed",
                            valueText = String.format("%.1fx", settings.springSpeedFactor),
                            value = settings.springSpeedFactor,
                            range = 0.7f..1.5f,
                            onValueChange = { v -> onUpdateSettings { it.copy(springSpeedFactor = v) } },
                        )
                        SettingsSliderRow(
                            label = "Drag & Go Edge-Scroll Speed",
                            valueText = String.format("%.1fx", settings.edgeScrollSpeed),
                            value = settings.edgeScrollSpeed,
                            range = 0.6f..1.8f,
                            onValueChange = { v -> onUpdateSettings { it.copy(edgeScrollSpeed = v) } },
                        )
                        SettingsSliderRow(
                            label = "Auto-Collapse Timeout",
                            valueText = if (settings.autoCollapseSeconds == 0) "Manual" else "${settings.autoCollapseSeconds}s",
                            value = settings.autoCollapseSeconds.toFloat(),
                            range = 0f..15f,
                            onValueChange = { v -> onUpdateSettings { it.copy(autoCollapseSeconds = v.toInt()) } },
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onResetDefaults,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_settings_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCoral.copy(alpha = 0.18f),
                        contentColor = AccentCoral,
                    ),
                ) {
                    Text("Reset All Calibration & Settings")
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCarbon)
            .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = IslandBlack,
                checkedTrackColor = AccentEmerald,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceElevated,
            ),
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(
                text = valueText,
                style = TabularMonoStyle.copy(fontSize = 12.sp),
                color = AccentCyan,
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentCyan,
                inactiveTrackColor = SurfaceElevated,
            ),
        )
    }
}
