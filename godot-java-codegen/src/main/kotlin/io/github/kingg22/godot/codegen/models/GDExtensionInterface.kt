package io.github.kingg22.godot.codegen.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GDExtensionInterface(
    @SerialName("_copyright") val copyright: List<String>,
    @SerialName("format_version") val formatVersion: Int,
    @SerialName($$"$schema") val schema: String,
    val types: List<Types>,
    @SerialName("interface") val interfaces: List<Interface>,
)
