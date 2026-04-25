package io.github.kingg22.godot.codegen.models.extensionapi

sealed interface MethodDescriptor :
    Named,
    Hashable,
    Documentable {
    val isConst: Boolean
    val isVararg: Boolean
    val isStatic: Boolean
    val arguments: List<MethodArg>
}
