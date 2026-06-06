package com.nwe.spadesscore.ui

import android.app.Activity
import com.nwe.spadesscore.domain.model.Language
import java.util.Locale

/**
 * Wendet die im Repository gespeicherte Spielsprache auf die Konfiguration dieser Activity an,
 * BEVOR das Layout inflated wird. Notwendig, damit nach einem Prozess-Tod auch eine tief liegende
 * Activity (die von Android ohne vorheriges Durchlaufen der MainActivity neu erstellt wird) in der
 * zuletzt gewählten Sprache erscheint, statt auf die Geräte-Sprache zurückzufallen. Kein recreate().
 */
fun Activity.applyPersistedLocale() {
    val code = if (gameRepository.state.value.language == Language.GERMAN) "de" else "en"
    if (resources.configuration.locales[0].language == code) return
    val locale = Locale(code)
    Locale.setDefault(locale)
    val config = resources.configuration
    config.setLocale(locale)
    @Suppress("DEPRECATION")
    resources.updateConfiguration(config, resources.displayMetrics)
}
