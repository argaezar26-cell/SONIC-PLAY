package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.dsp.EqPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SONICFLOW", appName)
    }

    @Test
    fun `preset JSON export and import round-trips cleanly`() {
        val vocalPreset = EqPresets.builtInPresets.first { it.name == "Vocal Clarity" }
        val json = EqPresets.exportPresetJson(vocalPreset)
        val imported = EqPresets.importPresetJson(json)
        assertNotNull(imported)
        assertEquals("Vocal Clarity", imported!!.name)
        assertEquals(vocalPreset.bands.size, imported.bands.size)
        assertEquals(vocalPreset.preampDb, imported.preampDb, 0.01f)
    }
}
