import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    // Apply the org.jetbrains.kotlin.multiplaform Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.multiplatform")
}

val kotlinVersion: Provider<String> = providers
    .gradleProperty("kotlinVersion")
    .orElse("2.3")

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.fromVersion(kotlinVersion.get()))
        apiVersion.set(languageVersion)
        optIn.add("kotlin.contracts.ExperimentalContracts")
        freeCompilerArgs.addAll("-Xcontext-sensitive-resolution", "-Xreturn-value-checker=full")
        allWarningsAsErrors.set(true)
        extraWarnings.set(true)
    }
}

afterEvaluate {
    logger.lifecycle("Kotlin target version: ${kotlinVersion.get()}")
}
