import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.ksp)
    // kogot K2 compiler plugin (proof of concept): validates @MustBeVariant contracts at call sites.
    id("io.github.kingg22.kogot.compiler")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.kotlinNative.api)
        implementation(projects.kotlinNative.binding)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
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

// The kogot compiler Gradle plugin requests the published io.github.kingg22.kogot:kogot-compiler-plugin
// artifact; inside this repo, build it from source instead.
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("io.github.kingg22.kogot:kogot-compiler-plugin")).using(project(":compiler-plugin"))
    }
}
