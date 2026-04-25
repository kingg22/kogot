package io.github.kingg22.buildlogic.java

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class JavaLibraryConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            apply("java-library")
            apply("buildlogic.common-styles-conventions")
            apply("buildlogic.jvm-toolchain-conventions")
            apply("buildlogic.testing-jvm-conventions")
        }
    }
}
