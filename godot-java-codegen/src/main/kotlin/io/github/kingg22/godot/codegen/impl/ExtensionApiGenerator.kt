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
    fun generate(api: ExtensionApi, outputDir: Path): List<Path> {
        val deferredNestedEnums = mutableListOf<ApiEnum>()
        return api.globalEnums.asSequence().mapNotNull { enumDef ->
            if (enumDef.name.contains(".")) {
                deferredNestedEnums += enumDef
                return@mapNotNull null
            }
            val enumSpec = generateEnum(enumDef)
            createFile(enumSpec, enumDef.name).writeTo(outputDir)
        }.plus(
            api.classes.asSequence().map { cls ->
                generateClass(cls).writeTo(outputDir)
            },
        ).plus(
            sequence {
                val fakeVariantClass = GodotClass(
                    name = "Variant",
                    isRefcounted = false,
                    isInstantiable = false,
                    apiType = "",
                    enums = deferredNestedEnums.mapNotNull {
                        if (it.name.startsWith("Variant.")) {
                            deferredNestedEnums.remove(it)
                            it.copy(name = it.name.substringAfter("Variant."))
                        } else {
                            null
                        }
                    },
                )
                yield(generateClass(fakeVariantClass).writeTo(outputDir))
                deferredNestedEnums.forEach { enumDef ->
                    System.err.println(
                        "WARNING: Enum '${enumDef.name}' is not in the global namespace and will be ignored.",
                    )
                }
            },
        ).toList()
    }

    private fun createFile(type: TypeSpec, fileName: String): FileSpec = FileSpec
        .builder(packageName, fileName)
        .commonConfiguration()
        .addType(type)
        .build()

    private fun generateEnum(enumDef: ApiEnum): TypeSpec {
        val typeBuilder = TypeSpec.enumBuilder(enumDef.name)
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
        return typeBuilder.build()
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

        val enumSpecs = cls.enums.map { enumDef -> generateEnum(enumDef) }

        typeBuilder.addTypes(enumSpecs)

        return createFile(typeBuilder.build(), cls.name)
    }
}
