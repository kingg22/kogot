package io.github.kingg22.godot.codegen.models.extensionapi.domain

import io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor

data class ResolvedEnum(val name: String, val shortName: String, val ownerName: String?, val raw: EnumDescriptor)
