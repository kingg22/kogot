package io.github.kingg22.buildlogic.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

@Suppress("unused")
class KotlinJvmCommonConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.plugins) {
            apply("org.jetbrains.kotlin.jvm")
            apply("buildlogic.testing-jvm-conventions")
            apply("buildlogic.jvm-toolchain-conventions")
        }

        val kotlinVersion = target.providers
            .gradleProperty("kotlinVersion")
            .orElse("2.3")

        target.extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                commonConfiguration(kotlinVersion)
                jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
            }
        }

        target.afterEvaluate {
            target.logger.lifecycle("Kotlin target version: ${kotlinVersion.get()}")
        }
    }
}
