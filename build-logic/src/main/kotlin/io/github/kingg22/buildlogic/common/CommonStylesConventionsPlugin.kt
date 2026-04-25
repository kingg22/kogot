package io.github.kingg22.buildlogic.common

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

@Suppress("unused")
class CommonStylesConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("com.diffplug.spotless")

        target.extensions.getByType<SpotlessExtension>().apply {
            encoding = Charsets.UTF_8
            lineEndings = LineEnding.PRESERVE

            kotlinGradle {
                // https://github.com/pinterest/ktlint/releases
                ktlint("1.8.0")
            }
        }

        target.tasks.named("spotlessCheck") {
            dependsOn(target.tasks.named("spotlessApply"))
        }
    }
}
