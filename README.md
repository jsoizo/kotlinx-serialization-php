# kotlinx-serialization-php

[![Maven Central Version](https://img.shields.io/maven-central/v/com.jsoizo/kotlinx-serialization-php)](https://central.sonatype.com/artifact/com.jsoizo/kotlinx-serialization-php/overview)
[![codecov](https://codecov.io/gh/jsoizo/kotlinx-serialization-php/graph/badge.svg?token=akOxbdLONY)](https://codecov.io/gh/jsoizo/kotlinx-serialization-php)

## About

This is a Kotlin library that provides [PHP serialization](https://www.php.net/manual/en/function.serialize.php) format support for kotlinx.serialization.  
It supports some kotlin targets(JVM, iOS, Android)

## Installation

Add the following dependency to your `build.gradle.kts` or `build.gradle` file:

```kotlin
dependencies {
    implementation("com.jsoizo:kotlinx-serialization-php:0.1.0")
}
```

## Usage

encoding

```kotlin
@Serializable
data class User(val id: Int, val name: String)

val user = User(id = 100, name = "John")

val php = PHP()
val encoded = php.encodeToString(user)
// "O:4:\"User\":2:{s:2:\"id\";i:100;s:4:\"name\";s:4:\"John\";}"
```

decoding

```kotlin
val encoded = "O:4:\"User\":2:{s:2:\"id\";i:100;s:4:\"name\";s:4:\"John\";}"

val php = PHP()
val decodedUser = php.decodeFromString<User>(encoded)
// User(id=100, name="John")
```

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
| `sealed class`             | ⚠️ Only supports encoding | `O`                     |
| `Nullable`                 | ✅                         | `N`                    |

## Supported Platforms

- JVM
- Android
- Native
- WebAssembly