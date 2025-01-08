import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmTargetDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
    alias(libs.plugins.powerAssert)
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.mavenPublish)
}

group = "com.jsoizo"
version = "0.2.0"
val projectName = "kotlinx-serialization-php"

kotlin {
    jvm()

    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    macosX64()

    linuxX64()
    linuxArm64()

    mingwX64()

    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    wasmWasi {
        nodejs()
        binaries.executable()
    }

    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val wasmCommonMain by creating {
            dependsOn(commonMain)
        }
        targets.withType<KotlinNativeTarget>().forEach { target ->
            target.compilations.getByName("main").defaultSourceSet {
                dependsOn(nativeMain)
            }
        }
        targets.withType<KotlinWasmTargetDsl>().forEach { target ->
            target.compilations.getByName("main").defaultSourceSet {
                dependsOn(wasmCommonMain)
            }
        }
    }
}

android {
    namespace = "com.jsoizo.serialization.php"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertEquals", "kotlin.test.assertNull")
    includedSourceSets = listOf("commonMain")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(group.toString(), projectName, version.toString())

    val repo = "github.com/jsoizo/$projectName"
    val repoHttpUrl = "https://$repo"
    val repoGitUrl = "git://$repo"

    pom {
        name = projectName
        description = "PHP support for kotlinx.serialization"
        inceptionYear = "2024"
        url = repoHttpUrl
        licenses {
            license {
                name.set("MIT")
                url.set("$repoHttpUrl/blob/master/LICENSE")
            }
        }
        scm {
            url.set(repoHttpUrl)
            connection.set("scm:git:$repoGitUrl.git")
            developerConnection.set(repoHttpUrl)
        }
        developers {
            developer {
                name.set("jsoizo")
            }
        }
    }
}
