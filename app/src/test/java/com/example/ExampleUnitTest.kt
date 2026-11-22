package com.example

import com.example.model.IslandSettings
import com.example.ui.island.formatMillisStopwatch
import com.example.ui.island.formatSecondsMmSs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun timeFormatters_formatCorrectly() {
        assertEquals("0:00", formatSecondsMmSs(0))
        assertEquals("5:03", formatSecondsMmSs(303))
        assertEquals("01:02.4", formatMillisStopwatch(62_450L))
    }

    @Test
    fun defaultSettings_stayLightweight() {
        val defaults = IslandSettings()
        assertTrue(defaults.dragAndGoEnabled)
        assertTrue(defaults.hotspotAutoTrack)
        assertTrue(defaults.downloadAutoTrack)
        assertEquals(122, defaults.baseWidthDp)
    }
}
