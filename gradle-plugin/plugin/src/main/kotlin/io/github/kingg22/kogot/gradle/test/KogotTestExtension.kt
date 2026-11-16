package io.github.kingg22.kogot.gradle.test

import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

/** How the Kotlin/Native tests are spread over Godot processes. */
enum class GodotTestIsolation {
    /** One fresh headless Godot per test: clean engine state, a crash only fails the test that caused it. */
    PER_TEST,

    /** One Godot for the whole run: fast, but tests share the engine and a crash hides the rest. */
    NONE,
}

/**
 * `kogotTest { ... }`: runs the module's Kotlin/Native tests (`<target>Test`, so also from the IDE) inside a headless
 * Godot. Every value has a `gradle.properties` fallback.
 */
abstract class KogotTestExtension
@Inject
constructor(project: Project) {
    private val providers = project.providers

    /**
     * Godot executable (or, on macOS, the `.app` bundle). Without it the launcher looks at `GODOT_BIN`, then `godot` /
     * `godot4` on `PATH`, then `~/Downloads` (matching [godotVersion] when set).
     */
    val godotBin: Property<String> = project.objects.property(String::class.java).convention(
        providers.gradleProperty("kogot.test.godotBin").orElse(providers.gradleProperty("godot.bin")),
    )

    /** Godot version (`X.Y.Z`) the tests must run against; the launcher fails when the binary reports another minor. */
    val godotVersion: Property<String> = project.objects.property(String::class.java).convention(
        providers.gradleProperty("kogot.test.godotVersion").orElse(providers.gradleProperty("godot.version")),
    )

    /** Seconds a single Godot process may run before it is killed and its test fails. */
    val timeoutSeconds: Property<Long> = project.objects.property(Long::class.java).convention(
        providers.gradleProperty("kogot.test.timeout").orElse(providers.gradleProperty("godot.timeout"))
            .map(String::toLong).orElse(120L),
    )

    val isolation: Property<GodotTestIsolation> = project.objects.property(GodotTestIsolation::class.java).convention(
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
