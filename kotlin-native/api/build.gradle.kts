import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension.Backend
import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension.Kind
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.godot.codegen.simple)
}

kotlin {
    compilerOptions {
        explicitApi()
        optIn.addAll(
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.experimental.ExperimentalNativeApi",
            "io.github.kingg22.godot.api.ExperimentalGodotApi",
            "io.github.kingg22.godot.api.ExperimentalGodotKotlin",
        )
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(libs.jetbrains.annotations)
        api(projects.kotlinNative.runtime)
        testImplementation(libs.kotlin.test)
        testImplementation(libs.kotest.assertions.core)
    }

    applyDefaultHierarchyTemplate()

    linuxX64 { configureGodotInterop() }
    // mingwX64 { configureGodotInterop() }
}

fun KotlinNativeTarget.configureGodotInterop() {
    compilations.getByName("main").cinterops.create("godotNativeStructures") {
        packageName = "io.github.kingg22.godot.api.native"
        defFile("nativeInterop/cinterop/extension_api_native.def")
        includeDirs.allHeaders("nativeInterop/cinterop")
    }
}

godotCodegen {
    backend = Backend.KOTLIN_NATIVE
    outputKind = Kind.API
    packageName = "io.github.kingg22.godot"
}
