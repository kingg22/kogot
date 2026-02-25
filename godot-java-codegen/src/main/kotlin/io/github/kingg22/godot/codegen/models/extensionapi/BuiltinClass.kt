package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BuiltinClass(
    override val name: String,
    @SerialName("indexing_return_type") val indexingReturnType: String? = null,
    @SerialName("is_keyed") val isKeyed: Boolean = false,
    val members: List<BuiltinClassMember> = emptyList(),
    override val constants: List<BuiltinClassConstant> = emptyList(),
    override val enums: List<BuiltinEnum> = emptyList(),
    val operators: List<Operator>,
    override val methods: List<BuiltinMethod> = emptyList(),
    val constructors: List<Constructor>,
    @SerialName("has_destructor") val hasDestructor: Boolean,
) : ClassDescriptor {
    @Serializable
    class BuiltinMethod(
        override val name: String,
        @SerialName("return_type") val returnType: String? = null,
        @SerialName("is_vararg") override val isVararg: Boolean,
        @SerialName("is_const") override val isConst: Boolean,
        @SerialName("is_static") override val isStatic: Boolean,
        override val hash: Long? = null,
        @SerialName("hash_compatibility") override val hashCompatibility: List<Long> = emptyList(),
        override val arguments: List<MethodArg> = emptyList(),
    ) : MethodDescriptor

    @Serializable
    class BuiltinClassMember(override val name: String, val type: String) : Named

    @Serializable
    class BuiltinClassConstant(override val name: String, val type: String, override val value: String) :
        ConstantDescriptor<String>
}
