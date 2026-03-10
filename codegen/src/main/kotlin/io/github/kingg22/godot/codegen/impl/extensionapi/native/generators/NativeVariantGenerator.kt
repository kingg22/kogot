package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.K_TODO
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.native.impl.VariantImplGen
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.screamingToPascalCase
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedEnum

/**
 * Generates the `Variant` sealed class and its nested `Type` / `Operator` enums.
 *
 * When [implGen] is provided (native implementation backend), the sealed class is augmented with:
 * - A private primary constructor backed by `CPointer<ByteVar>` storage
 * - `rawPtr: COpaquePointer` property
 * - `AutoCloseable.close()` that destroys the Godot Variant and frees native storage
 * - `NIL` as a `class` (not `object`) — every NIL variant owns a heap allocation
 * - Each typed subclass constructor that populates the Godot Variant via `variant_construct`
 *
 * When [implGen] is `null` (stub or interface-only backend), the original pure-API shape is
 * generated: `NIL` as `object`, no `rawPtr`, no lifecycle, pure Kotlin value wrappers.
 */
class NativeVariantGenerator(
    private val typeResolver: TypeResolver,
    private val enumGenerator: NativeEnumGenerator,
    private val implGen: VariantImplGen,
) {

    context(context: Context)
    fun generateSpec(variantEnums: List<ResolvedEnum>): TypeSpec {
        val spec = generateSpec(variantEnums.first { it.name == "Variant.Type" }).toBuilder()
        val enumsSpec = variantEnums.map { enum -> enumGenerator.generateSpec(enum) }
        spec.addTypes(enumsSpec)
        return spec.build()
    }

    context(context: Context)
    fun generateFile(variantEnums: List<ResolvedEnum>): FileSpec = createFile(
        type = generateSpec(variantEnums = variantEnums),
        fileName = "Variant",
        packageName = context.packageForOrDefault("Variant"),
    )

    context(context: Context)
    fun generateSpec(variantTypes: ResolvedEnum): TypeSpec = withExceptionContext({
        "Generating Variant class, nested enums count: ${variantTypes.raw.values.size}"
    }) {
        val variantClassName = ClassName(context.packageForOrDefault("Variant"), "Variant")

        val typeBuilder = TypeSpec
            .classBuilder(variantClassName)
            .addModifiers(KModifier.SEALED)

        // ── Native storage augmentation ───────────────────────────────────────
        // When implGen is present, the sealed class gets storage + rawPtr + close().
        // This MUST run before subclass generation so _cachedVariantSize is populated.
        implGen.configureVariantClass(typeBuilder)

        variantTypes.raw.values.forEach { variantType ->
            withExceptionContext({ "Generating Variant subclass '${variantType.name}'" }) {
                val enumValueName = variantType.name // e.g. "TYPE_PACKED_BYTE_ARRAY"
                val subclassName = enumValueName.removePrefix("TYPE_").takeUnless { it == "MAX" }
                    ?: return@forEach

                // ── NIL ───────────────────────────────────────────────────────
                if (subclassName == "NIL") {
                    val nilSpec = implGen
                        .buildNilSubclass(variantClassName)
                        .addKdocIfPresent(variantType)
                        .build()
                    typeBuilder.addType(nilSpec)
                    return@forEach
                }

                // ── Typed subclasses ──────────────────────────────────────────
                val godotTypeName = subclassName.screamingToPascalCase().renameGodotClass()
                val valueType = typeResolver.resolve(godotTypeName)

                val subclassBuilder = TypeSpec
                    .classBuilder(subclassName)
                    .superclass(variantClassName)
                    .addKdocIfPresent(variantType)

                // Build constructor — ctorBuilder.callSuperConstructor is set inside
                val ctorBuilder = FunSpec.constructorBuilder()
                    .addParameter("value", valueType)

                val initBody = implGen.buildSubclassConstructorBody(subclassName)

                subclassBuilder.primaryConstructor(ctorBuilder.build())
                subclassBuilder.addProperty(
                    PropertySpec.builder("value", valueType).initializer("value").build(),
                )

                // init block: real implementation
                subclassBuilder.addInitializerBlock(initBody)

                typeBuilder.addType(subclassBuilder.build())
            }
        }

        typeBuilder.build()
    }

    context(context: Context)
    fun generateFile(variantTypes: ResolvedEnum): FileSpec = createFile(
        type = generateSpec(variantTypes = variantTypes),
        fileName = "Variant",
        packageName = context.packageForOrDefault("Variant"),
    )
}
