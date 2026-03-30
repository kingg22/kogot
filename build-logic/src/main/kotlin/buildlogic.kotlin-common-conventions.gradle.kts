import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm")
    id("buildlogic.testing-jvm-conventions")
    id("buildlogic.jvm-toolchain-conventions")
}

val kotlinVersion: Provider<String> = providers
    .gradleProperty("kotlinVersion")
    .orElse("2.3")

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.fromVersion(kotlinVersion.get()))
        apiVersion.set(languageVersion)
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
        optIn.add("kotlin.contracts.ExperimentalContracts")
        freeCompilerArgs.addAll("-Xcontext-sensitive-resolution", "-Xreturn-value-checker=full")
        allWarningsAsErrors.set(true)
        extraWarnings.set(true)
    }
}

afterEvaluate {
    logger.lifecycle("Kotlin target version: ${kotlinVersion.get()}")
}
