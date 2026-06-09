package com.nwe.spadesscore.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Legt die System-Bar-Insets (Status- + Navigationsleiste, plus Display-Cutouts in
 * Landscape) als Padding auf diese View. Auf dem Activity-Content-Frame
 * (android.R.id.content) aufgerufen, der selbst kein Padding hat, bleibt das eigene
 * Padding jedes Layouts erhalten und wird nicht doppelt gezählt. targetSdk 36 erzwingt
 * Edge-to-Edge, daher app-weit notwendig.
 */
fun View.applySystemBarInsetsAsPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        insets
    }
}
