package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class EngineClass(
    override val name: String,
    @SerialName("is_refcounted") val isRefcounted: Boolean,
    @SerialName("is_instantiable") val isInstantiable: Boolean,
    @SerialName("brief_description") override val briefDescription: String? = null,
    @SerialName("api_type") val apiType: String,
    override val description: String? = null,
    val inherits: String? = null,
    override val constants: List<EnumConstant> = emptyList(),
    override val enums: List<ApiEnum> = emptyList(),
    override val methods: List<ClassMethod> = emptyList(),
    val properties: List<ClassProperty> = emptyList(),
    val signals: List<Signal> = emptyList(),
) : ClassDescriptor {
    init {
        check(apiType == "core" || apiType == "editor") { "New api type founded: $apiType" }
    }

    @Serializable
    class Signal private constructor(
        override val name: String,
        val arguments: List<MethodArg> = emptyList(),
        override val description: String? = null,
    ) : Named,
        Documentable

    @Serializable
    class ClassMethod private constructor(
        override val name: String,
        @SerialName("is_const") override val isConst: Boolean,
        @SerialName("is_vararg") override val isVararg: Boolean,
        @SerialName("is_static") override val isStatic: Boolean,
        @SerialName("is_virtual") val isVirtual: Boolean,
        override val hash: Long,
        override val description: String? = null,
        @SerialName("is_required") val isRequired: Boolean? = null,
        @SerialName("hash_compatibility") override val hashCompatibility: List<Long> = emptyList(),
        @SerialName("return_value") val returnValue: MethodReturn? = null,
        override val arguments: List<MethodArg> = emptyList(),
    ) : MethodDescriptor

    @Serializable
    class ClassProperty private constructor(
        override val name: String,
        val type: String,
        val getter: String,
        val setter: String? = null,
        override val description: String? = null,
        val index: Int? = null,
    ) : Named,
        Documentable
}
