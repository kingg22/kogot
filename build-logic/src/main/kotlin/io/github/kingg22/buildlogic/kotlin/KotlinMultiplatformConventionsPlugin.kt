package io.github.kingg22.buildlogic.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.BaseKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

@Suppress("unused")
class KotlinMultiplatformConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.kotlin.multiplatform")

        val kotlinVersion = target.providers
            .gradleProperty("kotlinVersion")
            .orElse("2.3")

        val useNewInteroperabilityMode = target.providers
            .gradleProperty("useNewInteroperabilityMode")
            .orElse("false")
            .map { it.toBoolean() }

        target.extensions.configure<KotlinMultiplatformExtension> {
            commonConfiguration(kotlinVersion)

            if (useNewInteroperabilityMode.isPresent && useNewInteroperabilityMode.get()) {
                targets.withType<KotlinNativeTarget>().configureEach {
                    compilations.configureEach {
                        cinterops.configureEach {
                            extraOpts += listOf("-Xccall-mode", "direct")
                        }
                    }
                }
            }
        }

        target.tasks.register("compileKotlin") {
            group = "build"
            description = "Compiles the Kotlin source code in all targets."
            dependsOn(target.tasks.withType<BaseKotlinCompile>(), target.tasks.withType<KotlinCompilationTask<*>>())
        }

        target.afterEvaluate {
            target.logger.lifecycle("Kotlin target version: ${kotlinVersion.get()}")
        }
    }
}
