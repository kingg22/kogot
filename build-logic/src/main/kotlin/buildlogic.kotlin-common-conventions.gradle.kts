import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm")
    id("buildlogic.testing-jvm-conventions")
    id("buildlogic.jvm-toolchain-conventions")
}

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_3)
        apiVersion.set(KotlinVersion.KOTLIN_2_3)
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}
