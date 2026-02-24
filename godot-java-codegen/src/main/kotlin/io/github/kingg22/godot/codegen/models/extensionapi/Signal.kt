package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
data class Signal(val name: String, val arguments: List<MethodArg>? = null)
