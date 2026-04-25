package io.github.kingg22.godot.codegen.models.extensionapi.domain

import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass

data class ResolvedBuiltinClass(
    val raw: BuiltinClass,
    val layout: ResolvedBuiltinLayout?,
    val constructors: List<ResolvedBuiltinConstructor>,
    val enums: List<ResolvedEnum>,
) {
    val name: String get() = raw.name
    val shortName: String get() = raw.name.substringAfterLast('.')
    val hasDestructor: Boolean get() = raw.hasDestructor
    val isKeyed: Boolean get() = raw.isKeyed
}
