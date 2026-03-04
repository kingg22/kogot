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
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor

/**
 * Generates top-level enums for Kotlin/Native.
 * Nested enums (Parent.Enum) are emitted as a top-level `ParentEnum` class in the Parent package.
 */
class NativeEnumGenerator {

    context(context: Context)
    fun generateFile(descriptor: EnumDescriptor, parentClassName: String? = null): FileSpec {
        val spec = generateSpec(descriptor, parentClassName)
        return createFile(spec, spec.name!!, packageFor(descriptor.name))
    }

    context(context: Context)
    fun generateSpec(descriptor: EnumDescriptor, parentClassName: String? = null): TypeSpec {
        withExceptionContext({ "Generating enum '${descriptor.name}', values count: ${descriptor.values.size}" }) {
            val enumName = enumTypeName(descriptor.name)

            val typeBuilder = TypeSpec
                .enumBuilder(enumName)
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
            descriptor.description?.takeIf { it.isNotBlank() }?.let { typeBuilder.addKdoc("%S", it) }

            val constants = context.getConstantEnumNamesFor(parentClassName, descriptor.name)

            descriptor.values.zip(constants) { enumConstant, entryName ->
                withExceptionContext({ "Error generating enum constant '${enumConstant.name}' as $entryName" }) {
                    typeBuilder.addEnumConstant(
                        sanitizeTypeName(entryName),
                        TypeSpec
                            .anonymousClassBuilder()
                            .addSuperclassConstructorParameter("%L", enumConstant.value)
                            .apply { enumConstant.description?.takeIf { it.isNotBlank() }?.let { addKdoc("%S", it) } }
                            .build(),
                    )
                }
            }

            return typeBuilder.build()
        }
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
