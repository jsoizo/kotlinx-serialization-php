package com.jsoizo.serialization.php

import kotlin.test.Test
import kotlin.test.assertEquals

class ValueFormatterTest {
    @Test
    fun formatFloat() {
        val formatter = ValueFormatter()
        val result = formatter.formatFloat(Float.MAX_VALUE)
        assertEquals("3.4028235E38", result)
    }

    @Test
    fun formatDouble() {
        val formatter = ValueFormatter()
        val result = formatter.formatDouble(Double.MAX_VALUE)
        assertEquals("1.7976931348623157E308", result)
    }
}
