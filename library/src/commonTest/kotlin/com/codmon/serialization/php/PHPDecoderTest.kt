package com.codmon.serialization.php

import com.codmon.serialization.php.testdata.ComplexClass
import com.codmon.serialization.php.testdata.SimpleClass
import com.codmon.serialization.php.testdata.TestEnum
import com.codmon.serialization.php.testdata.TestSealed
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PHPDecoderTest {
    @Test
    fun decodeNullTest() {
        val input = "N;"
        val result = PHP.decodeFromString<String?>(input)
        assertNull(result)
    }

    @Test
    fun decodeBooleanTest() {
        val input = "b:1;"
        val result = PHP.decodeFromString<Boolean>(input)
        assertEquals(true, result)
    }

    @Test
    fun decodeBooleanFalseTest() {
        val input = "b:0;"
        val result = PHP.decodeFromString<Boolean>(input)
        assertEquals(false, result)
    }

    @Test
    fun decodeIntTest() {
        val input = "i:2147483647;"
        val result = PHP.decodeFromString<Int>(input)
        assertEquals(Int.MAX_VALUE, result)
    }

    @Test
    fun decodeLongTest() {
        val input = "i:9223372036854775807;"
        val result = PHP.decodeFromString<Long>(input)
        assertEquals(Long.MAX_VALUE, result)
    }

    @Test
    fun decodeFloatTest() {
        val input = "d:3.4028235E38;"
        val result = PHP.decodeFromString<Float>(input)
        assertEquals(Float.MAX_VALUE, result)
    }

    @Test
    fun decodeDoubleTest() {
        val input = "d:1.7976931348623157E308;"
        val result = PHP.decodeFromString<Double>(input)
        assertEquals(Double.MAX_VALUE, result)
    }

    @Test
    fun decodeCharTest() {
        val input = "s:1:\"a\";"
        val result = PHP.decodeFromString<Char>(input)
        assertEquals('a', result)
    }

    @Test
    fun decodeStringTest() {
        val input = "s:13:\"Hello, World!\";"
        val result = PHP.decodeFromString<String>(input)
        assertEquals("Hello, World!", result)
    }

    @Test
    fun decodeMultiByteIncludingLigatureEmojiStringTest() {
        val input = "s:56:\"こんにちは、せかい！ 👨‍👩‍👦‍👦\";"
        val result = PHP.decodeFromString<String>(input)
        assertEquals("こんにちは、せかい！ 👨‍👩‍👦‍👦", result)
    }

    @Test
    fun decodeEnumTest() {
        val input = "E:14:\"TestEnum:EnumB\";"
        val result = PHP.decodeFromString<TestEnum>(input)
        assertEquals(TestEnum.EnumB, result)
    }

    @Test
    fun decodeSimpleListTest() {
        val input = "a:3:{i:0;i:1;i:1;i:2;i:2;i:3;}"
        val result = PHP.decodeFromString<List<Int>>(input)
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun decodeNestedListTest() {
        val input = "a:2:{i:0;a:2:{i:0;i:1;i:1;i:2;}i:1;a:2:{i:0;i:3;i:1;i:4;}}"
        val result = PHP.decodeFromString<List<List<Int>>>(input)
        assertEquals(listOf(listOf(1, 2), listOf(3, 4)), result)
    }

    @Test
    fun decodeSimpleMapTest() {
        val input = "a:2:{s:4:\"key1\";i:1;s:4:\"key2\";i:2;}"
        val result = PHP.decodeFromString<Map<String, Int>>(input)
        assertEquals(mapOf("key1" to 1, "key2" to 2), result)
    }

    @Test
    fun decodeNestedMapTest() {
        val input = "a:2:{s:4:\"key1\";a:2:{s:1:\"a\";i:1;s:1:\"b\";i:2;}s:4:\"key2\";a:2:{s:1:\"c\";i:3;s:1:\"d\";i:4;}}"
        val result = PHP.decodeFromString<Map<String, Map<String, Int>>>(input)
        assertEquals(mapOf("key1" to mapOf("a" to 1, "b" to 2), "key2" to mapOf("c" to 3, "d" to 4)), result)
    }

    @Test
    fun decodeClassTest() {
        val input = "O:11:\"SimpleClass\":2:{s:1:\"a\";i:1;s:1:\"b\";s:5:\"value\";}"
        val result = PHP.decodeFromString<SimpleClass>(input)
        assertEquals(SimpleClass(1, "value"), result)
    }

    @Test
    fun decodeComplexClassTest() {
        val input = "O:12:\"ComplexClass\":3:{s:1:\"a\";i:1;s:1:\"b\";a:2:{i:0;O:11:\"SimpleClass\":2:{s:1:\"a\";i:2;s:1:\"b\";s:3:\"foo\";}i:1;O:11:\"SimpleClass\":2:{s:1:\"a\";i:3;s:1:\"b\";s:3:\"bar\";}}s:1:\"c\";s:3:\"baz\";}"
        val result = PHP.decodeFromString<ComplexClass>(input)
        assertEquals(ComplexClass(1, listOf(SimpleClass(2, "foo"), SimpleClass(3, "bar")), "baz"), result)
    }

    @Test
    fun decodeSealedClassTest() {
        val input = "O:7:\"SealedA\":1:{s:1:\"a\";i:1;}"
        assertFailsWith<IllegalArgumentException> {
            PHP.decodeFromString<TestSealed>(input)
        }
    }
}
