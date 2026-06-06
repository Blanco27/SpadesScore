package com.nwe.spadesscore.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game")
data class GameEntity(
    @PrimaryKey val id: Int = 1,
    val playerCount: Int,
    val currentRound: Int,
    val dealerIndex: Int,
    val amountOfRounds: Int,
    val isSecondHalf: Boolean,
    val language: String,
)
