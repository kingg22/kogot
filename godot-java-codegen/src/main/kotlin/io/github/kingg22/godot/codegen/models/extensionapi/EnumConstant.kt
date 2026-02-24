package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
data class EnumConstant(val name: String, val value: Long)
