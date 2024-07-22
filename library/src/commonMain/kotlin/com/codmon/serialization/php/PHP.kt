package com.codmon.serialization.php

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

sealed class PHP(
    val config: PHPConfig,
    override val serializersModule: SerializersModule,
) : StringFormat {
    constructor(config: PHPConfig) : this(
        config = config,
        serializersModule = config.serializersModule,
    )

    companion object Default : PHP(PHPConfig())

    override fun <T> encodeToString(
        serializer: SerializationStrategy<T>,
        value: T,
    ): String {
        val encoder = PHPEncoder()
        encoder.encodeSerializableValue(serializer, value)
        return encoder.getOutput()
    }

    override fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        string: String,
    ): T {
        val decoder = PHPDecoder(string)
        return decoder.decodeSerializableValue(deserializer)
    }
}

fun PHP(
    from: PHP = PHP,
    builderAction: PHPConfigBuilder.() -> Unit,
): PHP {
    val builder = PHPConfigBuilder(PHP)
    builder.builderAction()
    val config = builder.build()
    return PHPImpl(config)
}

private class PHPImpl(config: PHPConfig) : PHP(config)

class PHPConfig(
    val serializersModule: SerializersModule = EmptySerializersModule(),
)

class PHPConfigBuilder internal constructor(php: PHP) {
    val config = php.config
    var serializersModule: SerializersModule = config.serializersModule

    fun build(): PHPConfig =
        PHPConfig(
            serializersModule = serializersModule,
        )
}
