package com.nwe.spadesscore

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.ui.applyPersistedLocale
import com.nwe.spadesscore.ui.applySystemBarInsetsAsPadding
import com.nwe.spadesscore.ui.gameRepository

abstract class SpadesAppCompatActivity : AppCompatActivity() {

    /**
     * Sprache, mit der diese Activity zuletzt inflated wurde. Dient dazu, nach der Rückkehr aus
     * den Einstellungen einen zwischenzeitlichen Sprachwechsel zu erkennen (siehe [onResume]).
     */
    private var appliedLanguage: Language? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyPersistedLocale()
        appliedLanguage = gameRepository.state.value.language
        initContentView()
        findViewById<View>(android.R.id.content).applySystemBarInsetsAsPadding()
        initializeUIComponents()
        setupUI()
        setupBackPressHandler()
    }

    /**
     * Wird die Sprache in den Einstellungen geändert, kehrt der Nutzer auf eine bereits erstellte
     * Activity (z. B. MainActivity) zurück, die Android nur fortsetzt statt neu zu erstellen – das
     * Layout bliebe sonst in der alten Sprache stehen. Darum hier den persistierten Stand gegen die
     * zuletzt angewendete Sprache prüfen und bei Abweichung neu aufbauen.
     */
    override fun onResume() {
        super.onResume()
        if (appliedLanguage != null && appliedLanguage != gameRepository.state.value.language) {
            recreate()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* disable back */ }
        })
    }

    protected abstract fun initializeUIComponents()

    protected abstract fun setupUI()

    protected abstract fun initContentView()
}
