package com.nwe.spadesscore.ui.settings

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.data.ThemePreferences
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.domain.model.ThemeMode

class SettingsViewModel(
    private val repository: GameRepository,
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    fun uiState(): SettingsUiState = SettingsUiState(
        language = repository.state.value.language,
        themeMode = themePreferences.themeMode,
    )

    fun setLanguage(language: Language) = repository.setLanguage(language)

    fun currentLanguage(): Language = repository.state.value.language
}
