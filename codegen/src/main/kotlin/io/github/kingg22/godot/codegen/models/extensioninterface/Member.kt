package io.github.kingg22.godot.codegen.models.extensioninterface

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Member(val name: String, @SerialName("type") val type: String, val description: List<String> = emptyList())
