package io.github.kingg22.godot.codegen.impl.extensionapi.knative.generators

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.sanitizeTypeName
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedEnum

/**
 * Generates top-level enums for Kotlin/Native.
 * Nested enums (Parent.Enum) are emitted as a top-level `ParentEnum` class in the Parent package.
 */
class NativeEnumGenerator {

    context(context: Context)
    fun generateFile(descriptor: ResolvedEnum): FileSpec {
        val spec = generateSpec(descriptor)
        return createFile(spec, spec.name!!, packageFor(descriptor))
    }

    context(context: Context)
    fun generateSpec(descriptor: ResolvedEnum): TypeSpec {
        withExceptionContext({ "Generating enum '${descriptor.name}', values count: ${descriptor.raw.values.size}" }) {
            val enumName = enumTypeName(descriptor.shortName)

            val typeBuilder = TypeSpec
                .enumBuilder(enumName)
                .addSuperinterface(context.classNameForOrDefault("GodotEnum", "GodotEnum"))
                .primaryConstructor(
                    FunSpec
                        .constructorBuilder()
                        .addParameter("value", LONG)
                        .build(),
                )
                .addProperty(
                    PropertySpec
                        .builder("value", LONG)
                        .addModifiers(KModifier.OVERRIDE)
                        .initializer("value")
                        .build(),
                )
                .addKdocIfPresent(descriptor.raw)
                .experimentalApiAnnotation(descriptor.name)

            val constants = context.getConstantEnumNamesFor(descriptor.ownerName, descriptor.shortName)

            check(descriptor.raw.values.size == constants.size) {
                "Enum '${descriptor.name}' has ${descriptor.raw.values.size} values, but enum shortener returns " +
                    "${constants.size} constants.\n" +
                    "Values: [${descriptor.raw.values.joinToString { it.name }}]\n" +
                    "Constants: [${constants.joinToString()}]"
            }

            val _ = descriptor.raw.values.zip(constants) { enumConstant, entryName ->
                withExceptionContext({ "Error generating enum constant '${enumConstant.name}' as $entryName" }) {
                    typeBuilder.addEnumConstant(
                        sanitizeTypeName(entryName),
                        TypeSpec
                            .anonymousClassBuilder()
                            .addSuperclassConstructorParameter("%L", enumConstant.value)
                            .addKdocIfPresent(enumConstant)
                            .build(),
                    )
                }
            }

            return typeBuilder.build()
        }
    }

    private fun enumTypeName(rawName: String): String {
        if (!rawName.contains('.')) return sanitizeTypeName(rawName.renameGodotClass())
        // Special cases
        if (rawName.startsWith("Variant.")) return sanitizeTypeName(rawName.removePrefix("Variant.").renameGodotClass())
        val parent = rawName.substringBeforeLast('.')
        val enumName = rawName.substringAfterLast('.')
        val parentName = sanitizeTypeName(parent.renameGodotClass())
        val nestedName = sanitizeTypeName(enumName.renameGodotClass())
        return parentName + nestedName
    }

    context(context: Context)
    private fun packageFor(descriptor: ResolvedEnum): String =
        descriptor.ownerName?.let(context::packageForOrDefault) ?: context.packageForOrDefault(descriptor.name)
}
