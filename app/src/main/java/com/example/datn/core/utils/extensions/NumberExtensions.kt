package com.example.datn.core.utils.extensions

import java.text.DecimalFormat
import kotlin.math.roundToInt

fun Double.formatScore(maxFractionDigits: Int = 2): String {
    val pattern = if (maxFractionDigits <= 0) "#" else "#." + "#".repeat(maxFractionDigits)
    val decimalFormat = DecimalFormat(pattern)
    return decimalFormat.format(this)
}

fun Int.toPercentString(): String = "${this}%"

fun Double.toPercentString(): String = "${(this * 100).roundToInt()}%"