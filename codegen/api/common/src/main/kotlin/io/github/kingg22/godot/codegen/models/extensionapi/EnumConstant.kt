package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.Serializable

@Serializable
class EnumConstant(override val name: String, override val value: Long, override val description: String? = null) :
    ConstantDescriptor<Long>
