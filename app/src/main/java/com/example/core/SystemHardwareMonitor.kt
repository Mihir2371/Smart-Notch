package com.example.core

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.model.RingerMode

class SystemHardwareMonitor(
    private val context: Context,
    private val coordinator: DynamicIslandCoordinator,
) {
    private var registered = false
    private var lastChargingState: Boolean? = null
    private var lastRingerMode: Int? = null
    private var lastHotspotState: Int? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> coordinator.setScreenOn(true)
                Intent.ACTION_SCREEN_OFF -> coordinator.setScreenOn(false)

                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED,
                Intent.ACTION_BATTERY_LOW,
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                    val pct = if (level >= 0) (level * 100) / scale else 50
                    val isFast = plugged == BatteryManager.BATTERY_PLUGGED_AC

                    if (intent.action == Intent.ACTION_POWER_CONNECTED ||
                        (lastChargingState != null && lastChargingState != isCharging && isCharging)
                    ) {
                        coordinator.triggerChargingAlert(pct, true, isFast)
                    } else if (intent.action == Intent.ACTION_BATTERY_LOW) {
                        coordinator.triggerLowBatteryAlert(pct)
                    }
                    lastChargingState = isCharging
                }

                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    val modeInt = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1)
                    if (lastRingerMode != null && lastRingerMode != modeInt) {
                        val mode = when (modeInt) {
                            AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
                            AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
                            else -> RingerMode.NORMAL
                        }
                        coordinator.triggerRingerModeAlert(mode)
                    }
                    lastRingerMode = modeInt
                }

                "android.net.wifi.WIFI_AP_STATE_CHANGED" -> {
                    val apState = intent.getIntExtra("wifi_state", -1)
                    // 13 == WIFI_AP_STATE_ENABLED, 11 == WIFI_AP_STATE_DISABLED
                    if (apState == 13 && lastHotspotState != 13) {
                        coordinator.setHotspotActive(enabled = true, connectedDevices = 1, fromSystem = true)
                    } else if (apState == 11 && lastHotspotState == 13) {
                        coordinator.setHotspotActive(enabled = false, connectedDevices = 0, fromSystem = true)
                    }
                    lastHotspotState = apState
                }

                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val hasBtPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                    if (hasBtPerm) {
                        @Suppress("DEPRECATION")
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val name = runCatching { device?.name }.getOrNull() ?: "Bluetooth Audio"
                        val connected = intent.action == BluetoothDevice.ACTION_ACL_CONNECTED
                        coordinator.triggerBluetoothAlert(name, batteryPercent = 92, connected = connected)
                    }
                }
            }
        }
    }

    fun register() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        registered = true
    }

    fun unregister() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }
}
