package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.sanitizeTypeName
import io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor

/**
 * Generates top-level enums for Kotlin/Native.
 * Nested enums (Parent.Enum) are emitted as a top-level `ParentEnum` class in the Parent package.
 */
class NativeEnumGenerator {

    context(context: Context)
    fun generateFile(descriptor: EnumDescriptor): FileSpec {
        val spec = generateSpec(descriptor)
        return createFile(spec, spec.name!!, packageFor(descriptor.name))
    }

    fun generateSpec(descriptor: EnumDescriptor): TypeSpec {
        val enumName = enumTypeName(descriptor.name)
        val typeBuilder = TypeSpec.enumBuilder(enumName)
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addParameter("value", LONG)
                    .build(),
            )
            .addProperty(
                PropertySpec
                    .builder("value", LONG)
                    .initializer("value")
                    .build(),
            )

        descriptor.values.forEach { value ->
            typeBuilder.addEnumConstant(
                sanitizeTypeName(value.name),
                TypeSpec
                    .anonymousClassBuilder()
                    .addSuperclassConstructorParameter("%L", value.value)
                    .build(),
            )
        }
        return typeBuilder.build()
    }

    private fun enumTypeName(rawName: String): String {
        if (!rawName.contains('.')) return sanitizeTypeName(rawName.renameGodotClass())
        val parent = rawName.substringBeforeLast('.')
        val enumName = rawName.substringAfterLast('.')
        val parentName = sanitizeTypeName(parent.renameGodotClass())
        val nestedName = sanitizeTypeName(enumName.renameGodotClass())
        return parentName + nestedName
    }

    context(context: Context)
    private fun packageFor(rawName: String): String {
        if (!rawName.contains('.')) return context.packageForOrDefault(rawName)
        val parent = rawName.substringBeforeLast('.')
        return context.packageForOrDefault(parent)
    }
}
