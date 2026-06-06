package com.nwe.spadesscore.ui

import android.content.Context
import com.nwe.spadesscore.SpadesApplication
import com.nwe.spadesscore.data.GameRepository

val Context.gameRepository: GameRepository
    get() = (applicationContext as SpadesApplication).container.gameRepository
