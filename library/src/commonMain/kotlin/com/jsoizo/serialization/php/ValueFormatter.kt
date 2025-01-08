package com.jsoizo.serialization.php

interface IValueFormatter {
    fun formatFloat(value: Float): String = value.toString()

    fun formatDouble(value: Double): String = value.toString()
}

expect class ValueFormatter() : IValueFormatter
