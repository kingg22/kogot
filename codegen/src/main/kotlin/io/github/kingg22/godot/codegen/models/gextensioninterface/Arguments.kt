package io.github.kingg22.godot.codegen.models.gextensioninterface

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Arguments(
    @SerialName("type") val type: String,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: List<String> = emptyList(),
)
