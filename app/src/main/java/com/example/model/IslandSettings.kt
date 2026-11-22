package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class IslandSettings(
    val overlayEnabled: Boolean = false,
    val dragAndGoEnabled: Boolean = true,
    val showIdlePill: Boolean = true,
    val offsetXdp: Int = 0,
    val offsetYdp: Int = 8,
    val baseWidthDp: Int = 122,
    val baseHeightDp: Int = 34,
    val cornerRadiusDp: Int = 24,
    val springSpeedFactor: Float = 1.0f,
    val batterySaverMode: Boolean = false,
    val edgeScrollSpeed: Float = 1.0f,
    val autoCollapseSeconds: Int = 8,
    val privacyDotsEnabled: Boolean = true,
    val hotspotAutoTrack: Boolean = true,
    val downloadAutoTrack: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
)

@Immutable
data class TelemetryStats(
    val pssMemoryMb: Float = 0f,
    val javaHeapMb: Float = 0f,
    val isScreenOn: Boolean = true,
    val isTickerRunning: Boolean = false,
    val isOverlayRunning: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
    val isNotificationAccessGranted: Boolean = false,
    val isPostNotificationsGranted: Boolean = false,
    val isBluetoothPermissionGranted: Boolean = false,
)
