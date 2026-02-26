package io.github.kingg22.godot.codegen.models.extensionapi

interface EnumDescriptor :
    Named,
    Documentable {
    val values: List<EnumConstant>

    fun copy(name: String = this.name): EnumDescriptor = object : EnumDescriptor {
        override val name: String = name
        override val values: List<EnumConstant> = this@EnumDescriptor.values
        override val description: String? = this@EnumDescriptor.description
    }
}
