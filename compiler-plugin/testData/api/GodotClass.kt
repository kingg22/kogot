// Shared stand-in for the marker the generated engine classes carry (kotlin-native/api/chore, api.internal).
package io.github.kingg22.godot.api.internal

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class GodotClass(val godotName: String)
