package com.jsoizo.serialization.php

/**
 * Formats floating point values into a canonical decimal representation that
 * is identical on every Kotlin target and parseable by PHP's unserialize().
 *
 * Platform toString() outputs differ (e.g. `1.0` vs `1`, `1.0E20` vs `1e+21`,
 * and Kotlin/JS prints Float values with Double precision), so the platform
 * output is parsed into (sign, digits, exponent), shortened to the fewest
 * digits that still round-trip, and re-rendered with fixed rules matching
 * the JVM's toString() conventions. Special values use PHP's NAN/INF tokens.
 */
internal object ValueFormatter {
    fun formatFloat(value: Float): String =
        when {
            value.isNaN() -> "NAN"
            value == Float.POSITIVE_INFINITY -> "INF"
            value == Float.NEGATIVE_INFINITY -> "-INF"
            value == 0.0f -> if (1 / value < 0) "-0.0" else "0.0"
            else -> formatFinite(value.toString()) { it.toFloat() == value }
        }

    fun formatDouble(value: Double): String =
        when {
            value.isNaN() -> "NAN"
            value == Double.POSITIVE_INFINITY -> "INF"
            value == Double.NEGATIVE_INFINITY -> "-INF"
            value == 0.0 -> if (1 / value < 0) "-0.0" else "0.0"
            else -> formatFinite(value.toString()) { it.toDouble() == value }
        }

    private fun formatFinite(
        raw: String,
        roundTrips: (String) -> Boolean,
    ): String {
        val (sign, digits, exponent) = parse(raw)
        val (shortDigits, shortExponent) = shorten(digits, exponent, sign, roundTrips)
        return render(sign, shortDigits, shortExponent)
    }

    private data class Parsed(
        val sign: String,
        val digits: String,
        val exponent: Int,
    )

    /**
     * Normalizes any toString() output into significant digits plus the
     * base-10 exponent of the first digit (i.e. value = 0.D1D2... * 10^(e+1)).
     */
    private fun parse(raw: String): Parsed {
        var s = raw
        val sign = if (s.startsWith("-")) "-" else ""
        s = s.removePrefix("-")

        val eIndex = s.indexOfFirst { it == 'e' || it == 'E' }
        val mantissa = if (eIndex >= 0) s.substring(0, eIndex) else s
        val expPart = if (eIndex >= 0) s.substring(eIndex + 1).removePrefix("+").toInt() else 0

        val dotIndex = mantissa.indexOf('.')
        val intPart = if (dotIndex >= 0) mantissa.substring(0, dotIndex) else mantissa
        val fracPart = if (dotIndex >= 0) mantissa.substring(dotIndex + 1) else ""

        var digits = intPart + fracPart
        // Exponent of the first digit of intPart.
        var exponent = intPart.length - 1 + expPart

        val leadingZeros = digits.length - digits.trimStart('0').length
        digits = digits.trimStart('0')
        exponent -= leadingZeros

        digits = digits.trimEnd('0')
        if (digits.isEmpty()) {
            // Only possible for zero, which is handled by the callers.
            return Parsed(sign, "0", 0)
        }
        return Parsed(sign, digits, exponent)
    }

    /**
     * Finds the fewest significant digits that still parse back to the same
     * value. This absorbs platform differences such as Kotlin/JS printing
     * Float values with Double precision.
     */
    private fun shorten(
        digits: String,
        exponent: Int,
        sign: String,
        roundTrips: (String) -> Boolean,
    ): Pair<String, Int> {
        for (length in 1 until digits.length) {
            val (candidate, exponentShift) = roundToLength(digits, length)
            if (roundTrips(render(sign, candidate, exponent + exponentShift))) {
                return candidate to exponent + exponentShift
            }
        }
        return digits to exponent
    }

    private fun roundToLength(
        digits: String,
        length: Int,
    ): Pair<String, Int> {
        val cut = digits.substring(0, length)
        if (digits[length] < '5') {
            return cut.trimEnd('0').ifEmpty { "0" } to 0
        }
        // Round up with carry propagation.
        val rounded = cut.toCharArray()
        var i = length - 1
        while (i >= 0) {
            if (rounded[i] == '9') {
                rounded[i] = '0'
                i--
            } else {
                rounded[i] = rounded[i] + 1
                break
            }
        }
        if (i < 0) {
            // All digits carried over (e.g. 99 -> 100): one digit, exponent + 1.
            return "1" to 1
        }
        return rounded.concatToString().trimEnd('0') to 0
    }

    /**
     * Renders digits/exponent following the JVM's Double.toString() layout:
     * plain notation for exponents in -3..6, scientific notation otherwise.
     */
    private fun render(
        sign: String,
        digits: String,
        exponent: Int,
    ): String =
        when (exponent) {
            in 0..6 -> {
                val intPart = digits.padEnd(exponent + 1, '0').substring(0, exponent + 1)
                val fracPart = digits.drop(exponent + 1).ifEmpty { "0" }
                "$sign$intPart.$fracPart"
            }
            in -3..-1 -> {
                "${sign}0." + "0".repeat(-exponent - 1) + digits
            }
            else -> {
                val fracPart = digits.drop(1).ifEmpty { "0" }
                "$sign${digits[0]}.${fracPart}E$exponent"
            }
        }
}
