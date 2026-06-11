package com.jsoizo.serialization.php

import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based tests for ValueFormatter. The formatter logic is common
 * code; cross-platform output equality is covered by the exact-string
 * assertions in commonTest's ValueFormatterTest.
 */
class ValueFormatterPropertyTest {
    private val plainPattern = Regex("""-?(0|[1-9]\d*)\.(\d*[1-9]|0)""")
    private val scientificPattern = Regex("""-?[1-9]\.(\d*[1-9]|0)E(-?\d+)""")

    private fun assertCanonicalSyntax(formatted: String) {
        val scientific = scientificPattern.matchEntire(formatted)
        if (scientific != null) {
            val exponent = scientific.groupValues[2].toInt()
            assertTrue(
                exponent !in -3..6,
                "Scientific notation must not be used for exponents in -3..6: $formatted",
            )
        } else {
            assertTrue(plainPattern.matches(formatted), "Not in canonical form: $formatted")
        }
    }

    private fun assertFormatsExactly(value: Double) {
        val formatted = ValueFormatter.formatDouble(value)
        when {
            value.isNaN() -> assertEquals("NAN", formatted)
            value == Double.POSITIVE_INFINITY -> assertEquals("INF", formatted)
            value == Double.NEGATIVE_INFINITY -> assertEquals("-INF", formatted)
            else -> {
                // Bit-exact round trip, including the sign of zero.
                assertEquals(value.toRawBits(), formatted.toDouble().toRawBits(), "Round trip failed: $value -> $formatted")
                assertCanonicalSyntax(formatted)
                // The canonical form is a fixed point of format(parse(...)).
                assertEquals(formatted, ValueFormatter.formatDouble(formatted.toDouble()))
            }
        }
    }

    private fun assertFormatsExactly(value: Float) {
        val formatted = ValueFormatter.formatFloat(value)
        when {
            value.isNaN() -> assertEquals("NAN", formatted)
            value == Float.POSITIVE_INFINITY -> assertEquals("INF", formatted)
            value == Float.NEGATIVE_INFINITY -> assertEquals("-INF", formatted)
            else -> {
                assertEquals(value.toRawBits(), formatted.toFloat().toRawBits(), "Round trip failed: $value -> $formatted")
                assertCanonicalSyntax(formatted)
                assertEquals(formatted, ValueFormatter.formatFloat(formatted.toFloat()))
            }
        }
    }

    @Test
    fun formatDoubleRoundTripsTest() {
        runBlocking {
            checkAll(Arb.double()) { value -> assertFormatsExactly(value) }
        }
    }

    @Test
    fun formatFloatRoundTripsTest() {
        runBlocking {
            checkAll(Arb.float()) { value -> assertFormatsExactly(value) }
        }
    }

    @Test
    fun formatDoubleFromRawBitsRoundTripsTest() {
        runBlocking {
            // Uniform bit patterns reach subnormals and other corners that
            // value-based generators rarely produce.
            checkAll(Arb.long().map(Double::fromBits)) { value -> assertFormatsExactly(value) }
        }
    }

    @Test
    fun formatFloatFromRawBitsRoundTripsTest() {
        runBlocking {
            checkAll(Arb.int().map(Float::fromBits)) { value -> assertFormatsExactly(value) }
        }
    }
}
