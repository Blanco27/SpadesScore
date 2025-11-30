package com.nwe.spadesscore

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.nwe.spadesscore.databinding.ViewTrickSpinnerBinding

class TrickSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding: ViewTrickSpinnerBinding =
        ViewTrickSpinnerBinding.inflate(LayoutInflater.from(context), this, true)

    var value: Int = 0
        private set

    var maxAmount: Int = 10 // Standardwert
        private set

    private var onValueChangedListener: (() -> Unit)? = null

    fun setOnValueChangedListener(listener: (() -> Unit)?) {
        onValueChangedListener = listener
    }

    fun setMaxAmount(amount: Int) {
        maxAmount = amount
    }

    init {
        setupListeners()
    }


    private fun setupListeners() {
        binding.btnPlus.setOnClickListener {
            if (value < maxAmount) {
                value++
                binding.tvValue.text = value.toString()
                onValueChangedListener?.invoke()
            }
        }

        binding.btnMinus.setOnClickListener {
            if (value > 0) {
                value--
                binding.tvValue.text = value.toString()
                onValueChangedListener?.invoke()
            }
        }
    }
}
