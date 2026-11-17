package com.triplog.ui.theme

/** Which colour theme to use: follow the phone's setting, or always light or dark. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** Unknown or missing values fall back to following the system. */
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
