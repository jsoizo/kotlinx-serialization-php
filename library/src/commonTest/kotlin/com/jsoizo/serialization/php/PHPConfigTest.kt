package com.jsoizo.serialization.php

import com.jsoizo.serialization.php.testdata.SimpleClass
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PHPConfigTest {
    data class ContextualValue(val raw: String)

    object ContextualValueSerializer : KSerializer<ContextualValue> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("ContextualValue", PrimitiveKind.STRING)

        override fun serialize(
            encoder: Encoder,
            value: ContextualValue,
        ) = encoder.encodeString(value.raw)

        override fun deserialize(decoder: Decoder): ContextualValue = ContextualValue(decoder.decodeString())
    }

    @Serializable
    data class HasContextual(
        @Contextual val value: ContextualValue,
    )

    private val phpWithContextual =
        PHP {
            serializersModule = SerializersModule { contextual(ContextualValueSerializer) }
        }

    @Test
    fun contextualSerializerIsUsedForEncodingTest() {
        val encoded = phpWithContextual.encodeToString(HasContextual(ContextualValue("hello")))
        assertEquals("O:13:\"HasContextual\":1:{s:5:\"value\";s:5:\"hello\";}", encoded)
    }

    @Test
    fun contextualSerializerIsUsedForDecodingTest() {
        val input = "O:13:\"HasContextual\":1:{s:5:\"value\";s:5:\"hello\";}"
        val decoded = phpWithContextual.decodeFromString<HasContextual>(input)
        assertEquals(HasContextual(ContextualValue("hello")), decoded)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun fromParameterInheritsConfigTest() {
        val base =
            PHP {
                serializersModule = SerializersModule { contextual(ContextualValueSerializer) }
                ignoreUnknownKeys = true
            }
        val derived = PHP(from = base) {}
        assertNotNull(derived.serializersModule.getContextual(ContextualValue::class))
        assertTrue(derived.config.ignoreUnknownKeys)
    }

    @Test
    fun ignoreUnknownKeysIsAppliedToDecodingTest() {
        val php = PHP { ignoreUnknownKeys = true }
        val input = "O:11:\"SimpleClass\":3:{s:1:\"a\";i:1;s:1:\"x\";i:9;s:1:\"b\";s:5:\"value\";}"
        assertEquals(SimpleClass(1, "value"), php.decodeFromString<SimpleClass>(input))
    }
}
