package io.github.kingg22.buildlogic.jvm

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

@Suppress("unused")
class JvmToolchainConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Puedes cambiar esto o leerlo desde gradle.properties
        val toolchainVersion = target.providers
            .gradleProperty("jvmToolchainVersion")
            .map(String::toInt)
            .orElse(24)

        val javaTargetVersion = target.providers
            .gradleProperty("javaTargetVersion")
            .map(String::toInt)

        fun configureJavaExtension() {
            target.extensions.configure<JavaPluginExtension> {
                if (javaTargetVersion.isPresent) {
                    val javaTarget = JavaVersion.toVersion(javaTargetVersion.get())
                    sourceCompatibility = javaTarget
                    targetCompatibility = javaTarget
                } else {
                    toolchain {
                        languageVersion.set(toolchainVersion.map(JavaLanguageVersion::of))
                    }
                }
            }
        }

        // ---------- JAVA ----------
        target.plugins.withId("java") {
            configureJavaExtension()
        }

        target.plugins.withId("java-library") {
            configureJavaExtension()
        }

        // ---------- KOTLIN JVM ----------
        target.plugins.withId("org.jetbrains.kotlin.jvm") {
            target.extensions.configure<KotlinJvmProjectExtension> {
                if (javaTargetVersion.isPresent) {
                    compilerOptions {
                        jvmTarget.set(javaTargetVersion.map(Int::toString).map(JvmTarget::fromTarget))
                    }
                } else {
                    jvmToolchain {
                        languageVersion.set(toolchainVersion.map(JavaLanguageVersion::of))
                    }
                }
            }
        }

        target.afterEvaluate {
            logger.lifecycle("JVM versions to use: ${toolchainVersion.get()}, with target: ${javaTargetVersion.orNull}")
        }
    }
}
