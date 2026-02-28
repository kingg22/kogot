package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
class Signal(
    override val name: String,
    val arguments: List<MethodArg> = emptyList(),
    override val description: String? = null,
) : Named,
    Documentable
