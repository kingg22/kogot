import io.github.kingg22.buildlogic.godot.conventions.CodegenBackend
import io.github.kingg22.buildlogic.godot.conventions.CodegenKind
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

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

    sourceSets.nativeTest.dependencies {
        implementation(libs.kotlin.test)
    }

    linuxX64()
    macosArm64()
    mingwX64()
}

// The mingwX64 test executable cannot be linked from a Linux or macOS host (`undefined symbol:
// Kotlin_getSourceInfo_libbacktrace`) and could only run on Windows anyway. The tests run on linuxX64 and macosArm64.
// https://youtrack.jetbrains.com/issue/KT-51866/Compile-error-to-mingwX64-with-libbacktrace
// https://youtrack.jetbrains.com/issue/KT-51886/Meaningful-errors-when-trying-to-use-libbacktrace-on-unsupported-targets
tasks.withType<KotlinNativeLink>().named { it.endsWith("TestMingwX64") }.configureEach {
    enabled = false
}

godotCodegen {
    backend = CodegenBackend.KOTLIN_NATIVE
    kind = CodegenKind.RUNTIME
    this.packageName = "io.github.kingg22.godot"
}
