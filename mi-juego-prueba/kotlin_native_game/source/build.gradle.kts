import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("buildlogic.kotlin-multiplatform-conventions")
    id("buildlogic.kotlin-styles-conventions")
    alias(libs.plugins.ksp)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.kotlinNative.api)
        implementation(projects.kotlinNative.annotations)
    }

    applyDefaultHierarchyTemplate()

    linuxX64()
    // mingwX64()
}

dependencies {
    add("kspCommonMainMetadata", projects.processor)
    add("kspLinuxX64", projects.processor)
}
