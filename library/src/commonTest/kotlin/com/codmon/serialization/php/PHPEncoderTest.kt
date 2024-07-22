package com.codmon.serialization.php

import com.codmon.serialization.php.testdata.SimpleClass
import com.codmon.serialization.php.testdata.TestEnum
import com.codmon.serialization.php.testdata.TestSealed
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class PHPEncoderTest {
    @Test
    fun encodeCharTest() {
        val char = 'a'
        val result = PHP.encodeToString(char)
        assertEquals("s:1:\"a\";", result)
    }

    @Test
    fun encodeStringTest() {
        val str = "Hello, World!"
        val result = PHP.encodeToString(str)
        assertEquals("s:13:\"Hello, World!\";", result)
    }

    @Test
    fun encodeMultiByteIncludingLigatureEmojiStringTest() {
        val str = "こんにちは、せかい！ 👨‍👩‍👦‍👦"
        val result = PHP.encodeToString(str)
        assertEquals("s:56:\"こんにちは、せかい！ 👨‍👩‍👦‍👦\";", result)
    }

    @Test
    fun encodeIntTest() {
        val int = Int.MAX_VALUE
        val result = PHP.encodeToString(int)
        assertEquals("i:2147483647;", result)
    }

    @Test
    fun encodeLongTest() {
        val long = Long.MAX_VALUE
        val result = PHP.encodeToString(long)
        assertEquals("i:9223372036854775807;", result)
    }

    @Test
    fun encodeFloatTest() {
        val float = Float.MAX_VALUE
        val result = PHP.encodeToString(float)
        assertEquals("d:3.4028235E38;", result)
    }

    @Test
    fun encodeDoubleTest() {
        val double = Double.MAX_VALUE
        val result = PHP.encodeToString(double)
        assertEquals("d:1.7976931348623157E308;", result)
    }

    @Test
    fun encodeEnumTest() {
        val testEnum: TestEnum = TestEnum.EnumB
        val result = PHP.encodeToString(testEnum)
        assertEquals("E:14:\"TestEnum:EnumB\";", result)
    }

    @Test
    fun encodeSimpleListTest() {
        val list = listOf(1, 2, 3)
        val result = PHP.encodeToString(list)
        assertEquals("a:3:{i:0;i:1;i:1;i:2;i:2;i:3;}", result)
    }

    @Test
    fun encodeNestedListTest() {
        val list = listOf(listOf(1, 2), listOf(3, 4, 5))
        val result = PHP.encodeToString(list)
        assertEquals("a:2:{i:0;a:2:{i:0;i:1;i:1;i:2;}i:1;a:3:{i:0;i:3;i:1;i:4;i:2;i:5;}}", result)
    }

    @Test
    fun encodeMapTest() {
        val map = mapOf("key1" to 1, "key2" to 2)
        val result = PHP.encodeToString(map)
        assertEquals("a:2:{s:4:\"key1\";i:1;s:4:\"key2\";i:2;}", result)
    }

    @Test
    fun encodeNestedMapTest() {
        val map = mapOf("key1" to mapOf("key2" to 2), "key3" to mapOf("key4" to 4, "key5" to 5))
        val result = PHP.encodeToString(map)
        val expected = "a:2:{s:4:\"key1\";a:1:{s:4:\"key2\";i:2;}s:4:\"key3\";a:2:{s:4:\"key4\";i:4;s:4:\"key5\";i:5;}}"
        assertEquals(expected, result)
    }

    @Test
    fun encodeClassTest() {
        val testClass = SimpleClass(123, "hello")
        val result = PHP.encodeToString(testClass)
        val expected = "O:11:\"SimpleClass\":2:{s:1:\"a\";i:123;s:1:\"b\";s:5:\"hello\";}"
        assertEquals(expected, result)
    }

    @Test
    fun encodeSealedClass() {
        val sealedClass: TestSealed = TestSealed.SealedA(1)
        val result = PHP.encodeToString(sealedClass)
        val expected = "O:7:\"SealedA\":1:{s:1:\"a\";i:1;}"
        assertEquals(expected, result)
    }
}
