package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
data class Members(val member: String, val offset: Int, val meta: String)
