package com.jsoizo.serialization.php

import kotlin.math.roundToInt

interface IValueFormatterWasm {
    fun String.ePlusToE(): String = this.replace("e+", "E")

    private val floatScale: Double
        get() = 10000000.0

    fun Float.formatScientificNotation(): String {
        val str = this.toString()
        val parts = str.split("e+")
        when (parts.size) {
            1 -> return str
            2 -> {
                val mantissa = parts[0].toDouble()
                val exponent = parts[1]
                val roundedMantissa = (mantissa * floatScale).roundToInt() / floatScale
                return "${roundedMantissa}E$exponent"
            }
            else -> throw IllegalArgumentException("Invalid float format: $str")
        }
    }
}
