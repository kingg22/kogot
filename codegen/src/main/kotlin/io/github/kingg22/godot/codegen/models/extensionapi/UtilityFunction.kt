package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UtilityFunction(
    override val name: String,
    @SerialName("return_type") val returnType: String? = null,
    val category: String,
    @SerialName("is_vararg") val isVararg: Boolean,
    override val hash: Long,
    override val description: String,
    val arguments: List<MethodArg> = emptyList(),
) : Hashable,
    Named,
    Documentable {
    override val hashCompatibility: List<Long> = emptyList()
}
