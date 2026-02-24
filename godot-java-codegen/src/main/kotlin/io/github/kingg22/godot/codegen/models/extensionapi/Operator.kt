package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Operator(
    val name: String,
    @SerialName("right_type") val rightType: String? = null,
    @SerialName("return_type") val returnType: String,
)
