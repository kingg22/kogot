package io.github.kingg22.godot.codegen.impl.extensionapi.shared

import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor

interface EnumGenerator {
    fun generate(enum: EnumDescriptor): TypeSpec
}
