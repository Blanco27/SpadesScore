package com.nwe.spadesscore.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.nwe.spadesscore.SpadesApplication
import com.nwe.spadesscore.data.ThemePreferences
import com.nwe.spadesscore.domain.model.ThemeMode

val Context.themePreferences: ThemePreferences
    get() = (applicationContext as SpadesApplication).container.themePreferences

fun ThemeMode.toNightMode(): Int = when (this) {
    ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
}

/** Apply a theme choice globally and persist it. */
fun Context.applyThemeMode(mode: ThemeMode) {
    themePreferences.themeMode = mode
    AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
}
