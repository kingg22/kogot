package io.github.kingg22.godot.codegen.models.extensionapi

interface ClassDescriptor : Named {
    val constants: List<ConstantDescriptor<Any>>
    val methods: List<MethodDescriptor>
    val enums: List<EnumDescriptor>
}
