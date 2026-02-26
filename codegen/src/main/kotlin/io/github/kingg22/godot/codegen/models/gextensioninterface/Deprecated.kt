package io.github.kingg22.godot.codegen.models.gextensioninterface

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Deprecated(
    @SerialName("since") val since: String,
    @SerialName("message") val message: String? = null,
    @SerialName("replace_with") val replaceWith: String? = null,
)
