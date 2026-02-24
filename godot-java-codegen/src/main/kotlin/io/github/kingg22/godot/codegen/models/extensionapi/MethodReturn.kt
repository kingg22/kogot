package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MethodReturn(@SerialName("type") val type: String, val meta: String? = null)
