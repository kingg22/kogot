package io.github.kingg22.godot.codegen.models.extensionapi

interface EnumDescriptor : Named {
    val values: List<EnumConstant>

    fun copy(name: String = this.name, values: List<EnumConstant> = this.values): EnumDescriptor =
        object : EnumDescriptor {
            override val name: String = name
            override val values: List<EnumConstant> = values
        }
}
