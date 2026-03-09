import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("buildlogic.kotlin-multiplatform-conventions")
    id("buildlogic.kotlin-styles-conventions")
    id("buildlogic.godot-codegen")
}

kotlin {
    explicitApi()

    compilerOptions {
        optIn.addAll(
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.experimental.ExperimentalNativeApi",
        )
        freeCompilerArgs.addAll("-Xcontext-sensitive-resolution")
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(libs.jetbrains.annotations)
        api(projects.kotlinNativeFfi)
    }

    applyDefaultHierarchyTemplate()

    // linux
    linuxX64()
}

tasks.generateGodotExtensionApi.configure {
    backendName.set("kotlin_native")
    outputKindName.set("runtime")
}
