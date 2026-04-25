package io.github.kingg22.buildlogic.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class KotlinApplicationConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("buildlogic.kotlin-jvm-common-conventions")
        target.plugins.apply("application")
    }
}
