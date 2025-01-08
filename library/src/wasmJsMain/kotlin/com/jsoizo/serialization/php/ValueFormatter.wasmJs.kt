package com.jsoizo.serialization.php

actual class ValueFormatter actual constructor() : IValueFormatter, IValueFormatterWasm {
    override fun formatFloat(value: Float): String = value.formatScientificNotation()

    override fun formatDouble(value: Double): String = value.toString().ePlusToE()
}
