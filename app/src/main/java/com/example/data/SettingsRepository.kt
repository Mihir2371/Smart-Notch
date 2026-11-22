package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CapsuleStyle
import com.example.model.IslandSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.islandDataStore: DataStore<Preferences> by preferencesDataStore(name = "aura_island_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val CAPSULE_STYLE = stringPreferencesKey("capsule_style")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val DRAG_AND_GO_ENABLED = booleanPreferencesKey("drag_and_go_enabled")
        val SHOW_IDLE_PILL = booleanPreferencesKey("show_idle_pill")
        val OFFSET_X_DP = intPreferencesKey("offset_x_dp")
        val OFFSET_Y_DP = intPreferencesKey("offset_y_dp")
        val BASE_WIDTH_DP = intPreferencesKey("base_width_dp")
        val BASE_HEIGHT_DP = intPreferencesKey("base_height_dp")
        val CORNER_RADIUS_DP = intPreferencesKey("corner_radius_dp")
        val SPRING_SPEED = floatPreferencesKey("spring_speed")
        val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val EDGE_SCROLL_SPEED = floatPreferencesKey("edge_scroll_speed")
        val AUTO_COLLAPSE_SEC = intPreferencesKey("auto_collapse_sec")
        val PRIVACY_DOTS = booleanPreferencesKey("privacy_dots")
        val HOTSPOT_AUTO_TRACK = booleanPreferencesKey("hotspot_auto_track")
        val DOWNLOAD_AUTO_TRACK = booleanPreferencesKey("download_auto_track")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    val settingsFlow: Flow<IslandSettings> = context.islandDataStore.data.map { prefs ->
        readSettings(prefs)
    }

    private fun readSettings(prefs: Preferences): IslandSettings {
        val styleName = prefs[Keys.CAPSULE_STYLE]
        val style = CapsuleStyle.entries.firstOrNull { it.name == styleName } ?: CapsuleStyle.DYNAMIC_ISLAND
        return IslandSettings(
            capsuleStyle = style,
            overlayEnabled = prefs[Keys.OVERLAY_ENABLED] ?: false,
            dragAndGoEnabled = prefs[Keys.DRAG_AND_GO_ENABLED] ?: true,
            showIdlePill = prefs[Keys.SHOW_IDLE_PILL] ?: true,
            offsetXdp = prefs[Keys.OFFSET_X_DP] ?: 0,
            offsetYdp = prefs[Keys.OFFSET_Y_DP] ?: 8,
            baseWidthDp = prefs[Keys.BASE_WIDTH_DP] ?: 122,
            baseHeightDp = prefs[Keys.BASE_HEIGHT_DP] ?: 34,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS_DP] ?: 24,
            springSpeedFactor = prefs[Keys.SPRING_SPEED] ?: 1.0f,
            batterySaverMode = prefs[Keys.BATTERY_SAVER] ?: false,
            edgeScrollSpeed = prefs[Keys.EDGE_SCROLL_SPEED] ?: 1.0f,
            autoCollapseSeconds = prefs[Keys.AUTO_COLLAPSE_SEC] ?: 8,
            privacyDotsEnabled = prefs[Keys.PRIVACY_DOTS] ?: true,
            hotspotAutoTrack = prefs[Keys.HOTSPOT_AUTO_TRACK] ?: true,
            downloadAutoTrack = prefs[Keys.DOWNLOAD_AUTO_TRACK] ?: true,
            hapticFeedbackEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
        )
    }

    suspend fun update(transform: (IslandSettings) -> IslandSettings) {
        context.islandDataStore.edit { prefs ->
            val current = readSettings(prefs)
            val next = transform(current)
            prefs[Keys.CAPSULE_STYLE] = next.capsuleStyle.name
            prefs[Keys.OVERLAY_ENABLED] = next.overlayEnabled
            prefs[Keys.DRAG_AND_GO_ENABLED] = next.dragAndGoEnabled
            prefs[Keys.SHOW_IDLE_PILL] = next.showIdlePill
            prefs[Keys.OFFSET_X_DP] = next.offsetXdp
            prefs[Keys.OFFSET_Y_DP] = next.offsetYdp
            prefs[Keys.BASE_WIDTH_DP] = next.baseWidthDp
            prefs[Keys.BASE_HEIGHT_DP] = next.baseHeightDp
            prefs[Keys.CORNER_RADIUS_DP] = next.cornerRadiusDp
            prefs[Keys.SPRING_SPEED] = next.springSpeedFactor
            prefs[Keys.BATTERY_SAVER] = next.batterySaverMode
            prefs[Keys.EDGE_SCROLL_SPEED] = next.edgeScrollSpeed
            prefs[Keys.AUTO_COLLAPSE_SEC] = next.autoCollapseSeconds
            prefs[Keys.PRIVACY_DOTS] = next.privacyDotsEnabled
            prefs[Keys.HOTSPOT_AUTO_TRACK] = next.hotspotAutoTrack
            prefs[Keys.DOWNLOAD_AUTO_TRACK] = next.downloadAutoTrack
            prefs[Keys.HAPTICS_ENABLED] = next.hapticFeedbackEnabled
        }
    }

    suspend fun resetDefaults() {
        context.islandDataStore.edit { it.clear() }
    }
}
