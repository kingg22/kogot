package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnum(
    val name: String,
    @SerialName("is_bitfield") val isBitfield: Boolean,
    val values: List<EnumConstant>,
)
