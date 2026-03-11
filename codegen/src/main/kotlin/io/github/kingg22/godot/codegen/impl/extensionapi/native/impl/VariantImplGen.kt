package io.github.kingg22.godot.codegen.impl.extensionapi.native.impl

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.K_AUTOCLOSEABLE
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.native.*
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.screamingToPascalCase
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedEnum

/**
 * Native implementation generator for the `Variant` sealed class.
 *
 * The standard [io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeVariantGenerator]
 * generates a pure-API sealed class that has no memory
 * backing — just wrapper subclasses holding Kotlin values. This class augments it with a proper
 * GDExtension memory lifecycle, so each `Variant` instance owns a Godot Variant allocation.
 *
 * ## What gets added to the sealed class
 *
 * ```kotlin
 * sealed class Variant private constructor(
 *     private val storage: CPointer<ByteVar>,
 * ) : AutoCloseable {
 *     private var closed: Boolean = false
 *
 *     internal val rawPtr: COpaquePointer
 *         get() = storage
 *
 *     override fun close() {
 *         if (!closed) {
 *             VariantBinding.instance.destroyRaw(rawPtr)
 *             freeBuiltinStorage(storage)
 *             closed = true
 *         }
 *     }
 * }
 * ```
 *
 * ## NIL becomes a `class`
 *
 * The base generator produces `NIL` as an `object`, but an `object` cannot hold per-instance
 * native storage. With this generator, `NIL` becomes a regular `class` that allocates a Godot
 * Variant and initialises it via `VariantBinding.instance.newNilRaw(rawPtr)`.
 *
 * ## Subclass constructor bodies
 *
 * Each typed subclass calls `super(allocateBuiltinStorage(variantSize))` and then populates the
 * Godot Variant through the appropriate GDExtension path:
 *
 * | Subclass  | GDExtension path                                                        |
 * |-----------|-------------------------------------------------------------------------|
 * | `NIL`     | `variant_new_nil`                                                       |
 * | `BOOL`    | `variant_construct(BOOL, rawPtr, [allocGdBool(value)], 1, null)`        |
 * | `INT`     | `variant_construct(INT, rawPtr, [LongVar(value).ptr], 1, null)`         |
 * | `FLOAT`   | `variant_construct(FLOAT, rawPtr, [DoubleVar(value).ptr], 1, null)`     |
 * | STRING … PACKED_VECTOR4_ARRAY | `variant_construct(TYPE, rawPtr, `value.rawPtr`, 1, null)` |
 * | `OBJECT`  | deferred — `TODO()`                                                     |
 *
 * ## Variant size
 *
 * `Variant` is NOT a `BuiltinClass` in the extension API JSON, so
 * `context.findResolvedBuiltinClass("Variant")` returns `null`.
 * The size is read directly from `context.extensionApi.builtinClassSizes`.
 */
class VariantImplGen(private val typeResolver: TypeResolver) {
    private lateinit var implPackageRegistry: ImplementationPackageRegistry

    /** Must be called before any generate* method. */
    fun initialize(implRegistry: ImplementationPackageRegistry) {
        implPackageRegistry = implRegistry
    }

    // ── Sealed class augmentation ─────────────────────────────────────────────

    /**
     * Augments [classBuilder] (which already has `SEALED` modifier) with:
     * - private primary constructor `(storage: CPointer<ByteVar>)`
     * - `storage` property
     * - `closed` guard
     * - `rawPtr` property
     * - `AutoCloseable` superinterface
     * - `close()` implementation
     *
     * Also caches the Variant size for use in [buildNilSubclass] and [buildSubclassConstructorBody].
     */
    context(context: Context)
    fun configureVariantClass(classBuilder: TypeSpec.Builder) {
        cachedVariantSize = resolveVariantSize(context)
        val variantSize = requireCachedSize()
        val allocStorage = implPackageRegistry.memberNameForOrDefault("allocateBuiltinStorage")

        val storageType = C_POINTER.parameterizedBy(BYTE_VAR)
        val storageProp = PropertySpec
            .builder("storage", storageType, KModifier.PRIVATE)
            .initializer("%M(%L)", allocStorage, variantSize)
            .build()

        classBuilder.addProperty(storageProp)
        classBuilder.addProperty(
            PropertySpec
                .builder("closed", BOOLEAN, KModifier.PRIVATE)
                .mutable(true)
                .initializer("false")
                .build(),
        )
        classBuilder.addProperty(
            PropertySpec
                .builder("rawPtr", COPAQUE_POINTER, KModifier.INTERNAL)
                .getter(FunSpec.getterBuilder().addStatement("return %N", storageProp).build())
                .build(),
        )

        classBuilder.addSuperinterface(K_AUTOCLOSEABLE)

        val variantBinding = implPackageRegistry.classNameForOrDefault("VariantBinding")
        val freeBuiltinStorage = implPackageRegistry.memberNameForOrDefault("freeBuiltinStorage")

        classBuilder.addFunction(
            FunSpec
                .builder("close")
                .addModifiers(KModifier.OVERRIDE)
                .addCode(
                    CodeBlock.builder()
                        .beginControlFlow("if (!closed)")
                        .addStatement("%T.instance.destroyRaw(rawPtr)", variantBinding)
                        .addStatement("%M(%N)", freeBuiltinStorage, "storage")
                        .addStatement("closed = true")
                        .endControlFlow()
                        .build(),
                )
                .build(),
        )

        classBuilder.addFunction(
            FunSpec
                .constructorBuilder()
                .addParameter(
                    ParameterSpec
                        .builder("from", COPAQUE_POINTER)
                        .addKdoc("Godot-owned variant pointer to copy from.")
                        .build(),
                )
                .addStatement("%T.instance.newCopyRaw(rawPtr, from)", variantBinding)
                .callThisConstructor()
                .build(),
        )
    }

    // ── NIL subclass ──────────────────────────────────────────────────────────

    /**
     * Builds the `NIL` subclass as a `class` (not an `object`).
     *
     * ```kotlin
     * class NIL : Variant(allocateBuiltinStorage(<size>)) {
     *     init {
     *         VariantBinding.instance.newNilRaw(rawPtr)
     *     }
     * }
     * ```
     *
     * Must be called after [configureVariantClass].
     */
    fun buildNilSubclass(variantClassName: ClassName): TypeSpec.Builder {
        val variantBinding = implPackageRegistry.classNameForOrDefault("VariantBinding")

        return TypeSpec
            .classBuilder("NIL")
            .superclass(variantClassName)
            .addInitializerBlock(
                CodeBlock
                    .builder()
                    .addStatement("%T.instance.newNilRaw(rawPtr)", variantBinding)
                    .build(),
            )
    }

    // ── Typed subclass constructors ───────────────────────────────────────────

    /**
     * Augments a typed subclass builder's constructor with:
     * 1. A `callSuperConstructor(allocateBuiltinStorage(variantSize))` delegation.
     * 2. Returns a [CodeBlock] to use as the constructor's `init` block (or body).
     *
     * Returns `null` for [subclassName]s that are not yet supported (e.g. `OBJECT`), so the
     * caller can fall back to `TODO()`.
     *
     * @param subclassName   The screaming-snake subclass name, e.g. `"BOOL"`, `"STRING"`.
     * @return The body block to use as the constructor's `init` block and the super call
     */
    fun buildSubclassConstructorBody(subclassName: String): CodeBlock {
        val getBinding = implPackageRegistry.classNameForOrDefault("GetBinding")
        val variantTypeConst = "GDEXTENSION_VARIANT_TYPE_$subclassName"

        return CodeBlock.builder().apply {
            // Paso 1: obtener el converter (igual para todos los casos)
            addStatement("val converter = %T.instance", getBinding)
            indent()
            addStatement(".variantFromTypeConstructorRaw(%L)", variantTypeConst)
            addStatement(
                "?: error(%S)",
                "Missing variant-from-type constructor for '$subclassName'",
            )
            unindent()

            // Paso 2: invocar converter(variantPtr, typePtr)
            // La firma es siempre: (GDExtensionVariantPtr, GDExtensionTypePtr) → Unit
            when (subclassName) {
                "BOOL" -> {
                    // Boolean necesita stack alloc como GDExtensionBool (UByte)
                    beginControlFlow("%M", memScoped)
                    addStatement(
                        "val boolVar = %M(value)",
                        implPackageRegistry.memberNameForOrDefault("allocGdBool"),
                    )
                    addStatement("converter.%M(rawPtr, boolVar)", cinteropInvoke)
                    endControlFlow()
                }

                "INT" -> {
                    // Long — stack alloc LongVar
                    beginControlFlow("%M", memScoped)
                    addStatement("val intVar = %M<%T>()", cinteropAlloc, LONG_VAR)
                    addStatement("intVar.%M = value.toLong()", cinteropValue)
                    addStatement("converter.%M(rawPtr, intVar.%M)", cinteropInvoke, cinteropPtr)
                    endControlFlow()
                }

                "FLOAT" -> {
                    // Double — stack alloc DoubleVar
                    beginControlFlow("%M", memScoped)
                    addStatement("val floatVar = %M<%T>()", cinteropAlloc, DOUBLE_VAR)
                    addStatement("floatVar.%M = value.toDouble()", cinteropValue)
                    addStatement("converter.%M(rawPtr, floatVar.%M)", cinteropInvoke, cinteropPtr)
                    endControlFlow()
                }

                else -> {
                    // STRING, VECTOR2, STRING_NAME, etc.
                    // value ya es un builtin con rawPtr — el converter copia directamente
                    addStatement("converter.%M(rawPtr, value.rawPtr)", cinteropInvoke)
                }
            }
        }.build()
    }

    // ── Utility members ───────────────────────────────────────────────────────

    /**
     * Augments [classBuilder] with all Variant utility member functions.
     *
     * Also returns the `asVariant()` extension functions (one per non-NIL typed subclass)
     * so the caller can emit them as top-level functions in the same file.
     */
    context(context: Context)
    fun buildUtilities(
        classBuilder: TypeSpec.Builder,
        variantClassName: ClassName,
        variantTypes: ResolvedEnum,
    ): List<FunSpec> {
        val variantBinding = implPackageRegistry.classNameForOrDefault("VariantBinding")
        val checkCallError = implPackageRegistry.memberNameForOrDefault("checkCallError")
        val stringNameClass = context.classNameForOrDefault("StringName")
        val godotStringClass = context.classNameForOrDefault("String", "GodotString")
        val variantTypeClass = variantClassName.nestedClass("Type")
        val variantOperatorClass = variantClassName.nestedClass("Operator")

        // ── getType() ─────────────────────────────────────────────────────────
        classBuilder.addFunction(
            FunSpec
                .builder("getType")
                .returns(variantTypeClass)
                .addCode(
                    CodeBlock
                        .builder()
                        .addStatement("val raw = %T.instance.getTypeRaw(rawPtr)", variantBinding)
                        .addStatement(
                            "return %T.entries.firstOrNull { it.value.toUInt() == raw.value } ?: %T.NIL",
                            variantTypeClass,
                            variantTypeClass,
                        )
                        .build(),
                )
                .build(),
        )

        // ── isNil() ───────────────────────────────────────────────────────────
        classBuilder.addFunction(
            FunSpec
                .builder("isNil")
                .returns(BOOLEAN)
                .addStatement(
                    "return %T.instance.getTypeRaw(rawPtr).value == %T.NIL.value.toUInt()",
                    variantBinding,
                    variantTypeClass,
                )
                .build(),
        )

        // ── evaluate() ────────────────────────────────────────────────────────
        classBuilder.addFunction(
            FunSpec
                .builder("evaluate")
                .returns(variantClassName.copy(nullable = true))
                .addParameter("rhs", variantClassName)
                .addParameter("op", variantOperatorClass)
                .addCode(
                    CodeBlock
                        .builder()
                        .addStatement("val result = %T()", variantClassName)
                        .addStatement(
                            "val status = %T.instance",
                            variantBinding,
                        )
                        .indent()
                        .addStatement(
                            ".evaluate(op.%M(), rawPtr, rhs.rawPtr, result.rawPtr)",
                            implPackageRegistry.memberNameForOrDefault("toGDExtensionVariantOperator"),
                        )
                        .unindent()
                        .beginControlFlow("return if (status.valid == true)")
                        .addStatement("result")
                        .nextControlFlow("else")
                        .addStatement("result.close()")
                        .addStatement("null")
                        .endControlFlow()
                        .build(),
                )
                .build(),
        )

        // ── booleanize() ──────────────────────────────────────────────────────
        classBuilder.addFunction(
            FunSpec
                .builder("booleanize")
                .returns(BOOLEAN)
                .addStatement("return %T.instance.booleanize(rawPtr)", variantBinding)
                .build(),
        )

        // ── stringify() ───────────────────────────────────────────────────────
        classBuilder.addFunction(
            FunSpec
                .builder("stringify")
                .returns(godotStringClass)
                .addCode(
                    CodeBlock.builder()
                        .addStatement("val result = %T()", godotStringClass)
                        .addStatement("%T.instance.stringifyRaw(rawPtr, result.rawPtr)", variantBinding)
                        .addStatement("return result")
                        .build(),
                )
                .build(),
        )

        // ── hashVariant() ─────────────────────────────────────────────────────
        classBuilder.addFunction(
            FunSpec
                .builder("hashVariant")
                .returns(LONG)
                .addKdoc("Named `hashVariant` to avoid collision with [Any.hashCode].")
                .addStatement("return %T.instance.hashRaw(rawPtr)", variantBinding)
                .build(),
        )

        // ── duplicate() ───────────────────────────────────────────────────────
        classBuilder.addFunction(
            FunSpec
                .builder("duplicate")
                .returns(variantClassName)
                .addParameter(
                    ParameterSpec
                        .builder("deep", BOOLEAN)
                        .defaultValue("false")
                        .addKdoc("If `true`, nested containers are also duplicated.")
                        .build(),
                )
                .addCode(
                    CodeBlock
                        .builder()
                        .addStatement("val result = %T()", variantClassName)
                        .addStatement("%T.instance.duplicate(rawPtr, result.rawPtr, deep)", variantBinding)
                        .addStatement("return result")
                        .build(),
                )
                .build(),
        )

        // ── call() ────────────────────────────────────────────────────────────
        classBuilder.addFunction(
            FunSpec
                .builder("call")
                .returns(variantClassName)
                .addParameter("method", stringNameClass)
                .addParameter("args", variantClassName, KModifier.VARARG)
                .addCode(
                    CodeBlock
                        .builder()
                        .addStatement("val result = %T()", variantClassName)
                        .addStatement("val errorInfo = %T.instance.call(", variantBinding)
                        .indent()
                        .addStatement("rawPtr,")
                        .addStatement("method.rawPtr,")
                        .addStatement("*args.map { it.rawPtr }.toTypedArray(),")
                        .addStatement("rReturn = result.rawPtr,")
                        .unindent()
                        .addStatement(")")
                        .addStatement($$"""%M("Variant.call: $method", errorInfo)""", checkCallError)
                        .addStatement("return result")
                        .build(),
                )
                .build(),
        )

        // ── callStatic() ──────────────────────────────────────────────────────
        classBuilder.addFunction(
            FunSpec
                .builder("callStatic")
                .returns(variantClassName)
                .addParameter("type", variantTypeClass)
                .addParameter("method", stringNameClass)
                .addParameter("args", variantClassName, KModifier.VARARG)
                .addCode(
                    CodeBlock
                        .builder()
                        .addStatement("val result = %T()", variantClassName)
                        .addStatement("val errorInfo = %T.instance.callStatic(", variantBinding)
                        .indent()
                        .addStatement(
                            "type.%M(),",
                            implPackageRegistry.memberNameForOrDefault("toGDExtensionVariantType"),
                        )
                        .addStatement("method.rawPtr,")
                        .addStatement("*args.map { it.rawPtr }.toTypedArray(),")
                        .addStatement("rReturn = result.rawPtr,")
                        .unindent()
                        .addStatement(")")
                        .addStatement($$"""%M("Variant.callStatic: $type.$method", errorInfo)""", checkCallError)
                        .addStatement("return result")
                        .build(),
                )
                .build(),
        )

        return buildAsVariantFunctions(variantClassName, variantTypes)
    }

    // ── asVariant() top-level functions ───────────────────────────────────────

    context(_: Context)
    private fun buildAsVariantFunctions(variantClassName: ClassName, variantTypes: ResolvedEnum): List<FunSpec> =
        variantTypes.raw.values.mapNotNull { enumValue ->
            val subclassName = enumValue.name.removePrefix("TYPE_")
                .takeUnless { it == "MAX" || it == "NIL" } ?: return@mapNotNull null

            // ── Typed subclasses ──────────────────────────────────────────
            val godotTypeName = subclassName.screamingToPascalCase().renameGodotClass()
            val receiverType = typeResolver.resolve(godotTypeName)

            FunSpec
                .builder("asVariant")
                .receiver(receiverType.copy(nullable = true))
                .returns(variantClassName)
                .addCode("return ")
                .beginControlFlow("if (this == null)")
                .addStatement("%T.NIL()", variantClassName)
                .nextControlFlow("else")
                .addStatement(
                    "%T.%L(this)",
                    variantClassName,
                    subclassName,
                )
                .endControlFlow()
                .build()
        }

    // ── Variant size ──────────────────────────────────────────────────────────

    /**
     * Looks up the Variant size from `builtinClassSizes` in the extension API JSON.
     *
     * `Variant` is present in `builtin_class_sizes` but NOT in `builtin_classes`,
     * so `context.findResolvedBuiltinClass("Variant")` always returns null.
     */
    private fun resolveVariantSize(context: Context): Int {
        val buildConfigName = context.model.buildConfiguration.jsonName
        return context.extensionApi.builtinClassSizes
            .firstOrNull { it.buildConfiguration == buildConfigName }
            ?.sizes
            ?.firstOrNull { it.name == "Variant" }
            ?.size
            ?: error("Cannot find Variant size for build config '$buildConfigName'")
    }

    private fun requireCachedSize(): Int {
        check(cachedVariantSize > 0) {
            "VariantImplGen: configureVariantClass() must be called before building subclasses"
        }
        return cachedVariantSize
    }

    // State set by configureVariantClass so subclass helpers don't need to re-resolve it
    private var cachedVariantSize: Int = -1
}
