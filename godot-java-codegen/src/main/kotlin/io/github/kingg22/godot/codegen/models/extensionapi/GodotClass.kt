package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GodotClass(
    val name: String,
    @SerialName("is_refcounted") val isRefcounted: Boolean,
    @SerialName("is_instantiable") val isInstantiable: Boolean,
    val inherits: String? = null,
    @SerialName("api_type") val apiType: String,
    val constants: List<EnumConstant>? = null,
    val enums: List<ApiEnum> = emptyList(),
    val methods: List<ClassMethod>? = null,
    val properties: List<ClassProperty>? = null,
    val signals: List<Signal>? = null,
) {
    @Serializable
    data class ClassMethod(
        val name: String,
        @SerialName("is_const") val isConst: Boolean,
        @SerialName("is_vararg") val isVararg: Boolean,
        @SerialName("is_static") val isStatic: Boolean,
        @SerialName("is_virtual") val isVirtual: Boolean,
        @SerialName("is_required") val isRequired: Boolean? = null,
        val hash: Long? = null,
        @SerialName("hash_compatibility") val hashCompatibility: List<Long>? = null,
        @SerialName("return_value") val returnValue: MethodReturn? = null,
        val arguments: List<MethodArg>? = null,
    )

    @Serializable
    data class ClassProperty(
        val type: String,
        val name: String,
        val setter: String? = null,
        val getter: String? = null,
        val index: Int? = null,
    )
}
