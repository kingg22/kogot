package io.github.kingg22.godot.codegen.impl

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.models.extensionapi.ApiEnum
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensionapi.GodotClass
import java.nio.file.Path

class ExtensionApiGenerator(private val packageName: String) {
    fun generate(api: ExtensionApi, outputDir: Path): List<Path> = api.globalEnums.asSequence().map { enumDef ->
        generateEnum(enumDef).writeTo(outputDir)
    }.plus(
        api.classes.asSequence().flatMap { cls ->
            sequence {
                yield(generateClass(cls).writeTo(outputDir))
                yieldAll(
                    cls.enums.asSequence().map { enumDef ->
                        generateEnum(enumDef, cls.name).writeTo(outputDir)
                    },
                )
            }
        },
    ).toList()

    private fun generateEnum(enumDef: ApiEnum, owner: String? = null): FileSpec {
        // TODO check this new name
        // Can't escape identifier `Variant.Type` because it contains illegal characters: .
        val (enumName, originalName) = if (owner == null) {
            enumDef.name.replace(".", "") to enumDef.name
        } else {
            "${owner.replace(".", "")}${enumDef.name.replace(".", "")}" to "$owner -> ${enumDef.name}"
        }
        val typeBuilder = TypeSpec.enumBuilder(enumName)
            .addKdoc("Original name is: `$originalName`")
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", LONG)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("value", LONG)
                    .initializer("value")
                    .build(),
            )

        enumDef.values.forEach { value ->
            val enumConst = TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter("%L", value.value)
                .build()
            typeBuilder.addEnumConstant(sanitizeEnumConstant(value.name), enumConst)
        }

        return FileSpec.builder(packageName, enumName)
            .addType(typeBuilder.build())
            .build()
    }

    private fun generateClass(cls: GodotClass): FileSpec {
        val typeBuilder = TypeSpec.classBuilder(cls.name)
            .addModifiers(KModifier.ABSTRACT)

        val parent = cls.inherits?.takeIf { it.isNotBlank() }
        if (parent != null) {
            typeBuilder.superclass(typeNameFor(packageName, parent))
        }

        cls.methods?.forEach { method ->
            val funSpec = FunSpec.builder(safeIdentifier(method.name))
                .addModifiers(KModifier.OPEN)
                .addParameters(methodArgsToParameters(packageName, method.arguments))
                .returns(methodReturnTypeName(packageName, method.returnValue))
                .addStatement("TODO()")
                .build()
            typeBuilder.addFunction(funSpec)
        }

        return FileSpec.builder(packageName, cls.name)
            .addType(typeBuilder.build())
            .build()
    }
}
