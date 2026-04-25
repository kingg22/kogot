package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
class MethodReturn(override val type: String, override val meta: String? = null) : TypeMetaHolder
