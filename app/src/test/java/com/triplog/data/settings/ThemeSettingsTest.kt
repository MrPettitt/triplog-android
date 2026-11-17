package com.triplog.data.settings

import com.triplog.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeSettingsTest {

    private class MemoryStore(initial: Map<String, String> = emptyMap()) : SettingsStore {
        val values = initial.toMutableMap()
        override fun getString(key: String): String? = values[key]
        override fun putString(key: String, value: String) {
            values[key] = value
        }
        override fun remove(key: String) {
            values.remove(key)
        }
    }

    @Test
    fun defaultsToFollowingTheSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeSettings(MemoryStore()).mode.value)
    }

    @Test
    fun readsTheSavedChoiceOnStartup() {
        val settings = ThemeSettings(MemoryStore(mapOf("theme_mode" to "LIGHT")))

        assertEquals(ThemeMode.LIGHT, settings.mode.value)
    }

    @Test
    fun setMode_updatesTheFlowAndPersists() {
        val store = MemoryStore()
        val settings = ThemeSettings(store)

        settings.setMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, settings.mode.value)
        assertEquals("DARK", store.values["theme_mode"])
        assertEquals(ThemeMode.DARK, ThemeSettings(store).mode.value)
    }

    @Test
    fun unknownStoredValue_fallsBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("PURPLE"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(null))
        assertEquals(ThemeMode.SYSTEM, ThemeSettings(MemoryStore(mapOf("theme_mode" to "nonsense"))).mode.value)
    }
}
