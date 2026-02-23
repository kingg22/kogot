package io.github.kingg22.godot.codegen.models

import kotlinx.serialization.Serializable

@Serializable
data class ValueType(val type: String, val description: List<String> = emptyList())
