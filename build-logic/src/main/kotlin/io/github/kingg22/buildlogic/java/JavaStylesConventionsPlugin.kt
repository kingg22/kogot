package io.github.kingg22.buildlogic.java

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@Suppress("unused")
class JavaStylesConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("buildlogic.common-styles-conventions")
        target.extensions.configure<SpotlessExtension> {
            java {
                removeUnusedImports()
                // https://github.com/palantir/palantir-java-format/releases
                palantirJavaFormat("2.89.0").formatJavadoc(false)
                importOrder("", "java", "javax", "\\#")
                formatAnnotations()
            }
        }
    }
}
