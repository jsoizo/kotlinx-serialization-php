package com.jsoizo.serialization.php

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalSerializationApi::class)
class PHPEncoder(
    override val serializersModule: SerializersModule = EmptySerializersModule(),
) : AbstractEncoder() {
    private val sb = StringBuilder()

    fun getOutput(): String {
        return sb.toString()
    }

    private fun SerialDescriptor.phpClassName(): String {
        val name = this.serialName.substringAfterLast('.')
        // PSR-1 class name validation
        val nameCheckRegex = Regex("^[A-Z][a-zA-Z0-9]*\$")
        if (!nameCheckRegex.matches(name)) {
            throw IllegalArgumentException("Invalid PHP class name: $name. Must be a valid PSR-1 class name")
        }
        return name
    }

    override fun encodeNull() {
        sb.append("N;")
    }

    override fun encodeBoolean(value: Boolean) {
        sb.append("b:${if (value) 1 else 0};")
    }

    override fun encodeInt(value: Int) {
        sb.append("i:$value;")
    }

    override fun encodeLong(value: Long) {
        sb.append("i:$value;")
    }

    override fun encodeFloat(value: Float) {
        sb.append("d:${floatFormat(value)};")
    }

    private val floatScale = 10.0.pow(7)

    /**
     * 1. Convert the scientific notation e+ to E in Kotlin/JS and Kotlin/Wasm.
     * 2. Round the mantissa to 7 decimal places in Kotlin/Wasm.
     */
    private fun floatFormat(value: Float): String {
        val str = value.toString()
        val parts = str.split("e+")
        when (parts.size) {
            1 -> return str
            2 -> {
                val mantissa = parts[0].toDouble()
                val exponent = parts[1]
                val roundedMantissa = (mantissa * floatScale).roundToInt() / floatScale
                return "${roundedMantissa}E$exponent"
            }
            else -> throw IllegalArgumentException("Invalid float format: $str")
        }
    }

    override fun encodeDouble(value: Double) {
        sb.append("d:${doubleFormat(value)};")
    }

    /**
     *  Convert the scientific notation e+ to E in Kotlin/JS and Kotlin/Wasm.
     */
    private fun doubleFormat(value: Double): String {
        return value.toString().replace("e+", "E")
    }

    override fun encodeChar(value: Char) {
        sb.append("s:1:\"$value\";")
    }

    override fun encodeString(value: String) {
        val bytes = value.encodeToByteArray()
        sb.append("s:${bytes.size}:\"$value\";")
    }

    override fun encodeEnum(
        enumDescriptor: SerialDescriptor,
        index: Int,
    ) {
        val serialName = enumDescriptor.phpClassName()
        val name = enumDescriptor.getElementName(index)
        sb.append("E:${serialName.length + name.length + 1}:\"$serialName:${name}\";")
    }

    override fun beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Int,
    ): CompositeEncoder {
        when (descriptor.kind) {
            StructureKind.LIST -> sb.append("a:$collectionSize:{")
            StructureKind.MAP -> sb.append("a:$collectionSize:{")
            else -> throw IllegalArgumentException("Unsupported collection kind: ${descriptor.kind}")
        }
        return this
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        when (descriptor.kind) {
            StructureKind.CLASS -> {
                val name = descriptor.phpClassName()
                val elementsCount = descriptor.elementsCount
                sb.append("O:${name.length}:\"$name\":$elementsCount:{")
            }
            PolymorphicKind.SEALED -> {
                // do nothing
            }
            else -> throw IllegalArgumentException("Unsupported structure kind: ${descriptor.kind}")
        }
        return this
    }

    override fun encodeElement(
        descriptor: SerialDescriptor,
        index: Int,
    ): Boolean {
        if (descriptor.kind == StructureKind.CLASS) {
            encodeString(descriptor.getElementName(index))
        }
        if (descriptor.kind == StructureKind.LIST) {
            sb.append("i:$index;")
        }
        if (descriptor.kind == PolymorphicKind.SEALED) {
            return descriptor.getElementName(index) == "value"
        }
        return true
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (descriptor.kind == PolymorphicKind.SEALED) {
            // do nothing
            return
        }
        sb.append("}")
    }
}
