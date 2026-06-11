package com.jsoizo.serialization.php

import com.jsoizo.serialization.php.testdata.SimpleClass
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.ascii
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.char
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.hiragana
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

/**
 * Exception-contract properties for the decoder: any input either decodes
 * successfully or fails with SerializationException. Nothing else may leak
 * (no infinite loop, no IndexOutOfBounds/NumberFormatException) — the
 * invariant established by the byte-based PHPReader.
 */
class PHPDecoderRobustnessPropertyTest {
    private fun assertDecodesOrFailsCleanly(block: () -> Any?) {
        try {
            block()
        } catch (e: SerializationException) {
            // The only permitted failure mode.
        } catch (e: Throwable) {
            fail("Expected SerializationException but got ${e::class.simpleName}: ${e.message}")
        }
    }

    @Test
    fun arbitraryInputNeverLeaksUnexpectedExceptionsTest() {
        runBlocking {
            checkAll(Arb.string(0..100, Codepoint.ascii())) { input ->
                assertDecodesOrFailsCleanly { PHP.decodeFromString<String>(input) }
                assertDecodesOrFailsCleanly { PHP.decodeFromString<Int>(input) }
                assertDecodesOrFailsCleanly { PHP.decodeFromString<Double>(input) }
                assertDecodesOrFailsCleanly { PHP.decodeFromString<List<Int>>(input) }
                assertDecodesOrFailsCleanly { PHP.decodeFromString<Map<String, String>>(input) }
                assertDecodesOrFailsCleanly { PHP.decodeFromString<SimpleClass>(input) }
            }
        }
    }

    private val validPayloads: Arb<String> =
        Arb.bind(
            Arb.int(),
            // Multi-byte content so that truncation can cut inside a
            // character of a declared-length string.
            Arb.string(0..50, Codepoint.hiragana()),
        ) { a, b -> PHP.encodeToString(SimpleClass(a, b)) }

    @Test
    fun truncatedPayloadAlwaysFailsWithSerializationExceptionTest() {
        runBlocking {
            val truncations =
                validPayloads.flatMap { payload ->
                    Arb.int(0 until payload.length).map { cut -> payload.substring(0, cut) }
                }
            checkAll(truncations) { truncated ->
                // Every strict prefix is incomplete: it must fail, and with
                // SerializationException rather than looping or crashing.
                assertFailsWith<SerializationException> {
                    PHP.decodeFromString<SimpleClass>(truncated)
                }
            }
        }
    }

    @Test
    fun mutatedPayloadNeverLeaksUnexpectedExceptionsTest() {
        runBlocking {
            val mutations =
                validPayloads.flatMap { payload ->
                    Arb.bind(
                        Arb.int(payload.indices),
                        Arb.char(' '..'~'),
                    ) { position, replacement ->
                        payload.substring(0, position) + replacement + payload.substring(position + 1)
                    }
                }
            checkAll(mutations) { mutated ->
                // A single-character corruption may still decode (e.g. inside
                // string content) or fail, but only with SerializationException.
                assertDecodesOrFailsCleanly { PHP.decodeFromString<SimpleClass>(mutated) }
            }
        }
    }
}
