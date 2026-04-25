package io.github.kingg22.godot.codegen.models.extensionapi.domain

import io.github.kingg22.godot.codegen.models.config.BuildConfiguration

data class ResolvedBuiltinLayout(
    val className: String,
    val buildConfiguration: BuildConfiguration,
    val size: Int,
    val align: Int,
    val memberOffsets: Map<String, Int>,
    val memberMeta: Map<String, String>,
)
