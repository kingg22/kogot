package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Operator private constructor(
    override val name: String,
    @SerialName("return_type") val returnType: String,
    @SerialName("right_type") val rightType: String? = null,
    override val description: String? = null,
) : Named,
    Documentable
