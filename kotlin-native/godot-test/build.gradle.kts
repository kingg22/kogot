import io.github.kingg22.buildlogic.godot.test.GodotTestTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlin.multiplatform.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.experimental.ExperimentalNativeApi",
            "io.github.kingg22.godot.api.ExperimentalGodotApi",
            "io.github.kingg22.godot.api.ExperimentalGodotKotlin",
        )
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(projects.kotlinNative.api)
        implementation(projects.kotlinNative.binding)
    }

    applyDefaultHierarchyTemplate()

    macosArm64 { configureHarness() }
    linuxX64 { configureHarness() }
}

dependencies {
    add("kspMacosArm64Test", projects.processor)
    add("kspLinuxX64Test", projects.processor)
}

/**
 * The tests are compiled as a shared library (a GDExtension), not as the usual test executable: Godot loads it, and the
 * Kotlin/Native test runner is started from the extension entry point (see `HarnessEntryPoint.kt`).
 */
fun KotlinNativeTarget.configureHarness() {
    val test = compilations.getByName("test")
    // KSP does not feed the generated sources of a native *test* compilation back into it, so add them by hand.
    test.defaultSourceSet.kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/$targetName/${targetName}Test/kotlin"))
    // The entry point refers to the processor's `GeneratedBindings`, so it lives in the same (per-target) fragment.
    test.defaultSourceSet.kotlin.srcDir("src/harnessEntry/kotlin")
    test.compileTaskProvider.configure {
        compilerOptions.freeCompilerArgs.add("-generate-test-runner")
    }
    binaries {
        sharedLib("godotTests", listOf(NativeBuildType.DEBUG)) {
            compilation = test
            baseName = "kogot_godot_tests"
        }
    }
}

// region godotTest
val osName = System.getProperty("os.name").lowercase()
val hostTarget = when {
    "mac" in osName -> "MacosArm64"
    "linux" in osName -> "LinuxX64"
    else -> null // mingwX64 is not wired up yet
}

if (hostTarget != null) {
    val hostDir = layout.projectDirectory.dir("host")
    val link = tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink>("linkGodotTestsDebugShared$hostTarget")

    tasks.register<GodotTestTask>("godotTest") {
        group = "verification"
        description = "Runs the Kotlin/Native tests inside a headless Godot (see docs/godot-native-test-harness.md)"
        dependsOn(link)
        library.set(layout.file(link.flatMap { it.outputFile }))
        hostProject.set(hostDir)
        reportsDir.set(layout.buildDirectory.dir("test-results/godotTest"))
        godotBin.set(providers.gradleProperty("godot.bin").orElse(providers.environmentVariable("GODOT_BIN")))
        godotVersion.set(providers.gradleProperty("godot.version"))
        timeoutSeconds.set(providers.gradleProperty("godot.timeout").map(String::toLong).orElse(120L))
        // Always run: the result depends on the engine binary, which Gradle does not track.
        outputs.upToDateWhen { false }
    }
    tasks.named("check") { dependsOn("godotTest") }
}
// endregion
