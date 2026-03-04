package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.screamingToPascalCase
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor

/** Generates the sealed `Variant` class without nested enums (native backend emits them top-level). */
class NativeVariantGenerator(private val typeResolver: TypeResolver, private val enumGenerator: NativeEnumGenerator) {

    context(context: Context)
    fun generateSpec(variantEnums: List<EnumDescriptor>): TypeSpec {
        val spec = generateSpec(variantEnums.find { it.name == "Variant.Type" }).toBuilder()
        variantEnums.forEach { enum ->
            spec.addType(enumGenerator.generateSpec(enum.copy(name = enum.name.removePrefix("Variant.")), "Variant"))
        }
        return spec.build()
    }

    context(context: Context)
    fun generateFile(variantEnums: List<EnumDescriptor>): FileSpec = createFile(
        type = generateSpec(variantEnums = variantEnums),
        fileName = "Variant",
        packageName = context.packageForOrDefault("Variant"),
    )

    context(context: Context)
    fun generateSpec(variantTypes: EnumDescriptor?): TypeSpec {
        return withExceptionContext({ "Generating Variant class, nested enums count: ${variantTypes?.values?.size}" }) {
            val variantClassName = ClassName(context.packageForOrDefault("Variant"), "Variant")

            val typeBuilder = TypeSpec
                .classBuilder(variantClassName)
                .addModifiers(KModifier.SEALED)

            if (variantTypes == null) println("WARNING: Variant types are null, generating empty Variant class")

            variantTypes?.values?.forEach { variantType ->
                withExceptionContext({ "Generating Variant subclass '${variantType.name}'" }) {
                    val enumValueName = variantType.name // "TYPE_PACKED_BYTE_ARRAY"
                    val subclassName = enumValueName.removePrefix("TYPE_").takeUnless { it == "MAX" } ?: return@forEach

                    if (subclassName == "NIL") {
                        typeBuilder.addType(
                            TypeSpec
                                .objectBuilder("NIL")
                                .superclass(variantClassName)
                                .apply { variantType.description?.let { addKdoc(it) } }
                                .build(),
                        )
                        return@forEach
                    }

                    val godotTypeName = subclassName.screamingToPascalCase()

                    val valueType = typeResolver.resolve(godotTypeName.renameGodotClass())

                    val variantTypeSpec = TypeSpec
                        .classBuilder(subclassName)
                        .superclass(variantClassName)
                        .primaryConstructor(
                            FunSpec
                                .constructorBuilder()
                                .addParameter("value", valueType)
                                .build(),
                        )
                        .addProperty(
                            PropertySpec
                                .builder("value", valueType)
                                .initializer("value")
                                .build(),
                        )

                    variantType.description?.let { variantTypeSpec.addKdoc(it) }

                    typeBuilder.addType(variantTypeSpec.build())
                }
            }

            typeBuilder.build()
        }
    }

    context(context: Context)
    fun generateFile(variantTypes: EnumDescriptor?): FileSpec = createFile(
        type = generateSpec(variantTypes = variantTypes),
        fileName = "Variant",
        packageName = context.packageForOrDefault("Variant"),
    )
}
