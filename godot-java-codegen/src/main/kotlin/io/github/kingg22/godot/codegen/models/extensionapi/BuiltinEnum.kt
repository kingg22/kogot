package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
data class BuiltinEnum(val name: String, val values: List<EnumConstant>)
