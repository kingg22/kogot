import io.github.kingg22.buildlogic.godot.conventions.CodegenBackend
import io.github.kingg22.buildlogic.godot.conventions.CodegenKind
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.godot.codegen.simple)
    alias(libs.plugins.dokka.conventions)
}

kotlin {
    compilerOptions {
        explicitApi()
        optIn.addAll(
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.experimental.ExperimentalNativeApi",
            "io.github.kingg22.godot.api.ExperimentalGodotApi",
            "io.github.kingg22.godot.api.ExperimentalGodotKotlin",
            "io.github.kingg22.godot.api.ExperimentalForInheritanceGodotApi",
            "io.github.kingg22.godot.api.InternalForInheritanceGodotApi",
        )
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.kotlinNative.runtime)
        api(projects.kotlinNative.api.chore)
        api(projects.kotlinNative.api.callable)
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
    backend = CodegenBackend.KOTLIN_NATIVE
    kind = CodegenKind.API
    packageName = "io.github.kingg22.godot"
}
