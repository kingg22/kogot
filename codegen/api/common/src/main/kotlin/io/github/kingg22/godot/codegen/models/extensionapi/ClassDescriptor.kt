package io.github.kingg22.godot.codegen.models.extensionapi

sealed interface ClassDescriptor :
    Named,
    Documentable {
    val constants: List<ConstantDescriptor<Any>>
    val methods: List<MethodDescriptor>
    val enums: List<EnumDescriptor>
    val briefDescription: String?
}
