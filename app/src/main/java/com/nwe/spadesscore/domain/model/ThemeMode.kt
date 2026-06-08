package com.nwe.spadesscore.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Pure parse of a persisted theme-mode string; unknown/null -> SYSTEM. */
fun themeModeFromStorage(value: String?): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.SYSTEM
