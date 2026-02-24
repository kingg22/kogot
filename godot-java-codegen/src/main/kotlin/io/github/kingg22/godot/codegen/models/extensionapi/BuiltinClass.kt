package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuiltinClass(
    val name: String,
    @SerialName("indexing_return_type") val indexingReturnType: String? = null,
    @SerialName("is_keyed") val isKeyed: Boolean = false,
    val members: List<BuiltinClassMember>? = null,
    val constants: List<BuiltinClassConstant>? = null,
    val enums: List<BuiltinEnum>? = null,
    val operators: List<Operator>,
    val methods: List<BuiltinMethod>? = null,
    val constructors: List<Constructor>,
    @SerialName("has_destructor") val hasDestructor: Boolean,
) {
    @Serializable
    data class BuiltinMethod(
        val name: String,
        @SerialName("return_type") val returnType: String? = null,
        @SerialName("is_vararg") val isVararg: Boolean,
        @SerialName("is_const") val isConst: Boolean,
        @SerialName("is_static") val isStatic: Boolean,
        val hash: Long? = null,
        @SerialName("hash_compatibility") val hashCompatibility: List<Long>? = null,
        val arguments: List<MethodArg>? = null,
    )

    @Serializable
    data class BuiltinClassMember(val name: String, val type: String)

    @Serializable
    data class BuiltinClassConstant(val name: String, val type: String, val value: String)
}
