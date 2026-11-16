// Shared stand-in for the user annotations (kotlin-native/api/annotations) the checkers read.
package io.github.kingg22.godot.api.annotations

import io.github.kingg22.godot.api.PropertyHint
import io.github.kingg22.godot.api.PropertyUsageFlags
import io.github.kingg22.godot.api.builtin.Variant

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class RegisterSignal(vararg val params: Param = [], val name: String = "") {
    @Retention(AnnotationRetention.SOURCE)
    @Target()
    annotation class Param(
        val type: Variant.Type,
        val name: String,
        val className: String = "",
        val hint: PropertyHint = PropertyHint.NONE,
        val hintString: String = "",
        val usages: Array<PropertyUsageFlags> = [],
    )
}
