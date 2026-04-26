import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind
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
    backend = GeneratorBackend.KOTLIN_NATIVE
    outputKind = GeneratorKind.API
    packageName = "io.github.kingg22.godot"
}
