package io.github.sheepdestroyer.materialisheep.preference

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.sheepdestroyer.materialisheep.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemePreferenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testGetTheme_blackOledThemeSpec() {
        val themeSpec = ThemePreference.getTheme("black", false)
        assertNotNull(themeSpec)
        assertEquals(R.string.theme_black, themeSpec.summary)
        assertEquals(R.style.Black, themeSpec.themeOverrides)
        assertTrue(themeSpec is ThemePreference.DarkSpec)
    }

    @Test
    fun testGetTheme_darkSpec() {
        val themeSpec = ThemePreference.getTheme("dark", false)
        assertNotNull(themeSpec)
        assertEquals(R.string.theme_dark, themeSpec.summary)
        assertTrue(themeSpec is ThemePreference.DarkSpec)
    }

    @Test
    fun testGetTheme_lightSpec() {
        val themeSpec = ThemePreference.getTheme("light", false)
        assertNotNull(themeSpec)
        assertEquals(R.string.theme_light, themeSpec.summary)
        assertTrue(themeSpec is ThemePreference.DayNightSpec)
    }

    @Test
    fun testGetTheme_translucent() {
        val themeSpec = ThemePreference.getTheme("black", true)
        assertNotNull(themeSpec)
        assertEquals(R.style.AppTheme_Dark_Translucent, themeSpec.theme)
    }

    @Test
    fun testGetTheme_fallbackForUnknown() {
        val themeSpec = ThemePreference.getTheme("non_existent_theme", false)
        assertNotNull(themeSpec)
        assertEquals(R.string.theme_light, themeSpec.summary)
    }
}
