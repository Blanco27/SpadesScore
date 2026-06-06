package com.nwe.spadesscore.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Int = 0,
    val playerIndex: Int,
    val roundIndex: Int,
    val cumulativeScore: Int,
)
