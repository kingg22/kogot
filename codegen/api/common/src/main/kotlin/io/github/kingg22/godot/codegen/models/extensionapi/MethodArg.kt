package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MethodArg(
    override val name: String,
    override val type: String,
    override val meta: String? = null,
    @SerialName("default_value") val defaultValue: String? = null,
) : TypeMetaHolder,
    Named {
    val isNullable: Boolean get() = type != "Variant" && defaultValue == "null"
}
