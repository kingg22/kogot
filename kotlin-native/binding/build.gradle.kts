import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension.Backend
import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension.Kind
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.godot.codegen.simple)
}

kotlin {
    explicitApi()

    compilerOptions {
        optIn.addAll(
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.experimental.ExperimentalNativeApi",
        )
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(libs.jetbrains.annotations)
        api(projects.kotlinNative.ffi)
        implementation(projects.kotlinNative.api)
    }

    applyDefaultHierarchyTemplate()

    // linux
    linuxX64()
    // mingwX64()
}

godotCodegen {
    outputKind = Kind.CALLABLE
    backend = Backend.KOTLIN_NATIVE
    packageName = "io.github.kingg22.godot.internal.callback"
}
