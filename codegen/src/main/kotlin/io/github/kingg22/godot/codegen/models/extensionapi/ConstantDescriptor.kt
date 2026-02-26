package io.github.kingg22.godot.codegen.models.extensionapi

interface ConstantDescriptor<out T> :
    Named,
    Documentable {
    val value: T
}
