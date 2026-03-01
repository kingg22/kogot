package io.github.kingg22.godot.codegen.impl.extensionapi.shared

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.commonConfiguration
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.snakeCaseToCamelCase
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor

/** Generates the sealed `Variant` class with nested enums from Godot's `Variant.*` global enums. */
class VariantGenerator(
    private val packageName: String,
    private val enumGen: EnumGenerator,
    private val typeResolver: TypeResolver,
) {
    fun generate(nestedEnums: List<EnumDescriptor>): FileSpec {
        val variantTypes = nestedEnums.find { it.name == "Variant.Type" }

        return withExceptionContext({ "Generating Variant class, nested enums count: ${nestedEnums.size}" }) {
            val variantClassName = ClassName(packageName, "Variant")

            val typeBuilder = TypeSpec
                .classBuilder(variantClassName)
                .addModifiers(KModifier.SEALED)

            variantTypes?.values?.forEach { variantType ->
                val enumValueName = variantType.name // "TYPE_PACKED_BYTE_ARRAY"
                val subclassName = enumValueName
                    .removePrefix("TYPE_") // "PACKED_BYTE_ARRAY" ← nombre de la subclase, en UPPER_SNAKE

                if (subclassName == "MAX") return@forEach

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

                // Tipo del value: convierte UPPER_SNAKE → PascalCase para que el resolver lo encuentre
                // "PACKED_BYTE_ARRAY" → "PackedByteArray" → typeResolver.resolve() → ClassName("", "PackedByteArray")
                val godotTypeName = subclassName
                    .lowercase()
                    .snakeCaseToCamelCase()
                    .replaceFirstChar { it.uppercaseChar() } // "PackedByteArray"

                // puede renombrar: "GodotString", "GodotObject", etc.
                val valueType = typeResolver.resolve(godotTypeName.renameGodotClass())

                // El nombre de la subclase NO pasa por el resolver ni por renameGodotClass —
                // queremos "STRING" no "GODOTSTRING", "OBJECT" no "GODOTOBJECT"
                // "STRING", "OBJECT", "PACKED_BYTE_ARRAY"
                val variantTypeSpec = TypeSpec
                    .classBuilder(subclassName)
                    .superclass(variantClassName)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("value", valueType)
                            .build(),
                    )
                    .addProperty(
                        PropertySpec
                            .builder("value", valueType)
                            .initializer("value")
                            .build(),
                    )
                    .apply { variantType.description?.let { addKdoc(it) } }

                typeBuilder.addType(variantTypeSpec.build())
            }

            typeBuilder.addTypes(
                nestedEnums.map {
                    withExceptionContext({ "Error generating nested enum '${it.name}'" }) {
                        enumGen.generate(it.copy(name = it.name.substringAfterLast(".")))
                    }
                },
            )

            FileSpec
                .builder(packageName, "Variant")
                .commonConfiguration()
                .addType(typeBuilder.build())
                .build()
        }
    }
}
