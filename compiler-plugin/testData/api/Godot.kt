// Shared stand-in for the user `@Godot` annotation (kotlin-native/api/annotations). BINARY like the real one: KSP also
// reads it from compiled dependencies.
package io.github.kingg22.godot.api.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Godot
