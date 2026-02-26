package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BuiltinSizes(
    @SerialName("build_configuration") val buildConfiguration: String,
    val sizes: List<BuiltinSizeForConfig>,
) {
    @Serializable
    class BuiltinSizeForConfig(val name: String, val size: Int)
}
