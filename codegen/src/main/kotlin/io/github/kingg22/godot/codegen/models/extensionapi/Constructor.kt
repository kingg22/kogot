package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
class Constructor private constructor(
    val index: Int,
    val arguments: List<MethodArg> = emptyList(),
    override val description: String? = null,
) : Documentable
