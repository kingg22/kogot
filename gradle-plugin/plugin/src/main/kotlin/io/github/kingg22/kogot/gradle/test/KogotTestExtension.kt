package io.github.kingg22.kogot.gradle.test

import io.github.kingg22.kogot.gradle.KogotConventions
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

/**
 * `kogotTest { ... }`: runs the module's Kotlin/Native tests (`<target>Test`, so also from the IDE) inside a headless
 * Godot. Every value has a `gradle.properties` fallback.
 */
abstract class KogotTestExtension @Inject constructor(providers: ProviderFactory, objects: ObjectFactory) {

    /**
     * Godot executable, found like the rest of the kogot plugin does (see [io.github.kingg22.kogot.gradle.GodotExecutableLocator]):
     * `kogot.godotExecutable`, then `GODOT_BIN`, then `godot4` / `godot` on `PATH`; on macOS it may be the `.app` bundle.
     */
    val godotExecutable: Property<String> = objects.property(String::class.java).convention(
        providers.gradleProperty(KogotConventions.PROP_GODOT_EXECUTABLE),
    )

    /**
     * Godot version (`X.Y.Z`) the tests must run against, the same `kogot.godotVersion` property of the plugin; the
     * launcher fails when the executable reports another minor. Unset, any version is accepted.
     */
    val godotVersion: Property<String> = objects.property(String::class.java).convention(
        providers.gradleProperty(KogotConventions.PROP_GODOT_VERSION),
    )

    /** Seconds a single Godot process may run before it is killed and its test fails. */
    val timeoutSeconds: Property<Long> = objects.property(Long::class.java).convention(
        providers.gradleProperty("kogot.test.timeout").map(String::toLong).orElse(120L),
    )

    val isolation: Property<GodotTestIsolation> = objects.property(GodotTestIsolation::class.java).convention(
        providers.gradleProperty("kogot.test.isolation").map { GodotTestIsolation.valueOf(it.uppercase()) }
            .orElse(GodotTestIsolation.PER_TEST),
    )

    /**
     * The published kogot processor, added to the test KSP configurations (same `kogot.group` / `kogot.version`
     * properties as the main plugin). A build that has the processor as a project substitutes this module.
     */
    internal val processorCoordinate = providers.gradleProperty("kogot.group").orElse("io.github.kingg22.kogot").zip(
        providers.gradleProperty("kogot.version").orElse("0.1.0"),
    ) { group, version -> "$group:kogot-processor:$version" }
}
