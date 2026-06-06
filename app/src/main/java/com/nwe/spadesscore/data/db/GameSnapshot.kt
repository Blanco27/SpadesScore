package com.nwe.spadesscore.data.db

data class GameSnapshot(
    val game: GameEntity,
    val players: List<PlayerEntity>,
    val scores: List<ScoreEntity>,
    val predictions: List<PredictionEntity>,
)
