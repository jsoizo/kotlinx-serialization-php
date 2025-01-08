package com.jsoizo.serialization.php

actual class ValueFormatter actual constructor() : IValueFormatter {
    private fun String.ePlusToE(): String = this.replace("e+", "E")

    override fun formatFloat(value: Float): String = value.toString().ePlusToE()

    override fun formatDouble(value: Double): String = value.toString().ePlusToE()
}
