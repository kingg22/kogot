package io.github.kingg22.kogot.gradle.test

/** How the Kotlin/Native tests are spread over Godot processes. */
enum class GodotTestIsolation {
    /** One fresh headless Godot per test: clean engine state, a crash only fails the test that caused it. */
    PER_TEST,

    /** One Godot for the whole run: fast, but tests share the engine and a crash hides the rest. */
    NONE,
}
