package com.jsoizo.serialization.php.testdata

import kotlinx.serialization.Serializable

@Serializable
sealed interface TestSealed {
    @Serializable
    class SealedA(val a: Int) : TestSealed
}
