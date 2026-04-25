package io.github.kingg22.buildlogic.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class KotlinMultiplatformConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.kotlin.multiplatform")

        val kotlinVersion = target.providers
            .gradleProperty("kotlinVersion")
            .orElse("2.3")

        target.extensions.configure<KotlinMultiplatformExtension> {
            commonConfiguration(kotlinVersion)
        }

        target.afterEvaluate {
            target.logger.lifecycle("Kotlin target version: ${kotlinVersion.get()}")
        }
    }
}
