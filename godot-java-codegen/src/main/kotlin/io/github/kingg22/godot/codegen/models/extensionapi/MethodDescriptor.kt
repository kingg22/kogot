package io.github.kingg22.godot.codegen.models.extensionapi

interface MethodDescriptor :
    Named,
    Hashable {
    val isConst: Boolean
    val isVararg: Boolean
    val isStatic: Boolean
    val arguments: List<MethodArg>
}
