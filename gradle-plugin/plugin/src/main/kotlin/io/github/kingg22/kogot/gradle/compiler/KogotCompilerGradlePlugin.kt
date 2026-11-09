package io.github.kingg22.kogot.gradle.compiler

import io.github.kingg22.kogot.gradle.KogotConventions
import io.github.kingg22.kogot.gradle.KogotExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import javax.inject.Inject

/** DSL of the kogot compiler plugin: `kogotCompiler { enabled.set(false) }`. */
abstract class KogotCompilerExtension @Inject constructor(project: Project) {
    /** Whether the kogot K2 compiler plugin runs on this project's Kotlin compilations. */
    val enabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(
        project.providers.gradleProperty(KogotConventions.PROP_COMPILER_PLUGIN_ENABLED)
            .map(String::toBoolean)
            .orElse(true),
    )
}

/**
 * Wires the kogot K2 compiler plugin (`kogot-compiler-plugin`) into every Kotlin compilation.
 *
 * A single artifact serves the JVM/metadata and Kotlin/Native compilers alike: it is built only against
 * `kotlin-compiler-plugin-api`, so it needs no relocated "embeddable" variant.
 */
abstract class KogotCompilerGradlePlugin : KotlinCompilerPluginSupportPlugin {
    private lateinit var project: Project
    private lateinit var extension: KogotCompilerExtension

    override fun apply(target: Project) {
        project = target
        extension = target.extensions.create(
            KogotConventions.COMPILER_EXTENSION_NAME,
            KogotCompilerExtension::class.java,
            target,
        )
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = extension.enabled.get()

    override fun getCompilerPluginId(): String = KogotConventions.COMPILER_PLUGIN_ID

    // The kogot DSL (`kogot { kogotVersion = ... }`) when the kogot plugin is applied, else the same Gradle properties
    // and defaults it falls back to. Read lazily: Kotlin asks for the artifact after the build script ran.
    override fun getPluginArtifact(): SubpluginArtifact {
        val kogot = project.extensions.findByType(KogotExtension::class.java)
        return SubpluginArtifact(
            groupId = kogot?.kogotGroup?.orNull
                ?: gradleProperty(KogotConventions.PROP_KOGOT_GROUP, KogotConventions.DEFAULT_KOGOT_GROUP),
            artifactId = KogotConventions.ARTIFACT_COMPILER_PLUGIN,
            version = kogot?.kogotVersion?.orNull
                ?: gradleProperty(KogotConventions.PROP_KOGOT_VERSION, KogotConventions.DEFAULT_KOGOT_VERSION),
        )
    }

    // The plugin takes no options yet.
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }

    private fun gradleProperty(name: String, default: String): String =
        project.providers.gradleProperty(name).getOrElse(default)
}
