package io.github.kingg22.buildlogic.godot

import io.github.kingg22.buildlogic.godot.chore.GodotCodegenChoreExtension
import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension
import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension.Backend
import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension.Kind
import io.github.kingg22.buildlogic.godot.task.GenerateGodotTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
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
private val DEFAULT_BACKEND = Backend.KOTLIN_NATIVE
private val DEFAULT_OUTPUT_KIND = Kind.API
private const val DEFAULT_PACKAGE = "io.github.kingg22.godot"
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

        // Create conventions extension with defaults
        val conventionsExtension: GodotCodegenExtension = target.extensions.create(EXTENSION_NAME)

        // Apply default values from extension companion object
        conventionsExtension.backend.convention(DEFAULT_BACKEND)
        conventionsExtension.outputKind.convention(DEFAULT_OUTPUT_KIND)
        conventionsExtension.packageName.convention(DEFAULT_PACKAGE)
        conventionsExtension.skipPlatformSpecificApis.convention(choreExtension.skipPlatformSpecificApis)
        conventionsExtension.excludeTypes.convention(emptyList())

        target.dependencies {
            codegenConfig(
                target.provider {
                    when (
                        val combination =
                            conventionsExtension.backend.get() to conventionsExtension.outputKind.get()
                    ) {
                        Backend.KOTLIN_NATIVE to Kind.API -> target.project(":codegen:api:kotlin-native")

                        Backend.KOTLIN_NATIVE to Kind.RUNTIME -> target.project(":codegen:runtime:kotlin-native")

                        else -> error(
                            "Unsupported backend/output kind combination: $combination",
                        )
                    }
                },
            )
        }

        val generateGodotTask = target.tasks.register<GenerateGodotTask>("generateGodotExtensionApi") {
            runnerClasspath.setFrom(codegenConfig)
            runnerClasspath.disallowChanges()
            backendName.set(conventionsExtension.backend)
            backendName.disallowChanges()
            outputKindName.convention(conventionsExtension.outputKind)
            outputKindName.disallowChanges()
            inputExtension.set(choreExtension.extensionApiFile)
            inputExtension.disallowChanges()
            inputInterface.set(choreExtension.extensionInterfaceFile)
            inputInterface.disallowChanges()

            outputDir.convention(
                target.layout.buildDirectory.dir(
                    target.provider {
                        "generated/sources/godot/" +
                            conventionsExtension.backend.get().name.lowercase() + "/" +
                            conventionsExtension.outputKind.get().name.lowercase()
                    },
                ),
            )

            packageName.convention(conventionsExtension.packageName)

            filterOnlyEnums.convention(conventionsExtension.onlyEnums)
            filterOnlyBuiltinClasses.convention(conventionsExtension.onlyBuiltinClasses)
            filterOnlyEngineClasses.convention(conventionsExtension.onlyEngineClasses)
            filterExcludeTypes.convention(conventionsExtension.excludeTypes)
        }

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
