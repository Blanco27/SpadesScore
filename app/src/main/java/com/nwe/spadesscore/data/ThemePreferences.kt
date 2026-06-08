package com.nwe.spadesscore.data

import android.content.Context
import com.nwe.spadesscore.domain.model.ThemeMode
import com.nwe.spadesscore.domain.model.themeModeFromStorage

/** Persists the global UI theme choice (not game state). */
class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("spades_ui_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = themeModeFromStorage(prefs.getString(KEY_THEME, null))
        set(value) { prefs.edit().putString(KEY_THEME, value.name).apply() }

    private companion object { const val KEY_THEME = "theme_mode" }
}
