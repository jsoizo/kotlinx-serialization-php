package com.jsoizo.serialization.php

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs on every target: the expected strings are identical across platforms
 * so that serialized output is platform-stable.
 */
class ValueFormatterTest {
    @Test
    fun formatFloatMaxValueTest() {
        assertEquals("3.4028235E38", ValueFormatter.formatFloat(Float.MAX_VALUE))
    }

    @Test
    fun formatDoubleMaxValueTest() {
        assertEquals("1.7976931348623157E308", ValueFormatter.formatDouble(Double.MAX_VALUE))
    }

    @Test
    fun formatPlainDoubleTest() {
        assertEquals("0.0", ValueFormatter.formatDouble(0.0))
        assertEquals("1.0", ValueFormatter.formatDouble(1.0))
        assertEquals("-1.5", ValueFormatter.formatDouble(-1.5))
        assertEquals("0.1", ValueFormatter.formatDouble(0.1))
        assertEquals("3.14159", ValueFormatter.formatDouble(3.14159))
        assertEquals("123.456", ValueFormatter.formatDouble(123.456))
    }

    @Test
    fun formatPlainFloatTest() {
        assertEquals("0.0", ValueFormatter.formatFloat(0.0f))
        assertEquals("1.0", ValueFormatter.formatFloat(1.0f))
        // Kotlin/JS prints Float values with Double precision
        // (e.g. "3.140000104904175"); the formatter must shorten them back.
        assertEquals("3.14", ValueFormatter.formatFloat(3.14f))
        assertEquals("-0.5", ValueFormatter.formatFloat(-0.5f))
    }

    @Test
    fun formatNotationBoundaryTest() {
        // Follows the JVM toString() convention: plain notation for
        // 1e-3 <= |value| < 1e7, scientific notation outside of it.
        assertEquals("1000000.0", ValueFormatter.formatDouble(1e6))
        assertEquals("1.0E7", ValueFormatter.formatDouble(1e7))
        assertEquals("0.001", ValueFormatter.formatDouble(1e-3))
        assertEquals("1.0E-4", ValueFormatter.formatDouble(1e-4))
        assertEquals("1.0E20", ValueFormatter.formatDouble(1e20))
        assertEquals("1.5E-7", ValueFormatter.formatDouble(1.5e-7))
        assertEquals("-1.0E21", ValueFormatter.formatDouble(-1e21))
    }

    @Test
    fun formatSpecialValuesTest() {
        assertEquals("NAN", ValueFormatter.formatDouble(Double.NaN))
        assertEquals("INF", ValueFormatter.formatDouble(Double.POSITIVE_INFINITY))
        assertEquals("-INF", ValueFormatter.formatDouble(Double.NEGATIVE_INFINITY))
        assertEquals("NAN", ValueFormatter.formatFloat(Float.NaN))
        assertEquals("INF", ValueFormatter.formatFloat(Float.POSITIVE_INFINITY))
        assertEquals("-INF", ValueFormatter.formatFloat(Float.NEGATIVE_INFINITY))
    }

    @Test
    fun formatRoundTripTest() {
        val doubles = listOf(0.3, 2.5e-10, 6.62607015e-34, 9.109383701528e-31, 12345.6789)
        doubles.forEach { value ->
            assertEquals(value, ValueFormatter.formatDouble(value).toDouble())
        }
        val floats = listOf(0.3f, 1.17549435e-38f, 1.5e10f)
        floats.forEach { value ->
            assertEquals(value, ValueFormatter.formatFloat(value).toFloat())
        }
    }
}
