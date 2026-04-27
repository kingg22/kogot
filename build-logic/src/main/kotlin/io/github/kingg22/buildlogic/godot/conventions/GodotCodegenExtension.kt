package io.github.kingg22.buildlogic.godot.conventions

import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * DSL extension for configuring Godot codegen conventions.
 *
 * This extension provides:
 * - Backend target (kotlin_native, java_ffm)
 * - Output kind (API or RUNTIME)
 * - Filter options for generated types
 * - Package name for generated code
 *
 * Note: The backend and output kind values must match the expected string representations.
 */
abstract class GodotCodegenExtension {
    /**
     * Target backend for code generation.
     * Values: "KOTLIN_NATIVE", "JAVA_FFM"
     * Default: "KOTLIN_NATIVE"
     */
    abstract val backend: Property<GeneratorBackend>

    /**
     * Output kind - API signatures with implementation bodies, or RUNTIME FFI signatures.
     * Values: "API", "RUNTIME"
     * Default: "API"
     */
    abstract val outputKind: Property<GeneratorKind>

    /**
     * Skip platform-specific APIs not common across all native targets.
     * Default: true
     */
    abstract val skipPlatformSpecificApis: Property<Boolean>

    /**
     * Generate only enums.
     * Default: false
     */
    abstract val onlyEnums: Property<Boolean>

    /**
     * Generate only builtin classes.
     * Default: false
     */
    abstract val onlyBuiltinClasses: Property<Boolean>

    /**
     * Generate only engine classes.
     * Default: false
     */
    abstract val onlyEngineClasses: Property<Boolean>

    /**
     * Comma-separated list of type names to exclude.
     * Default: ""
     */
    abstract val excludeTypes: ListProperty<String>

    /**
     * Base package name for generated code.
     * Default: "io.github.kingg22.godot"
     */
    abstract val packageName: Property<String>
}