package com.nwe.spadesscore.ui.settings

import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.domain.model.ThemeMode

data class SettingsUiState(
    val language: Language,
    val themeMode: ThemeMode,
)
