package com.jsoizo.serialization.php

import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
class PHPDecoder(
    private val input: String,
    override val serializersModule: SerializersModule = EmptySerializersModule(),
    private val decodeStringChunkSize: Int = 100,
) : AbstractDecoder() {
    private var position = 0

    private data class StructureInfo(
        var size: Int = -1,
        var index: Int = 0,
        var isDecodingKey: Boolean = false,
    )

    private val structureStack = mutableListOf<StructureInfo>()

    class CurrentStructureNotFoundException : SerializationException("Current structure not found in stack")

    private fun currentStructure(): StructureInfo? = structureStack.lastOrNull()

    private fun readUntil(delimiter: Char): String {
        val start = position
        while (position < input.length && input[position] != delimiter) {
            position++
        }
        return input.substring(start, position)
    }

    private fun expect(char: Char) {
        if (input[position] != char) throw SerializationException("Expected '$char', but found '${input[position]}'")
        position++
    }

    override fun decodeNotNullMark(): Boolean {
        return !input.startsWith("N;", position)
    }

    override fun decodeNull(): Nothing? {
        expect('N')
        expect(';')
        return null
    }

    override fun decodeBoolean(): Boolean {
        expect('b')
        expect(':')
        val value = readUntil(';').toInt()
        expect(';')
        return value == 1
    }

    override fun decodeInt(): Int {
        expect('i')
        expect(':')
        val value = readUntil(';').toInt()
        expect(';')
        return value
    }

    override fun decodeLong(): Long {
        expect('i')
        expect(':')
        val value = readUntil(';').toLong()
        expect(';')
        return value
    }

    override fun decodeFloat(): Float {
        expect('d')
        expect(':')
        val value = readUntil(';').toFloat()
        expect(';')
        return value
    }

    override fun decodeDouble(): Double {
        expect('d')
        expect(':')
        val value = readUntil(';').toDouble()
        expect(';')
        return value
    }

    override fun decodeChar(): Char {
        expect('s')
        expect(':')
        expect('1')
        expect(':')
        expect('"')
        val char = input[position]
        position++
        expect('"')
        expect(';')
        return char
    }

    private fun decodeStringWithoutType(): String {
        expect(':')
        val length = readUntil(':').toInt()
        expect(':')
        expect('"')

        val result = buildString { appendStringRecursive(length) }
        expect('"')
        return result
    }

    override fun decodeString(): String {
        expect('s')
        val result = decodeStringWithoutType()
        expect(';')
        return result
    }

    private tailrec fun StringBuilder.appendStringRecursive(remainingLength: Int) {
        if (remainingLength <= 0) return

        val chunkSize = decodeStringChunkSize
        val endPosition = (position + chunkSize).coerceAtMost(input.length)
        val chunk = input.substring(position, endPosition)
        val readBytes = chunk.encodeToByteArray()
        val bytesToRead = minOf(remainingLength, readBytes.size)
        val newReadBytes = readBytes.sliceArray(0 until bytesToRead)
        val readChars = newReadBytes.decodeToString()

        append(readChars)
        position += readChars.length

        appendStringRecursive(remainingLength - bytesToRead)
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        expect('E')
        val fullName = decodeStringWithoutType()
        expect(';')
        val name = fullName.substringAfterLast(':')
        return enumDescriptor.getElementIndex(name)
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        val current = currentStructure() ?: throw CurrentStructureNotFoundException()
        if (current.size == -1) {
            expect('a')
            expect(':')
            current.size = readUntil(':').toInt()
            expect(':')
            expect('{')
        }
        return current.size
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (position >= input.length || input[position] == '}') return CompositeDecoder.DECODE_DONE

        val current = currentStructure() ?: return throw CurrentStructureNotFoundException()
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
                if (current.index > descriptor.elementsCount) {
                    return CompositeDecoder.DECODE_DONE
                }
                val fieldName = decodeString()
                val index = descriptor.getElementIndex(fieldName)
                current.index++
                return index
            }
            else -> throw IllegalArgumentException("Unsupported structure kind: ${descriptor.kind}")
        }
    }

    private fun skipArrayKey() {
        when (input[position]) {
            'i' -> {
                decodeLong()
            }
            's' -> {
                decodeString()
            }
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
                expect('O')
                decodeStringWithoutType()
                expect(':')
                readUntil('{')
                expect('{')
            }
            else -> throw IllegalArgumentException("Unsupported structure kind: ${descriptor.kind}")
        }
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        expect('}')
        structureStack.removeLastOrNull()
    }
}
