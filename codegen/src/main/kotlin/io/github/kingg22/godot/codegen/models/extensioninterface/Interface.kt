package io.github.kingg22.godot.codegen.models.extensioninterface

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Interface(
    val name: String,
    val arguments: List<Arguments>,
    val description: List<String>,
    val since: String,
    @SerialName("return_value") val returnValue: ValueType? = null,
    val deprecated: Deprecated? = null,
    val see: List<String> = emptyList(),
    @SerialName("legacy_type_name") val legacyTypeName: String? = null,
)
