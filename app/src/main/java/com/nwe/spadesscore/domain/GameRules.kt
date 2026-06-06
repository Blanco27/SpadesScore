package com.nwe.spadesscore.domain

/** Reine Spielregel-Konstanten (keine UI-/Präsentationswerte). */
object GameRules {
    /** Gesamtzahl Karten, geteilt durch Spielerzahl ergibt die Rundenzahl je Halbzeit. */
    const val TOTAL_CARDS = 32

    /** Bonus auf den Score, wenn ein Spieler seine Vorhersage trifft. */
    const val HIT_BONUS = 5

    /** Maximale Länge eines Spielernamens. */
    const val MAX_NAME_LENGTH = 10
}
