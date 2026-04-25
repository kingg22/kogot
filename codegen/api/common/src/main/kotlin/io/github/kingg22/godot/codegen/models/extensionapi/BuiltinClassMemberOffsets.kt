package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BuiltinClassMemberOffsets(
    @SerialName("build_configuration") val buildConfiguration: String,
    val classes: List<Classes>,
) {
    @Serializable
    class Classes(val name: String, val members: List<Members>)

    @Serializable
    class Members(val member: String, val offset: Int, val meta: String)
}
