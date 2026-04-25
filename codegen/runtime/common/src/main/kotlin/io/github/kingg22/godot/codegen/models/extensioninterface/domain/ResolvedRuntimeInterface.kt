package io.github.kingg22.godot.codegen.models.extensioninterface.domain

import io.github.kingg22.godot.codegen.models.extensioninterface.Interface

data class ResolvedRuntimeInterface(val raw: Interface, val groupPrefix: String) {
    val name: String get() = raw.name
}
