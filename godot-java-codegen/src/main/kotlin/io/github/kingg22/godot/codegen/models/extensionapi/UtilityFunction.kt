package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UtilityFunction(
    val name: String,
    @SerialName("return_type") val returnType: String? = null,
    val category: String,
    @SerialName("is_vararg") val isVararg: Boolean,
    val hash: Long,
    val arguments: List<MethodArg>? = null,
)
