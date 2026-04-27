package io.github.kingg22.buildlogic.godot.chore

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * DSL extension for configuring Godot codegen globally.
 *
 * This extension provides:
 * - Godot version (e.g., "4.6.1", "4.6.2")
 * - Precision configuration (float_32, float_64, double_32, double_64)
 * - Skip platform-specific APIs flag
 * - Paths to extension_api.json and gdextension_interface.json
 *
 * All values can be configured via:
 * - gradle.properties (godotVersion, godotPrecision)
 * - DSL extension (godotCodegen { ... })
 * - Gradle CLI arguments (-PgodotVersion=4.6.1)
 */
abstract class GodotCodegenChoreExtension {
    /**
     * Godot version to use for code generation.
     * Default: `"4.6.0"`
     */
    abstract val version: Property<String>

    /**
     * Skip platform-specific APIs that are not common across all native targets.
     */
    abstract val skipPlatformSpecificApis: Property<Boolean>

    /**
     * Base directory containing versioned Godot API files.
     * Computed from [version] to `godot-version/vX.Y.Z/`
     */
    abstract val godotVersionDir: DirectoryProperty

    /**
     * Path to extension_api.json for this version. Default is `extension_api.json` inside [godotVersionDir]
     */
    abstract val extensionApiFile: RegularFileProperty

    /**
     * Path to gdextension_interface.json for this version. Default is `gdextension_interface.json` inside [godotVersionDir]
     */
    abstract val extensionInterfaceFile: RegularFileProperty
}