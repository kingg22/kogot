package io.github.kingg22.buildlogic.godot

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

// FIXME this is a hack, must be fixed with appropriated plugin, dsl and more
@Suppress("unused")
class GodotExportConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.withId("java") {
            val copyRuntimeDepsToLibTask = target.tasks.register<Copy>("copyRuntimeDepsToLib") {
                group = "build"
                from(target.configurations.getByName("runtimeClasspath"))
                into(target.layout.buildDirectory.dir("lib"))
            }

            val copyRuntimeJarToLibTask = target.tasks.register<Copy>("copyRuntimeJarToLib") {
                group = "build"
                dependsOn(target.tasks.named("jar"))
                from(target.tasks.named("jar"))
                into(target.layout.buildDirectory.dir("lib"))
            }

            target.tasks.named("assemble").configure {
                dependsOn(copyRuntimeJarToLibTask, copyRuntimeDepsToLibTask)
            }
        }
    }
}
