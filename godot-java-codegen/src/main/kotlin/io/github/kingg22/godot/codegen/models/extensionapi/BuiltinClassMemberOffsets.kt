package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuiltinClassMemberOffsets(
    @SerialName("build_configuration") val buildConfiguration: String,
    val classes: List<Classes>,
)
