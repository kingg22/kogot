package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MethodArg(
    val name: String,
    @SerialName("type") val type: String,
    val meta: String? = null,
    @SerialName("default_value") val defaultValue: String? = null,
)
