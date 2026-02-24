package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
data class Constructor(val index: Int, val arguments: List<MethodArg>? = null)
