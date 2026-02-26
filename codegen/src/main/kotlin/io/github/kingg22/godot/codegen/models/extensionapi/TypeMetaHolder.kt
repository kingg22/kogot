package io.github.kingg22.godot.codegen.models.extensionapi

interface TypeMetaHolder {
    val type: String
    val meta: String?

    fun isRequired(): Boolean = meta == "required"
}
