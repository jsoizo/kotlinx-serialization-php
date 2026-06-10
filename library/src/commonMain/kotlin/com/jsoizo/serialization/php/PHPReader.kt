package com.jsoizo.serialization.php

import kotlinx.serialization.SerializationException

/**
 * Cursor over the raw bytes of a PHP-serialized payload.
 *
 * The PHP serialization format declares string lengths in bytes, so all
 * positions are tracked as byte offsets. Every read is bounds-checked and
 * fails with [SerializationException] on truncated input.
 */
internal class PHPReader(input: String) {
    private val bytes: ByteArray = input.encodeToByteArray()

    private var pos: Int = 0

    fun hasMore(): Boolean = pos < bytes.size

    fun peek(): Char {
        if (pos >= bytes.size) throw SerializationException("Unexpected end of input at byte $pos")
        return bytes[pos].toInt().toChar()
    }

    fun expect(char: Char) {
        val actual = peek()
        if (actual != char) {
            throw SerializationException("Expected '$char' at byte $pos, but found '$actual'")
        }
        pos++
    }

    fun startsWith(prefix: String): Boolean {
        if (pos + prefix.length > bytes.size) return false
        return prefix.indices.all { bytes[pos + it].toInt().toChar() == prefix[it] }
    }

    fun readUntil(delimiter: Char): String {
        val start = pos
        while (pos < bytes.size && bytes[pos].toInt().toChar() != delimiter) {
            pos++
        }
        if (pos >= bytes.size) {
            throw SerializationException("Expected '$delimiter' but reached end of input")
        }
        return bytes.decodeToString(start, pos)
    }

    fun readIntUntil(delimiter: Char): Int {
        val raw = readUntil(delimiter)
        return raw.toIntOrNull() ?: throw SerializationException("Expected an integer but found '$raw'")
    }

    fun readLongUntil(delimiter: Char): Long {
        val raw = readUntil(delimiter)
        return raw.toLongOrNull() ?: throw SerializationException("Expected an integer but found '$raw'")
    }

    /**
     * Reads the `:<byte length>:"<bytes>"` portion shared by `s`, `E` and `O` values.
     * The declared length counts bytes, not characters.
     */
    fun readLengthPrefixedString(): String {
        expect(':')
        val length = readIntUntil(':')
        if (length < 0) throw SerializationException("Invalid string length: $length")
        expect(':')
        expect('"')
        if (pos + length > bytes.size) {
            throw SerializationException("Declared string length $length exceeds remaining input")
        }
        val result = bytes.decodeToString(pos, pos + length)
        pos += length
        expect('"')
        return result
    }
}
