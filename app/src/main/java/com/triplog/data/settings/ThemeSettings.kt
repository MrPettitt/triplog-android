package com.triplog.data.settings

import android.content.Context
import com.triplog.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Minimal key-value storage, so the settings can be tested without Android. */
interface SettingsStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

class SharedPreferencesStore @Inject constructor(
    @ApplicationContext context: Context
) : SettingsStore {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

/** The chosen theme, remembered across launches. */
@Singleton
class ThemeSettings @Inject constructor(private val store: SettingsStore) {

    private val _mode = MutableStateFlow(ThemeMode.fromName(store.getString(KEY_THEME_MODE)))
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        store.putString(KEY_THEME_MODE, mode.name)
        _mode.value = mode
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
