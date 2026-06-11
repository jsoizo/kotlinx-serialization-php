package com.jsoizo.serialization.php

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.char
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.hiragana
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.printableAscii
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Encode/decode round-trip properties for strings and chars. The PHP format
 * declares string lengths in bytes, so the generators deliberately mix
 * multi-byte characters, astral-plane codepoints (surrogate pairs), NUL and
 * the format's own delimiter characters.
 */
class PHPStringRoundTripPropertyTest {
    private val anyCodepoint: Arb<Codepoint> =
        Arb.choice(
            Codepoint.printableAscii(),
            Codepoint.hiragana(),
            // Emoji block: astral-plane codepoints become surrogate pairs in UTF-16.
            Arb.int(0x1F300..0x1F64F).map(::Codepoint),
            // Delimiters of the serialization format itself, plus NUL.
            Arb.of(';', ':', '"', '{', '}', '\u0000').map { Codepoint(it.code) },
        )

    // 0..300 codepoints: spans the 100-character chunk size of the old
    // decoder implementation, where multi-byte handling used to break.
    private val unicodeStrings: Arb<String> = Arb.string(0..300, anyCodepoint)

    @Test
    fun stringRoundTripsTest() {
        runBlocking {
            checkAll(unicodeStrings) { value ->
                assertEquals(value, PHP.decodeFromString<String>(PHP.encodeToString(value)))
            }
        }
    }

    @Test
    fun charRoundTripsTest() {
        runBlocking {
            // Lone surrogates are not representable in UTF-8, so only valid
            // BMP scalar values are generated.
            val bmpChars =
                Arb.choice(
                    Arb.char('\u0020'..'\uD7FF'),
                    Arb.char('\uE000'..'\uFFFD'),
                )
            checkAll(bmpChars) { value ->
                assertEquals(value, PHP.decodeFromString<Char>(PHP.encodeToString(value)))
            }
        }
    }
}
