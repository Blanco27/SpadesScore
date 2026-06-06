package com.nwe.spadesscore.domain.model

/** Ein Spieler mit Namen und kumulativer Score-Historie (Index 0 = Startwert 0). */
data class Player(
    val name: String,
    val scores: List<Int>,
) {
    val currentScore: Int get() = scores.last()
}
