package com.jsoizo.serialization.php

import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
class PHPDecoder(
    input: String,
    private val config: PHPConfig = PHPConfig(),
) : AbstractDecoder() {
    override val serializersModule: SerializersModule = config.serializersModule

    private val reader = PHPReader(input)

    private data class StructureInfo(
        var size: Int = -1,
        var index: Int = 0,
        var isDecodingKey: Boolean = false,
    )

    private val structureStack = mutableListOf<StructureInfo>()

    class CurrentStructureNotFoundException : SerializationException("Current structure not found in stack")

    private fun currentStructure(): StructureInfo? = structureStack.lastOrNull()

    override fun decodeNotNullMark(): Boolean {
        return !reader.startsWith("N;")
    }

    override fun decodeNull(): Nothing? {
        reader.expect('N')
        reader.expect(';')
        return null
    }

    override fun decodeBoolean(): Boolean {
        reader.expect('b')
        reader.expect(':')
        val value = reader.readIntUntil(';')
        reader.expect(';')
        return value == 1
    }

    override fun decodeInt(): Int {
        reader.expect('i')
        reader.expect(':')
        val value = reader.readIntUntil(';')
        reader.expect(';')
        return value
    }

    override fun decodeLong(): Long {
        reader.expect('i')
        reader.expect(':')
        val value = reader.readLongUntil(';')
        reader.expect(';')
        return value
    }

    override fun decodeFloat(): Float {
        val raw = readDoubleRaw()
        return when (raw) {
            "NAN" -> Float.NaN
            "INF" -> Float.POSITIVE_INFINITY
            "-INF" -> Float.NEGATIVE_INFINITY
            else -> raw.toFloatOrNull() ?: throw SerializationException("Invalid float value: '$raw'")
        }
    }

    override fun decodeDouble(): Double {
        val raw = readDoubleRaw()
        return when (raw) {
            "NAN" -> Double.NaN
            "INF" -> Double.POSITIVE_INFINITY
            "-INF" -> Double.NEGATIVE_INFINITY
            else -> raw.toDoubleOrNull() ?: throw SerializationException("Invalid double value: '$raw'")
        }
    }

    private fun readDoubleRaw(): String {
        reader.expect('d')
        reader.expect(':')
        val raw = reader.readUntil(';')
        reader.expect(';')
        return raw
    }

    override fun decodeChar(): Char {
        val value = decodeString()
        if (value.length != 1) {
            throw SerializationException("Expected a single character string but found \"$value\"")
        }
        return value[0]
    }

    override fun decodeString(): String {
        reader.expect('s')
        val result = reader.readLengthPrefixedString()
        reader.expect(';')
        return result
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        reader.expect('E')
        val fullName = reader.readLengthPrefixedString()
        reader.expect(';')
        val name = fullName.substringAfterLast(':')
        val index = enumDescriptor.getElementIndex(name)
        if (index == CompositeDecoder.UNKNOWN_NAME) {
            throw SerializationException("Unknown enum case '$name' for enum '${enumDescriptor.serialName}'")
        }
        return index
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        val current = currentStructure() ?: throw CurrentStructureNotFoundException()
        if (current.size == -1) {
            reader.expect('a')
            reader.expect(':')
            val size = reader.readIntUntil(':')
            if (size < 0) throw SerializationException("Invalid collection size: $size")
            current.size = size
            reader.expect(':')
            reader.expect('{')
        }
        return current.size
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (!reader.hasMore() || reader.peek() == '}') return CompositeDecoder.DECODE_DONE

        val current = currentStructure() ?: throw CurrentStructureNotFoundException()
        when (descriptor.kind) {
            StructureKind.MAP -> {
                if (current.index >= current.size * 2) {
                    return CompositeDecoder.DECODE_DONE
                }
                current.isDecodingKey = current.index % 2 == 0
                current.index++
                return current.index - 1
            }
            StructureKind.LIST -> {
                if (current.index >= current.size) {
                    return CompositeDecoder.DECODE_DONE
                }
                skipArrayKey()
                current.index++
                return current.index - 1
            }
            StructureKind.CLASS -> {
                while (true) {
                    val rawName = decodeString()
                    // PHP serializes protected properties as "\0*\0name" and
                    // private ones as "\0Class\0name"; match by the bare name.
                    val fieldName = rawName.substringAfterLast('\u0000')
                    val index = descriptor.getElementIndex(fieldName)
                    current.index++
                    if (index != CompositeDecoder.UNKNOWN_NAME) return index
                    if (!config.ignoreUnknownKeys) {
                        throw SerializationException(
                            "Unknown field '$fieldName' for class '${descriptor.serialName}'. " +
                                "Use ignoreUnknownKeys = true in PHPConfig to ignore unknown fields.",
                        )
                    }
                    reader.skipValue()
                    if (!reader.hasMore() || reader.peek() == '}') return CompositeDecoder.DECODE_DONE
                }
            }
            else -> throw IllegalArgumentException("Unsupported structure kind: ${descriptor.kind}")
        }
    }

    private fun skipArrayKey() {
        when (reader.peek()) {
            'i' -> decodeLong()
            's' -> decodeString()
            else -> throw SerializationException("Unexpected array key type")
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val newStructureInfo = StructureInfo()
        structureStack.add(newStructureInfo)
        when (descriptor.kind) {
            StructureKind.LIST, StructureKind.MAP -> {
                decodeCollectionSize(descriptor)
            }
            StructureKind.CLASS -> {
                reader.expect('O')
                reader.readLengthPrefixedString()
                reader.expect(':')
                reader.readIntUntil(':')
                reader.expect(':')
                reader.expect('{')
            }
            else -> throw IllegalArgumentException("Unsupported structure kind: ${descriptor.kind}")
        }
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        reader.expect('}')
        structureStack.removeLastOrNull()
    }
}
