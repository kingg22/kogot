package io.github.kingg22.buildlogic.godot.conventions

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

interface GodotCodegenDsl {
    val backend: Property<CodegenBackend>
    val kind: Property<CodegenKind>
    val packageName: Property<String>
    val onlyEnums: Property<Boolean>
    val onlyBuiltinClasses: Property<Boolean>
    val onlyEngineClasses: Property<Boolean>
    val onlyNativeStructures: Property<Boolean>
    val excludeTypes: ListProperty<String>
    val skipPlatformSpecificApis: Property<Boolean>
}
