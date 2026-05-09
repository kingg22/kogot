import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.kotlinNative.api.annotations)
        api(projects.kotlinNative.api.generated)
        api(projects.kotlinNative.api.utils)
        api(projects.kotlinNative.api.signal)
    }

    applyDefaultHierarchyTemplate()

    linuxX64()
    // mingwX64()
}
