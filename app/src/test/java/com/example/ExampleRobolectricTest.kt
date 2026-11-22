package com.example

import android.content.ClipData
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.DragAndGoResolver
import com.example.core.DynamicIslandCoordinator
import com.example.model.DragPayloadType
import com.example.model.IslandDisplayMode
import com.example.model.LiveActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Aura Island`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Aura Island", context.getString(R.string.app_name))
    }

    @Test
    fun `hotspot on and off updates LiveActivity like iOS`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coordinator = DynamicIslandCoordinator(context)

        coordinator.setHotspotActive(enabled = true, connectedDevices = 3)
        assertTrue(coordinator.isHotspotActive())
        val active = coordinator.snapshot.value.primaryActivity as LiveActivity.HotspotActivity
        assertEquals(3, active.connectedDevices)

        coordinator.setHotspotActive(enabled = false)
        assertFalse(coordinator.isHotspotActive())
    }

    @Test
    fun `dual activities trigger compact split bubble mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coordinator = DynamicIslandCoordinator(context)

        coordinator.triggerDualActivityPreset()
        assertEquals(IslandDisplayMode.COMPACT_SPLIT, coordinator.snapshot.value.displayMode)
    }

    @Test
    fun `drag resolver classifies URL and image payloads cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = DragAndGoResolver(context)

        val urlClip = ClipData.newPlainText("link", "https://developer.android.com")
        val urlPayload = resolver.classifyClipData(urlClip.description, urlClip)
        assertEquals(DragPayloadType.URL, urlPayload.type)

        val geoClip = ClipData.newPlainText("addr", "1600 Amphitheatre Pkwy, Mountain View, CA")
        val geoPayload = resolver.classifyClipData(geoClip.description, geoClip)
        assertEquals(DragPayloadType.GEO, geoPayload.type)
    }
}
