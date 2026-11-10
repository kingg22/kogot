import io.github.kingg22.buildlogic.godot.conventions.CodegenBackend
import io.github.kingg22.buildlogic.godot.conventions.CodegenKind
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.godot.codegen.simple)
    alias(libs.plugins.dokka.conventions)
}

kotlin {
    explicitApi()

    /*
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        keepLocallyUnsupportedTargets.set(true)
    }
     */

    compilerOptions {
        optIn.addAll(
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.experimental.ExperimentalNativeApi",
            "io.github.kingg22.godot.api.ExperimentalGodotApi",
            "io.github.kingg22.godot.api.ExperimentalGodotKotlin",
            "io.github.kingg22.godot.api.ExperimentalForInheritanceGodotApi",
            "io.github.kingg22.godot.api.InternalForInheritanceGodotApi",
            // The generated wrappers are part of the binding's own implementation: the ones around a
            // `Ref<T>` return call `adoptReturnedReference`. The marker is there to keep users away.
            "io.github.kingg22.godot.internal.binding.InternalBinding",
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
    macosArm64 { configureGodotInterop() }
    mingwX64 { configureGodotInterop() }
}

fun KotlinNativeTarget.configureGodotInterop() {
    compilations.named("main").configure {
        cinterops.register("godotNativeStructures") {
            this.packageName = "io.github.kingg22.godot.api.native"
            defFile("nativeInterop/cinterop/extension_api_native.def")
            includeDirs.allHeaders("nativeInterop/cinterop")
        }
    }
}

godotCodegen {
    backend = CodegenBackend.KOTLIN_NATIVE
    kind = CodegenKind.API
    this.packageName = "io.github.kingg22.godot"

    combinations {
        // Typed `Callable { ... }` factories: they need the builtin Callable/Variant, so they live here.
        register("callable") {
            kind = CodegenKind.CALLABLE
        }
        // Typed `emit`/`connect` extensions of TypedSignal (hand-written in this module, api.signal).
        register("signal") {
            kind = CodegenKind.SIGNAL
        }
    }
}
