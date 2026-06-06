package com.nwe.spadesscore.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prediction")
data class PredictionEntity(
    @PrimaryKey val playerIndex: Int,
    val value: Int,
)
