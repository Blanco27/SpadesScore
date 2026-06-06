package com.nwe.spadesscore.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val playerIndex: Int,
    val name: String,
)
