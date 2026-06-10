# kotlinx-serialization-php

[![Maven Central Version](https://img.shields.io/maven-central/v/com.jsoizo/kotlinx-serialization-php)](https://central.sonatype.com/artifact/com.jsoizo/kotlinx-serialization-php/overview)
[![codecov](https://codecov.io/gh/jsoizo/kotlinx-serialization-php/graph/badge.svg?token=akOxbdLONY)](https://codecov.io/gh/jsoizo/kotlinx-serialization-php)

## About

This is a Kotlin Multiplatform library that provides [PHP serialization](https://www.php.net/manual/en/function.serialize.php) format support for kotlinx.serialization.

## Installation

Add the following dependency to your `build.gradle.kts` or `build.gradle` file:

```kotlin
dependencies {
    implementation("com.jsoizo:kotlinx-serialization-php:0.2.1")
}
```

## Usage

encoding

```kotlin
@Serializable
data class User(val id: Int, val name: String)

val user = User(id = 100, name = "John")

val encoded = PHP.encodeToString(user)
// "O:4:\"User\":2:{s:2:\"id\";i:100;s:4:\"name\";s:4:\"John\";}"
```

decoding

```kotlin
val encoded = "O:4:\"User\":2:{s:2:\"id\";i:100;s:4:\"name\";s:4:\"John\";}"

val decodedUser = PHP.decodeFromString<User>(encoded)
// User(id=100, name="John")
```

To customize the behavior, build a configured instance with the `PHP {}` builder:

```kotlin
val php = PHP {
    // Skip fields in the PHP data that the Kotlin class does not declare
    // (default: false, unknown fields raise SerializationException)
    ignoreUnknownKeys = true
    // Register contextual/custom serializers
    serializersModule = SerializersModule { /* ... */ }
}

val decoded = php.decodeFromString<User>(encoded)
```

PHP serializes `protected` properties as `"\0*\0name"` and `private` ones as
`"\0ClassName\0name"`; these are automatically matched to the Kotlin property
by their bare name when decoding.

To see more usage and examples, please refer to the [kotlinx.serialization documentation](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/basic-serialization.md)

## Supported types

Kotlin types that can be serialized to and deserialized from PHP serialization format.

| Kotlin type                | Supported                 | PHP Serialization type |
|----------------------------|---------------------------|------------------------|
| `String`                   | ✅                         | `s`                    |
| `Char`                     | ✅                         | `s`                    |
| `Int`                      | ✅                         | `i`                    |
| `Long`                     | ✅                         | `i`                    |
| `Float`                    | ✅                         | `d`                    |
| `Double`                   | ✅                         | `d`                    |
| `Boolean`                  | ✅                         | `b`                    |
| `List`                     | ✅                         | `a`                    |
| `Map`                      | ✅                         | `a`                    |
| `Set`                      | 🚫                        |                        |
| `class with @Serializable` | ✅                         | `O`                    |
| `enum class`               | ✅                         | `E`                    |
| `sealed class`             | ⚠️ Only supports encoding | `O`                    |
| `Nullable`                 | ✅                         | `N`                    |

`Float.NaN` and infinities are mapped to PHP's `NAN` / `INF` / `-INF` in both
directions. Map keys must be `Int`, `Long`, `String` or `Char` because PHP
array keys can only be integers or strings; other key types fail with
`SerializationException`.

## Supported Platforms

- JVM
- Android
- Native (iOS, macOS, Linux, Windows)
- JS
- WebAssembly (wasm-js, wasm-wasi)

## Limitations

- **Sealed classes are encode-only.** The output contains no type
  discriminator, so the original subclass cannot be recovered when decoding —
  neither by this library nor by PHP.
- **Class names are not validated or mapped.** Encoding uses the Kotlin class's
  simple name (PSR-1 validated); decoding ignores the class name in the input
  entirely. There is no mapping for namespaced PHP class names such as
  `App\Models\User`.
- **PHP references and custom serializable objects are not supported.**
  Payloads containing `R:` / `r:` (references) or `C:` (classes implementing
  `Serializable`) cannot be decoded.
- **List decoding ignores array keys.** Elements are read in their serialized
  order, so sparse arrays (e.g. `a:2:{i:5;...;i:9;...;}`) lose their indices.
  Decode such data into a `Map` instead.
- **Integer-like string keys.** PHP converts array keys like `"123"` to
  integers when serializing; such keys cannot be decoded into a
  `Map<String, V>` (use `Map<Long, V>` when keys may be numeric).
