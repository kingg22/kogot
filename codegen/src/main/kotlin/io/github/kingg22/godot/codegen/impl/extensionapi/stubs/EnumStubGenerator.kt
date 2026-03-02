package io.github.kingg22.godot.codegen.impl.extensionapi.stubs

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.sanitizeTypeName
import io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor

/**
 * Generates a Kotlin `enum class` from a Godot [io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor].
 */
class EnumStubGenerator(private val packageName: String) {
    fun generateFile(descriptor: EnumDescriptor): FileSpec {
        val spec = generateSpec(descriptor)
        return createFile(spec, descriptor.name.renameGodotClass(), packageName)
    }

    fun generateSpec(descriptor: EnumDescriptor): TypeSpec {
        val typeBuilder = TypeSpec.enumBuilder(descriptor.name.renameGodotClass())
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
}
