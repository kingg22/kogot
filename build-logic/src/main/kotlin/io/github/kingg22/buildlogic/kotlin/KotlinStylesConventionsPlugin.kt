package io.github.kingg22.buildlogic.kotlin

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@Suppress("unused")
class KotlinStylesConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("buildlogic.common-styles-conventions")
        target.extensions.configure<SpotlessExtension> {
            kotlin {
                // https://github.com/pinterest/ktlint/releases
                ktlint("1.8.0")
            }
        }
    }
}
