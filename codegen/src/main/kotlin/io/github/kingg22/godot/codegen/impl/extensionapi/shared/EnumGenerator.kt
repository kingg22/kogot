package io.github.kingg22.godot.codegen.impl.extensionapi.shared

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor

interface EnumGenerator {
    fun generateFile(descriptor: EnumDescriptor): FileSpec
    fun generateSpec(descriptor: EnumDescriptor): TypeSpec
}
