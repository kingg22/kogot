package io.github.kingg22.kogot.gradle.test

import com.google.devtools.ksp.gradle.KspAATask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import java.io.File

/**
 * `io.github.kingg22.kogot.test`: makes the ordinary `<target>Test` task of a Kotlin/Native module run its tests inside
 * a headless Godot, so tests are plain `kotlin.test` code that may use the Godot API, and the IDE runs, filters and
 * reports them like any other test.
 *
 * Per native target the host OS can run (macosArm64 on macOS, linuxX64 on Linux):
 * 1. the kogot KSP processor runs on the test compilation, so `@Godot` classes declared in tests are registered;
 * 2. a generated harness (runner node + GDExtension entry point) is added to the test compilation, which is also linked
 *    as a shared library;
 * 3. `<target>Test` runs [GodotTestLauncher] instead of the test executable (see [PrepareGodotTestHostTask]).
 */
@Suppress("unused")
abstract class KogotTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("kogotTest", KogotTestExtension::class.java, project)
        project.pluginManager.apply("com.google.devtools.ksp")

        project.plugins.withType(KotlinMultiplatformPluginWrapper::class.java) {
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
                if (canRunOnHost(target.name)) configureTarget(project, extension, target)
            }
        }
    }

    private fun configureTarget(project: Project, extension: KogotTestExtension, target: KotlinNativeTarget) {
        val capitalized = target.name.replaceFirstChar(Char::uppercase)
        val compilation = target.compilations.getByName("test")
        val buildDir = project.layout.buildDirectory

        // 1. KSP on the test compilation, with the aggregator in a package that cannot clash with the main one.
        val kspConfiguration = "ksp${capitalized}Test"
        project.configurations.configureEach { configuration ->
            if (configuration.name == kspConfiguration) {
                project.dependencies.addProvider(kspConfiguration, extension.processorCoordinate)
            }
        }
        project.tasks.withType(KspAATask::class.java).matching {
            it.name == "kspTestKotlin$capitalized"
        }.configureEach {
            it.commandLineArgumentProviders.add(
                CommandLineArgumentProvider {
                    listOf("kogot.generatedBindingsPackage=${GenerateTestHarnessTask.GENERATED_BINDINGS_PACKAGE}")
                },
            )
        }

        // 2. The harness, and the test compilation linked as a shared library. KSP's output for a native test
        // compilation is not picked up by the compilation on its own, and the harness must be in the same
        // (per-target) fragment as the generated `GeneratedBindings` it refers to.
        val harnessDir = buildDir.dir("generated/kogotTest/${target.name}")
        val generateHarness = project.tasks.register(
            "generateKogotTestHarness$capitalized",
            GenerateTestHarnessTask::class.java,
        ) { it.outputDir.set(harnessDir) }
        compilation.defaultSourceSet.kotlin.srcDir(generateHarness.map { it.outputDir })
        compilation.defaultSourceSet.kotlin.srcDir(
            buildDir.dir("generated/ksp/${target.name}/${target.name}Test/kotlin"),
        )
        compilation.compileTaskProvider.configure { it.compilerOptions.freeCompilerArgs.add("-generate-test-runner") }

        target.binaries.sharedLib("kogotGodotTests", listOf(NativeBuildType.DEBUG)) {
            this.compilation = compilation
            baseName = "kogot_godot_tests"
        }
        val link = project.tasks.named("linkKogotGodotTestsDebugShared$capitalized")

        // 3. The host project + launcher, and `<target>Test` running the launcher.
        val prepare = project.tasks.register(
            "prepareKogotGodotTestHost$capitalized",
            PrepareGodotTestHostTask::class.java,
        ) { task ->
            task.dependsOn(link)
            task.library.set(
                project.layout.file(
                    project.provider {
                        (link.get() as org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink).outputFile.get()
                    },
                ),
            )
            task.godotMinVersion.set("4.6")
            task.godotBin.set(extension.godotBin)
            task.godotVersion.set(extension.godotVersion)
            task.timeoutSeconds.set(extension.timeoutSeconds)
            task.isolation.set(extension.isolation)
            task.launcherClasspath.set(launcherClasspath())
            task.javaExecutable.set(File(System.getProperty("java.home"), "bin/java").absolutePath)
            task.hostDir.set(buildDir.dir("kogotGodotTest/${target.name}"))
        }
        project.tasks.withType(KotlinNativeTest::class.java).matching {
            it.name == "${target.name}Test"
        }.configureEach { test ->
            test.dependsOn(prepare)
            test.executableProperty.set(project.files(prepare.map { it.script }).builtBy(prepare))
        }
    }

    /** The plugin's own classes and the Kotlin stdlib they use: all the launcher needs. */
    private fun launcherClasspath(): String = listOf(GodotTestLauncher::class.java, Unit::class.java)
        .map { File(it.protectionDomain.codeSource.location.toURI()).absolutePath }
        .distinct()
        .joinToString(File.pathSeparator)

    private fun canRunOnHost(targetName: String): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return when (targetName) {
            "macosArm64" -> "mac" in os && System.getProperty("os.arch") in setOf("aarch64", "arm64")
            "linuxX64" -> "linux" in os
            else -> false
        }
    }
}
