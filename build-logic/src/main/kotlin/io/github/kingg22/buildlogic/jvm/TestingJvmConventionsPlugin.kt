package io.github.kingg22.buildlogic.jvm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.invoke
import org.gradle.testing.base.TestingExtension

@Suppress("unused")
class TestingJvmConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("java")

        @Suppress("UnstableApiUsage")
        target.extensions.configure<TestingExtension> {
            suites {
                val test by getting(JvmTestSuite::class) {
                    // Use JUnit Jupiter test framework
                    useJUnitJupiter("5.14.3")
                }
            }
        }
    }
}
