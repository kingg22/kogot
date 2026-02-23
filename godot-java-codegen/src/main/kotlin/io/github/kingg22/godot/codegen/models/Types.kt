package io.github.kingg22.godot.codegen.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
sealed class Types {
    abstract val name: String
    abstract val description: List<String>
    abstract val deprecated: Deprecated?

    @Serializable
    @SerialName("enum")
    data class EnumType(
        override val name: String,
        override val description: List<String> = emptyList(),
        override val deprecated: Deprecated? = null,
        @SerialName("is_bitfield") val isBitfield: Boolean? = null,
        val values: List<EnumValue>,
    ) : Types() {
        @Serializable
        data class EnumValue(val name: String, val value: Int, val description: List<String> = emptyList())
    }

    @Serializable
    @SerialName("handle")
    data class HandleType(
        override val name: String,
        override val description: List<String> = emptyList(),
        override val deprecated: Deprecated? = null,
        val parent: String? = null,
        @SerialName("is_const") val isConst: Boolean? = null,
        @SerialName("is_uninitialized") val isUninitialized: Boolean? = null,
    ) : Types()

    @Serializable
    @SerialName("alias")
    data class AliasType(
        override val name: String,
        override val description: List<String> = emptyList(),
        override val deprecated: Deprecated? = null,
        val type: String,
    ) : Types()

    @Serializable
    @SerialName("struct")
    data class StructType(
        override val name: String,
        override val description: List<String> = emptyList(),
        override val deprecated: Deprecated? = null,
        val members: List<Member>,
    ) : Types()

    @Serializable
    @SerialName("function")
    data class FunctionType(
        override val name: String,
        override val description: List<String> = emptyList(),
        override val deprecated: Deprecated? = null,
        @SerialName("return_value") val returnValue: ValueType? = null,
        val arguments: List<Arguments>,
    ) : Types()
}
