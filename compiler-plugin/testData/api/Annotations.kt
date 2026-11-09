// Shared stand-in for the user annotations (kotlin-native/api/annotations) the checkers read.
package io.github.kingg22.godot.api.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class RegisterSignal(val name: String = "")
