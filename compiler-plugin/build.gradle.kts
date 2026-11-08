import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.library.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
}

dependencies {
    // Provided at runtime by whichever compiler loads the plugin (kotlinc, Gradle's embeddable compiler, the IDE).
    compileOnly(libs.kotlin.compiler.plugin.api)
}

// The plugin runs inside the compiler's JVM, which can be older than the project toolchain
// (e.g. the IDE's JBR 21 when loaded through Kotlin External FIR Support). Keep the bytecode on 17.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}
