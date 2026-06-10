package com.jsoizo.serialization.php

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
class PHPEncoder(
    config: PHPConfig = PHPConfig(),
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = config.serializersModule

    private val sb = StringBuilder()

    // True while the next value to encode is a map key. PHP array keys can
    // only be integers or strings; anything else must be rejected up front
    // because PHP's unserialize would fail on the produced output.
    private var isEncodingMapKey = false

    private fun requireNotMapKey(type: String) {
        if (isEncodingMapKey) {
            throw SerializationException(
                "PHP array keys must be Int, Long, String or Char, but $type was used as a map key",
            )
        }
    }

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
        requireNotMapKey("null")
        sb.append("N;")
    }

    override fun encodeBoolean(value: Boolean) {
        requireNotMapKey("Boolean")
        sb.append("b:${if (value) 1 else 0};")
    }

    override fun encodeInt(value: Int) {
        sb.append("i:$value;")
    }

    override fun encodeLong(value: Long) {
        sb.append("i:$value;")
    }

    override fun encodeFloat(value: Float) {
        requireNotMapKey("Float")
        sb.append("d:${ValueFormatter.formatFloat(value)};")
    }

    override fun encodeDouble(value: Double) {
        requireNotMapKey("Double")
        sb.append("d:${ValueFormatter.formatDouble(value)};")
    }

    override fun encodeChar(value: Char) {
        encodeString(value.toString())
    }

    override fun encodeString(value: String) {
        val bytes = value.encodeToByteArray()
        sb.append("s:${bytes.size}:\"$value\";")
    }

    override fun encodeEnum(
        enumDescriptor: SerialDescriptor,
        index: Int,
    ) {
        requireNotMapKey("enum ${enumDescriptor.serialName}")
        val serialName = enumDescriptor.phpClassName()
        val name = enumDescriptor.getElementName(index)
        val value = "$serialName:$name"
        sb.append("E:${value.encodeToByteArray().size}:\"$value\";")
    }

    override fun beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Int,
    ): CompositeEncoder {
        requireNotMapKey(descriptor.serialName)
        when (descriptor.kind) {
            StructureKind.LIST -> sb.append("a:$collectionSize:{")
            StructureKind.MAP -> sb.append("a:$collectionSize:{")
            else -> throw IllegalArgumentException("Unsupported collection kind: ${descriptor.kind}")
        }
        return this
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        requireNotMapKey(descriptor.serialName)
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
        isEncodingMapKey = descriptor.kind == StructureKind.MAP && index % 2 == 0
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
