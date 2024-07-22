# kotlinx-serialization-php

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
