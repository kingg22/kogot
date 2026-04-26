package io.github.kingg22.buildlogic.godot.chore

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

private const val EXTENSION_NAME = "godotCodegenChore"
private const val PROP_GODOT_VERSION = "godotVersion"
private const val PROP_SKIP_PLATFORM_SPECIFIC_APIS = "codegen.skipPlatformSpecificApis"

// Sensible defaults
private const val DEFAULT_VERSION = "4.6.0"

/**
 * Godot Codegen Chore Plugin
 *
 * This plugin is the SINGLE SOURCE OF TRUTH for Godot version and precision configuration.
 * It provides:
 * - Global Godot version via gradle.properties or DSL
 * - Paths to extension_api.json and gdextension_interface.json
 *
 * Other plugins (like GodotCodegenConventionsPlugin) MUST query this plugin
 * for configuration and MUST NOT hardcode or override these values.
 *
 * Configuration order (later overrides earlier):
 * 1. Default values (version=[DEFAULT_VERSION])
 * 2. gradle.properties (godotVersion, skipPlatformSpecificApis)
 * 3. DSL extension (godotCodegen { version = "4.6.1" })
 * 4. Gradle CLI args (-PgodotVersion=4.6.1) - via project property
 */
@Suppress("unused")
class GodotCodegenChorePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        if (target != target.rootProject) {
            error("This plugin must be applied to the root project.")
        }

        val extension: GodotCodegenChoreExtension = target.extensions.create(EXTENSION_NAME)

        // Default values
        extension.extensionApiFile.convention(extension.godotVersionDir.file("extension_api.json"))
        extension.extensionInterfaceFile.convention(extension.godotVersionDir.file("gdextension_interface.json"))

        // Configure from gradle.properties first
        val godotVersionFromProps = target.providers.gradleProperty(PROP_GODOT_VERSION)
            .orElse(DEFAULT_VERSION)

        // Apply gradle.properties values (but allow DSL to override)
        extension.version.convention(godotVersionFromProps)

        // Compute godotVersionDir based on version
        extension.godotVersionDir.convention(
            extension.version.map { version ->
                target.rootProject.layout.projectDirectory.dir("godot-version/v${version.replace(".", "_")}")
            },
        )

        // Allow CLI override of skipPlatformSpecificApis
        val skipPlatformApisFromProps = target.providers.gradleProperty(PROP_SKIP_PLATFORM_SPECIFIC_APIS)
            .map { it.toBoolean() }

        // Allow DSL override of skipPlatformSpecificApis
        extension.skipPlatformSpecificApis.convention(skipPlatformApisFromProps)

        // Validate paths exist when the project is evaluated
        target.afterEvaluate {
            val version = extension.version.get()
            val versionDir = extension.godotVersionDir.get().asFile

            if (!versionDir.exists() || !versionDir.isDirectory) {
                throw IllegalStateException(
                    "Godot version directory does not exist or is not a directory: ${versionDir.absolutePath}. " +
                        "Please ensure godotVersion is set correctly in gradle.properties or the version directory exists.",
                )
            }

            val extensionApi = extension.extensionApiFile.get().asFile
            val extensionInterface = extension.extensionInterfaceFile.get().asFile

            if (!extensionApi.exists() || !extensionApi.isFile) {
                throw IllegalStateException(
                    "extension_api.json not found or is not a file at ${extensionApi.absolutePath}",
                )
            }

            if (!extensionInterface.exists() || !extensionInterface.isFile) {
                throw IllegalStateException(
                    "gdextension_interface.json not found or is not a file at ${extensionInterface.absolutePath}",
                )
            }

            logger.lifecycle("Godot version: $version, version dir: $versionDir")
        }
    }
}