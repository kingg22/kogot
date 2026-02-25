package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GodotClass(
    override val name: String,
    @SerialName("is_refcounted") val isRefcounted: Boolean,
    @SerialName("is_instantiable") val isInstantiable: Boolean,
    val inherits: String? = null,
    @SerialName("api_type") val apiType: String,
    override val constants: List<EnumConstant> = emptyList(),
    override val enums: List<ApiEnum> = emptyList(),
    override val methods: List<ClassMethod> = emptyList(),
    val properties: List<ClassProperty> = emptyList(),
    val signals: List<Signal> = emptyList(),
) : ClassDescriptor {
    @Serializable
    class ClassMethod(
        override val name: String,
        @SerialName("is_const") override val isConst: Boolean,
        @SerialName("is_vararg") override val isVararg: Boolean,
        @SerialName("is_static") override val isStatic: Boolean,
        @SerialName("is_virtual") val isVirtual: Boolean,
        @SerialName("is_required") val isRequired: Boolean? = null,
        override val hash: Long? = null,
        @SerialName("hash_compatibility") override val hashCompatibility: List<Long> = emptyList(),
        @SerialName("return_value") val returnValue: MethodReturn? = null,
        override val arguments: List<MethodArg> = emptyList(),
    ) : MethodDescriptor

    @Serializable
    class ClassProperty(
        val type: String,
        val name: String,
        val setter: String? = null,
        val getter: String? = null,
        val index: Int? = null,
    )
}
