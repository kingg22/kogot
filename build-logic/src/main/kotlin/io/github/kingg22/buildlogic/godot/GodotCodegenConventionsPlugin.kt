package io.github.kingg22.buildlogic.godot

import io.github.kingg22.buildlogic.godot.chore.GodotCodegenChoreExtension
import io.github.kingg22.buildlogic.godot.conventions.CodegenBackend
import io.github.kingg22.buildlogic.godot.conventions.CodegenBackend.JAVA_FFM
import io.github.kingg22.buildlogic.godot.conventions.CodegenBackend.KOTLIN_NATIVE
import io.github.kingg22.buildlogic.godot.conventions.CodegenKind
import io.github.kingg22.buildlogic.godot.conventions.CodegenKind.API
import io.github.kingg22.buildlogic.godot.conventions.CodegenKind.CALLABLE
import io.github.kingg22.buildlogic.godot.conventions.CodegenKind.RUNTIME
import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenDsl
import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension
import io.github.kingg22.buildlogic.godot.task.GenerateGodotTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

private const val EXTENSION_NAME = "godotCodegen"
private const val CODEGEN_CONFIG = "codegenConfig"

@Suppress("unused")
class GodotCodegenConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.rootProject.plugins.findPlugin("buildlogic.godot-codegen-chore") ?: error(
            "The Godot Codegen plugin requires the Godot Codegen Chore plugin to be applied first in the root project.",
        )

        val codegenConfig = target.configurations.register(CODEGEN_CONFIG) {
            description = "Internal configuration for the godot-codegen plugin."
            isCanBeConsumed = false
            isCanBeResolved = true
        }

        // Get the chore extension from the root project (single source of truth)
        val choreExtension: GodotCodegenChoreExtension = target.rootProject.extensions.getByType()

        // Create conventions extension
        val conventionsExtension = target.extensions.create<GodotCodegenExtension>(EXTENSION_NAME)

        // Apply defaults for top-level options (used as fallback when combinations empty)
        conventionsExtension.backend.convention(KOTLIN_NATIVE)
        conventionsExtension.kind.convention(API)
        conventionsExtension.packageName.convention("io.github.kingg22.godot")
        conventionsExtension.skipPlatformSpecificApis.convention(choreExtension.skipPlatformSpecificApis)
        conventionsExtension.excludeTypes.convention(emptyList())

        target.tasks.register("generateGodot") {
            group = "codegen"
            description = "Generates Godot bindings for the current project."
        }

        target.afterEvaluate {
            val combinations = conventionsExtension.combinations.toList() + conventionsExtension
            createTasksForCombinations(target, conventionsExtension, choreExtension, combinations, codegenConfig)
        }
    }

    private fun createTasksForCombinations(
        target: Project,
        conventionsExtension: GodotCodegenExtension,
        choreExtension: GodotCodegenChoreExtension,
        combinations: List<GodotCodegenDsl>,
        codegenConfig: NamedDomainObjectProvider<Configuration>,
    ) {
        val publishGenerated = target.providers.gradleProperty("publishGenerated")

        val combinationTasks = combinations.map { combination ->
            val backend = combination.backend.get()
            val kind = combination.kind.get()
            val taskName = "generateGodot" +
                backend.name
                    .lowercase()
                    .replaceFirstChar(Char::uppercase)
                    .replace(Regex("_(\\w)")) { matchResult ->
                        matchResult.groupValues[1].uppercase()
                    } + kind.name.lowercase().replaceFirstChar(Char::uppercase)

            resolveDependency(target, codegenConfig, backend, kind)

            val task = target.tasks.register<GenerateGodotTask>(taskName) {
                classpath.from(codegenConfig)
                backendName.set(backend)
                backendName.disallowChanges()
                outputKindName.set(kind)
                outputKindName.disallowChanges()
                inputExtension.set(choreExtension.extensionApiFile)
                inputInterface.set(choreExtension.extensionInterfaceFile)

                val outputBaseDir = if (publishGenerated.isPresent) {
                    target.provider { target.layout.projectDirectory.dir("src/generated/kotlin") }
                } else {
                    target.layout.buildDirectory.dir("generated/sources/godot")
                }

                outputDir.set(
                    outputBaseDir.map { baseDir ->
                        baseDir
                            .dir(backend.name.lowercase())
                            .dir(kind.name.lowercase())
                    },
                )

                packageName.set(combination.packageName)

                filterOnlyEnums.set(combination.onlyEnums)
                filterOnlyBuiltinClasses.set(combination.onlyBuiltinClasses)
                filterOnlyEngineClasses.set(combination.onlyEngineClasses)
                filterOnlyNativeStruct.set(combination.onlyNativeStructures)
                filterExcludeTypes.set(combination.excludeTypes)
                skipPlatformSpecificApis.set(combination.skipPlatformSpecificApis)
            }

            configureSourceSets(target, task, conventionsExtension)

            task
        }

        target.tasks.named("generateGodot") {
            dependsOn(combinationTasks)
        }
    }

    private fun resolveDependency(
        target: Project,
        codegenConfig: NamedDomainObjectProvider<Configuration>,
        backend: CodegenBackend,
        kind: CodegenKind,
    ) {
        val dependencyProject = when (backend to kind) {
            KOTLIN_NATIVE to API -> target.project(":codegen:api:kotlin-native")
            KOTLIN_NATIVE to RUNTIME -> target.project(":codegen:runtime:kotlin-native")
            KOTLIN_NATIVE to CALLABLE -> target.project(":codegen:api:kotlin-native-callable")
            JAVA_FFM to API -> target.project(":codegen:api:java-ffm")
            else -> error("Unsupported backend/output kind combination: $backend to $kind")
        }

        target.dependencies {
            codegenConfig(dependencyProject)
        }
    }

    private fun configureSourceSets(
        target: Project,
        generateGodotTask: TaskProvider<GenerateGodotTask>,
        conventionsExtension: GodotCodegenExtension,
    ) {
        target.plugins.withId("org.jetbrains.kotlin.jvm") {
            target.extensions.configure<KotlinJvmProjectExtension> {
                generateGodotTask {
                    skipPlatformSpecificApis.convention(conventionsExtension.skipPlatformSpecificApis)
                }
                sourceSets {
                    named(SourceSet.MAIN_SOURCE_SET_NAME) {
                        @OptIn(ExperimentalKotlinGradlePluginApi::class)
                        generatedKotlin.srcDir(generateGodotTask.map { it.outputDir })
                    }
                }
            }
        }

        target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            target.extensions.configure<KotlinMultiplatformExtension> {
                val isMultiTargetNativeProvider = target.provider {
                    targets.count { it.platformType == KotlinPlatformType.native } > 1
                }
                generateGodotTask {
                    skipPlatformSpecificApis.convention(
                        conventionsExtension.skipPlatformSpecificApis
                            .orElse(isMultiTargetNativeProvider),
                    )
                }
                sourceSets {
                    nativeMain {
                        @OptIn(ExperimentalKotlinGradlePluginApi::class)
                        generatedKotlin.srcDir(generateGodotTask.map { it.outputDir })
                    }
                }
            }
        }
    }
}
