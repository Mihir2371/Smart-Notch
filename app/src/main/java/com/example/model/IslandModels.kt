package com.example.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

enum class ActivityCategory(val displayName: String) {
    MEDIA("Media & Audio"),
    TIMERS("Timers & Stopwatch"),
    CALLS("Calls & Voice"),
    TRANSFERS("Downloads & Uploads"),
    HOTSPOT("Personal Hotspot"),
    NAVIGATION("Navigation"),
    TRACKERS("Delivery, Rides & Flights"),
    NOTIFICATIONS("System Notifications"),
}

enum class NavDirection {
    LEFT,
    RIGHT,
    STRAIGHT,
    ARRIVE,
}

enum class RingerMode {
    SILENT,
    VIBRATE,
    NORMAL,
}

@Immutable
sealed interface LiveActivity {
    val id: String
    val category: ActivityCategory
    val priority: Int

    @Immutable
    data class MediaPlayback(
        override val id: String = "media_active",
        val title: String,
        val artist: String,
        val album: String = "",
        val isPlaying: Boolean,
        val progressMs: Long,
        val durationMs: Long,
        val artworkBitmap: Bitmap? = null,
        val isSystemSession: Boolean = false,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.MEDIA
        override val priority: Int = 70
    }

    @Immutable
    data class TimerActivity(
        override val id: String = "timer_active",
        val label: String,
        val remainingSeconds: Int,
        val totalSeconds: Int,
        val isPaused: Boolean = false,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.TIMERS
        override val priority: Int = 85
    }

    @Immutable
    data class StopwatchActivity(
        override val id: String = "stopwatch_active",
        val elapsedMillis: Long,
        val isRunning: Boolean = true,
        val lapCount: Int = 1,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.TIMERS
        override val priority: Int = 65
    }

    @Immutable
    data class CallActivity(
        override val id: String = "call_active",
        val callerName: String,
        val subtitle: String,
        val elapsedSeconds: Int,
        val isIncoming: Boolean,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = false,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.CALLS
        override val priority: Int = 100
    }

    @Immutable
    data class NavigationActivity(
        override val id: String = "nav_active",
        val direction: NavDirection,
        val instruction: String,
        val streetName: String,
        val distanceText: String,
        val etaText: String,
        val remainingMinutes: Int,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.NAVIGATION
        override val priority: Int = 80
    }

    @Immutable
    data class VoiceRecording(
        override val id: String = "voice_recording",
        val label: String,
        val elapsedSeconds: Int,
        val isPaused: Boolean = false,
        val amplitudeLevel: Float = 0.65f,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.CALLS
        override val priority: Int = 92
    }

    @Immutable
    data class DownloadActivity(
        override val id: String = "download_active",
        val itemName: String,
        val sourceLabel: String,
        val isAppInstall: Boolean,
        val progressFraction: Float,
        val speedText: String,
        val transferredText: String,
        val totalText: String,
        val isPaused: Boolean = false,
        val isCompleted: Boolean = false,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.TRANSFERS
        override val priority: Int = 78
    }

    @Immutable
    data class UploadActivity(
        override val id: String = "upload_active",
        val itemName: String,
        val targetDestination: String,
        val progressFraction: Float,
        val speedText: String,
        val transferredText: String,
        val totalText: String,
        val isPaused: Boolean = false,
        val isCompleted: Boolean = false,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.TRANSFERS
        override val priority: Int = 77
    }

    @Immutable
    data class HotspotActivity(
        override val id: String = "hotspot_active",
        val networkName: String,
        val connectedDevices: Int,
        val elapsedSeconds: Int,
        val bandLabel: String = "5 GHz",
        val isSystemSynced: Boolean = false,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.HOTSPOT
        override val priority: Int = 74
    }

    @Immutable
    data class DeliveryTracker(
        override val id: String = "delivery_active",
        val merchant: String,
        val stageLabel: String,
        val stageIndex: Int,
        val etaMinutes: Int,
        val progressFraction: Float,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.TRACKERS
        override val priority: Int = 62
    }

    @Immutable
    data class RideshareTracker(
        override val id: String = "rideshare_active",
        val driverName: String,
        val vehicleModel: String,
        val licensePlate: String,
        val etaMinutes: Int,
        val statusLabel: String,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.TRACKERS
        override val priority: Int = 68
    }

    @Immutable
    data class FlightTracker(
        override val id: String = "flight_active",
        val flightCode: String,
        val originCode: String,
        val destinationCode: String,
        val gate: String,
        val statusLabel: String,
        val boardingInMinutes: Int,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.TRACKERS
        override val priority: Int = 64
    }

    @Immutable
    data class SportsScore(
        override val id: String = "sports_active",
        val league: String,
        val homeTeam: String,
        val awayTeam: String,
        val homeScore: Int,
        val awayScore: Int,
        val clockLabel: String,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.TRACKERS
        override val priority: Int = 60
    }

    @Immutable
    data class FileTransfer(
        override val id: String = "airdrop_transfer",
        val fileName: String,
        val peerName: String,
        val progressFraction: Float,
        val isCompleted: Boolean = false,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.TRANSFERS
        override val priority: Int = 88
    }

    @Immutable
    data class SystemNotification(
        override val id: String,
        val packageName: String,
        val appName: String,
        val title: String,
        val message: String,
        val timestampMillis: Long,
    ) : LiveActivity {
        override val category: ActivityCategory = ActivityCategory.NOTIFICATIONS
        override val priority: Int = 55
    }
}

@Immutable
sealed interface HardwareAlert {
    val id: String

    @Immutable
    data class ChargingStatus(
        override val id: String = "alert_charging",
        val batteryPercent: Int,
        val isCharging: Boolean,
        val isFastCharging: Boolean,
    ) : HardwareAlert

    @Immutable
    data class LowBattery(
        override val id: String = "alert_low_battery",
        val batteryPercent: Int,
    ) : HardwareAlert

    @Immutable
    data class RingerModeChanged(
        override val id: String = "alert_ringer",
        val mode: RingerMode,
    ) : HardwareAlert

    @Immutable
    data class HotspotToggled(
        override val id: String = "alert_hotspot",
        val isEnabled: Boolean,
        val connectedDevices: Int,
    ) : HardwareAlert

    @Immutable
    data class BluetoothDevice(
        override val id: String = "alert_bluetooth",
        val deviceName: String,
        val batteryPercent: Int,
        val isConnected: Boolean,
    ) : HardwareAlert

    @Immutable
    data class FocusMode(
        override val id: String = "alert_focus",
        val modeName: String,
        val isEnabled: Boolean,
    ) : HardwareAlert

    @Immutable
    data class PrivacyIndicator(
        override val id: String = "alert_privacy",
        val micActive: Boolean,
        val cameraActive: Boolean,
    ) : HardwareAlert
}

enum class IslandDisplayMode {
    HIDDEN,
    IDLE_PILL,
    COMPACT_SINGLE,
    COMPACT_SPLIT,
    EXPANDED_CARD,
    TRANSIENT_POPUP,
    DRAG_AND_GO_DOCK,
}
