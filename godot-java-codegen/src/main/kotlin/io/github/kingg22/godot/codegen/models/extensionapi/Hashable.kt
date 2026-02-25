package io.github.kingg22.godot.codegen.models.extensionapi

interface Hashable {
    val hash: Long?
    val hashCompatibility: List<Long>
}
