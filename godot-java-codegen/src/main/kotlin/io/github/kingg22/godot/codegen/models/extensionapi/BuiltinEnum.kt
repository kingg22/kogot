package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
class BuiltinEnum(override val name: String, override val values: List<EnumConstant>) : EnumDescriptor
