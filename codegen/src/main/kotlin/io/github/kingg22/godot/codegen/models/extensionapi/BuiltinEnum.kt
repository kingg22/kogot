package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
class BuiltinEnum private constructor(
    override val name: String,
    override val values: List<EnumConstant>,
    override val description: String? = null,
) : EnumDescriptor,
    Documentable
