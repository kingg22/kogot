package io.github.kingg22.buildlogic.godot

import io.github.kingg22.buildlogic.godot.task.GenerateGodotTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

private const val CODEGEN_CONFIG = "codegenConfig"

@Suppress("unused")
class GodotCodegenConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val codegenConfig = target.configurations.register(CODEGEN_CONFIG) {
            description = "Internal configuration for the godot-codegen plugin."
            isCanBeConsumed = false
            isCanBeResolved = true
        }

        target.dependencies.add(CODEGEN_CONFIG, target.project(":codegen:cli"))

        val generateGodotTask = target.tasks.register<GenerateGodotTask>("generateGodotExtensionApi") {
            classpath(codegenConfig)

            inputExtension.convention(
                target.rootProject.layout.projectDirectory.file("godot-version/v4_6_2/extension_api.json"),
            )
            inputInterface.convention(
                target.rootProject.layout.projectDirectory.file("godot-version/v4_6_2/gdextension_interface.json"),
            )
            outputDir.convention(target.layout.buildDirectory.dir("generated/sources/godotApi"))
            packageName.convention("io.github.kingg22.godot")
        }

        target.plugins.withId("org.jetbrains.kotlin.jvm") {
            target.extensions.configure<KotlinJvmProjectExtension> {
                generateGodotTask {
                    skipPlatformSpecificApis.convention(
                        target.providers.gradleProperty("codegen.skipPlatformSpecificApis").map { it.toBoolean() },
                    )
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
                        target.providers.gradleProperty("codegen.skipPlatformSpecificApis")
                            .map { it.toBoolean() }
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
