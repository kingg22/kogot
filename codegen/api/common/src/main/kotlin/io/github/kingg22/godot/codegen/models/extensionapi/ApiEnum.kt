package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnum(
    override val name: String,
    @SerialName("is_bitfield") val isBitfield: Boolean,
    override val values: List<EnumConstant>,
    override val description: String? = null,
) : EnumDescriptor
