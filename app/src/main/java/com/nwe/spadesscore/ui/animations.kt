package com.nwe.spadesscore.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.view.View

/**
 * Animates the fill color of a view whose background is a [GradientDrawable]
 * (e.g. the CTA's bg_cta) from [fromColor] to [toColor] over 250 ms.
 */
fun animateFill(view: View, fromColor: Int, toColor: Int) {
    ValueAnimator.ofArgb(fromColor, toColor).apply {
        duration = 250
        addUpdateListener { animation ->
            (view.background as GradientDrawable).setColor(animation.animatedValue as Int)
        }
        start()
    }
}

/** Horizontal shake — signals a locked/blocked CTA. */
fun View.shake() {
    ObjectAnimator.ofFloat(this, "translationX", 0f, 16f, -16f, 12f, -12f, 6f, -6f, 0f).apply {
        duration = 350
        start()
    }
}
