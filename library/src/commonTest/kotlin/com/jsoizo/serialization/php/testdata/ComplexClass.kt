package com.jsoizo.serialization.php.testdata

import kotlinx.serialization.Serializable

@Serializable
data class ComplexClass(val a: Int, val b: List<SimpleClass>, val c: String)
