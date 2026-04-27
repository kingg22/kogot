package io.github.kingg22.buildlogic.godot.task

import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters

interface CodegenRunnerWorkParameters : WorkParameters {
    val classpath: ConfigurableFileCollection
    val inputInterface: RegularFileProperty
    val inputExtension: RegularFileProperty
    val outputDir: DirectoryProperty
    val packageName: Property<String>
    val backendName: Property<GodotCodegenExtension.Backend>
    val kindName: Property<GodotCodegenExtension.Kind>
    val generateDocs: Property<Boolean>
    val skipPlatformSpecificApis: Property<Boolean>
    val filterOnlyEnums: Property<Boolean>
    val filterOnlyBuiltinClasses: Property<Boolean>
    val filterOnlyEngineClasses: Property<Boolean>
    val filterExcludeTypes: ListProperty<String>
}
