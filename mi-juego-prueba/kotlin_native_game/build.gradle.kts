import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.ksp)
    // kogot K2 compiler plugin (proof of concept): validates @MustBeVariant contracts at call sites.
    alias(libs.plugins.kogot.compiler)
    // Runs the nativeTest sources (`macosArm64Test`, `linuxX64Test`) inside a headless Godot, also from the IDE.
    alias(libs.plugins.kogot.test)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.kotlinNative.api)
        implementation(projects.kotlinNative.binding)
    }

    sourceSets {
        commonTest {
            // The tests use the Godot API like the game code does.
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
                optIn("io.github.kingg22.godot.api.ExperimentalGodotApi")
                optIn("io.github.kingg22.godot.api.ExperimentalGodotKotlin")
            }
            dependencies {
                implementation(libs.kotlin.test)
                // Only the coroutine and threading tests use them.
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }

    applyDefaultHierarchyTemplate()

    linuxX64()
    macosArm64()
    mingwX64()
}

dependencies {
    add("kspCommonMainMetadata", projects.processor)
    add("kspLinuxX64", projects.processor)
    add("kspMacosArm64", projects.processor)
    add("kspMingwX64", projects.processor)
    // put KSP on nativeMain source set when have more than one target
}

// The kogot Gradle plugins request the published io.github.kingg22.kogot:kogot-compiler-plugin and kogot-processor
// artifacts; inside this repo, build them from source instead.
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("io.github.kingg22.kogot:kogot-compiler-plugin")).using(project(":compiler-plugin"))
        substitute(module("io.github.kingg22.kogot:kogot-processor")).using(project(":processor"))
    }
}
