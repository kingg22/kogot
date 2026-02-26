package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
class Singleton(val name: String, val type: String) {
    init {
        check(name == type) {
            "Detected Singleton with different name and type: $name != $type"
        }
    }
}
