import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind
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
    }

    applyDefaultHierarchyTemplate()

    // linux
    linuxX64()
    // mingwX64()
}

godotCodegen {
    backend = GeneratorBackend.KOTLIN_NATIVE
    outputKind = GeneratorKind.RUNTIME
    packageName = "io.github.kingg22.godot"
}
