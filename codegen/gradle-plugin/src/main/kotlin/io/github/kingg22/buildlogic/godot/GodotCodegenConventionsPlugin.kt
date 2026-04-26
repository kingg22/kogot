package io.github.kingg22.buildlogic.godot

import io.github.kingg22.buildlogic.godot.chore.GodotCodegenChoreExtension
import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension
import io.github.kingg22.buildlogic.godot.task.GenerateGodotTask
import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.internal.classloader.FilteringClassLoader.DEFAULT_PACKAGE
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

private const val EXTENSION_NAME = "godotCodegen"
private val DEFAULT_BACKEND = GeneratorBackend.KOTLIN_NATIVE
private val DEFAULT_OUTPUT_KIND = GeneratorKind.API
private const val DEFAULT_PACKAGE = "io.github.kingg22.godot"

@Suppress("unused")
class GodotCodegenConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.rootProject.plugins.findPlugin("buildlogic.godot-codegen-chore") ?: error(
            "The Godot Codegen plugin requires the Godot Codegen Chore plugin to be applied first in the root project.",
        )

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

        val generateGodotTask = target.tasks.register<GenerateGodotTask>("generateGodotExtensionApi") {
            backendName.set(conventionsExtension.backend)
            inputExtension.set(choreExtension.extensionApiFile)
            inputInterface.set(choreExtension.extensionInterfaceFile)

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
            outputKindName.convention(conventionsExtension.outputKind)

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
