package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BuiltinClass private constructor(
    override val name: String,
    @SerialName("indexing_return_type") val indexingReturnType: String? = null,
    @SerialName("is_keyed") val isKeyed: Boolean,
    @SerialName("has_destructor") val hasDestructor: Boolean,
    @SerialName("brief_description") override val briefDescription: String? = null,
    override val description: String? = null,
    val members: List<BuiltinClassMember> = emptyList(),
    override val constants: List<BuiltinClassConstant> = emptyList(),
    override val enums: List<BuiltinEnum> = emptyList(),
    val operators: List<Operator> = emptyList(),
    override val methods: List<BuiltinMethod> = emptyList(),
    val constructors: List<Constructor> = emptyList(),
) : ClassDescriptor {

    @Serializable
    class BuiltinEnum private constructor(
        override val name: String,
        override val values: List<EnumConstant>,
        override val description: String? = null,
    ) : EnumDescriptor,
        Documentable

    @Serializable
    class Operator private constructor(
        override val name: String,
        @SerialName("return_type") val returnType: String,
        @SerialName("right_type") val rightType: String? = null,
        override val description: String? = null,
    ) : Named,
        Documentable

    @Serializable
    class Constructor private constructor(
        val index: Int,
        val arguments: List<MethodArg> = emptyList(),
        override val description: String? = null,
    ) : Documentable

    @Serializable
    class BuiltinMethod private constructor(
        override val name: String,
        @SerialName("return_type") val returnType: String? = null,
        @SerialName("is_vararg") override val isVararg: Boolean,
        @SerialName("is_const") override val isConst: Boolean,
        @SerialName("is_static") override val isStatic: Boolean,
        override val hash: Long,
        override val description: String? = null,
        @SerialName("hash_compatibility") override val hashCompatibility: List<Long> = emptyList(),
        override val arguments: List<MethodArg> = emptyList(),
    ) : MethodDescriptor

    @Serializable
    class BuiltinClassMember private constructor(
        override val name: String,
        val type: String,
        override val description: String? = null,
    ) : Named,
        Documentable

    @Serializable
    class BuiltinClassConstant private constructor(
        override val name: String,
        val type: String,
        override val value: String,
        override val description: String? = null,
    ) : ConstantDescriptor<String>
}
