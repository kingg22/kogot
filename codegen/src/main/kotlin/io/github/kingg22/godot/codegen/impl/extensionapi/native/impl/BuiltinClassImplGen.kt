package io.github.kingg22.godot.codegen.impl.extensionapi.native.impl

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.native.*
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.BodyGenerator
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinConstructor

// ── Named constants ────────────────────────────────────────────────────────────
private const val VARIANT_TYPE_STRING = "GDEXTENSION_VARIANT_TYPE_STRING"
private const val VARIANT_TYPE_STRING_NAME = "GDEXTENSION_VARIANT_TYPE_STRING_NAME"
private const val VARIANT_TYPE_NODE_PATH = "GDEXTENSION_VARIANT_TYPE_NODE_PATH"

/**
 * All Godot builtin class types that need native memory backing (storage + rawPtr).
 *
 * Covers every non-primitive builtin. Types without a destructor (Vector2, Color, etc.)
 * still need storage + rawPtr so they can be passed to other GDExtension ptr-constructors.
 *
 * `Variant` itself is NOT here — handled separately by `VariantImplGen`.
 */
private val STORAGE_BACKED_BUILTINS = setOf(
    "String", "StringName", "NodePath",
    // Math types — no destructor, but need rawPtr
    "Vector2", "Vector2i", "Rect2", "Rect2i",
    "Vector3", "Vector3i", "Transform2D",
    "Vector4", "Vector4i", "Plane", "Quaternion",
    "Aabb", "Basis", "Transform3D", "Projection",
    "Color",
    // Misc — have destructors
    "Rid", "Callable", "Signal", "Dictionary", "Array",
    // Packed arrays — all have destructors
    "PackedByteArray", "PackedInt32Array", "PackedInt64Array",
    "PackedFloat32Array", "PackedFloat64Array", "PackedStringArray",
    "PackedVector2Array", "PackedVector3Array", "PackedColorArray",
    "PackedVector4Array",
)

/** Explicit map of Godot class name → GDEXTENSION_VARIANT_TYPE_* constant. */
private fun variantTypeConst(godotName: String): String = when (godotName) {
    "String" -> VARIANT_TYPE_STRING
    "StringName" -> VARIANT_TYPE_STRING_NAME
    "NodePath" -> VARIANT_TYPE_NODE_PATH
    "Vector2" -> "GDEXTENSION_VARIANT_TYPE_VECTOR2"
    "Vector2i" -> "GDEXTENSION_VARIANT_TYPE_VECTOR2I"
    "Rect2" -> "GDEXTENSION_VARIANT_TYPE_RECT2"
    "Rect2i" -> "GDEXTENSION_VARIANT_TYPE_RECT2I"
    "Vector3" -> "GDEXTENSION_VARIANT_TYPE_VECTOR3"
    "Vector3i" -> "GDEXTENSION_VARIANT_TYPE_VECTOR3I"
    "Transform2D" -> "GDEXTENSION_VARIANT_TYPE_TRANSFORM2D"
    "Vector4" -> "GDEXTENSION_VARIANT_TYPE_VECTOR4"
    "Vector4i" -> "GDEXTENSION_VARIANT_TYPE_VECTOR4I"
    "Plane" -> "GDEXTENSION_VARIANT_TYPE_PLANE"
    "Quaternion" -> "GDEXTENSION_VARIANT_TYPE_QUATERNION"
    "Aabb" -> "GDEXTENSION_VARIANT_TYPE_AABB"
    "Basis" -> "GDEXTENSION_VARIANT_TYPE_BASIS"
    "Transform3D" -> "GDEXTENSION_VARIANT_TYPE_TRANSFORM3D"
    "Projection" -> "GDEXTENSION_VARIANT_TYPE_PROJECTION"
    "Color" -> "GDEXTENSION_VARIANT_TYPE_COLOR"
    "Rid" -> "GDEXTENSION_VARIANT_TYPE_RID"
    "Callable" -> "GDEXTENSION_VARIANT_TYPE_CALLABLE"
    "Signal" -> "GDEXTENSION_VARIANT_TYPE_SIGNAL"
    "Dictionary" -> "GDEXTENSION_VARIANT_TYPE_DICTIONARY"
    "Array" -> "GDEXTENSION_VARIANT_TYPE_ARRAY"
    "PackedByteArray" -> "GDEXTENSION_VARIANT_TYPE_PACKED_BYTE_ARRAY"
    "PackedInt32Array" -> "GDEXTENSION_VARIANT_TYPE_PACKED_INT32_ARRAY"
    "PackedInt64Array" -> "GDEXTENSION_VARIANT_TYPE_PACKED_INT64_ARRAY"
    "PackedFloat32Array" -> "GDEXTENSION_VARIANT_TYPE_PACKED_FLOAT32_ARRAY"
    "PackedFloat64Array" -> "GDEXTENSION_VARIANT_TYPE_PACKED_FLOAT64_ARRAY"
    "PackedStringArray" -> "GDEXTENSION_VARIANT_TYPE_PACKED_STRING_ARRAY"
    "PackedVector2Array" -> "GDEXTENSION_VARIANT_TYPE_PACKED_VECTOR2_ARRAY"
    "PackedVector3Array" -> "GDEXTENSION_VARIANT_TYPE_PACKED_VECTOR3_ARRAY"
    "PackedColorArray" -> "GDEXTENSION_VARIANT_TYPE_PACKED_COLOR_ARRAY"
    "PackedVector4Array" -> "GDEXTENSION_VARIANT_TYPE_PACKED_VECTOR4_ARRAY"
    else -> error("Unknown builtin class name for variantTypeConst: $godotName")
}

/**
 * Generates implementation bodies for Godot builtin classes.
 *
 * Injecting [typeResolver] is required so that constructor argument types are resolved with
 * their `meta` hint (e.g. `meta:"int32"` → `Int`, `meta:"float"` → `Float`) before deciding
 * which `*Var` stack allocation to emit inside a `memScoped` block.
 */
class BuiltinClassImplGen(private val delegate: BodyGenerator, private val typeResolver: TypeResolver) {
    private lateinit var implPackageRegistry: ImplementationPackageRegistry

    fun todoBody() = delegate.todoBody()
    fun todoGetter() = delegate.todoGetter()

    fun initialize(implementationPackageRegistry: ImplementationPackageRegistry) {
        implPackageRegistry = implementationPackageRegistry
    }

    // ── Storage infrastructure ────────────────────────────────────────────────

    fun configureStorageBackedBuiltin(
        builtinClass: ResolvedBuiltinClass,
        classBuilder: TypeSpec.Builder,
    ): TypeSpec.Builder = classBuilder.apply {
        if (builtinClass.name !in STORAGE_BACKED_BUILTINS) return@apply

        val storageType = C_POINTER.parameterizedBy(BYTE_VAR)
        val storageProperty = PropertySpec
            .builder("storage", storageType, KModifier.PRIVATE)
            .initializer("storage")
            .build()

        classBuilder.primaryConstructor(
            FunSpec
                .constructorBuilder()
                .addParameter("storage", storageType)
                .addModifiers(KModifier.PRIVATE)
                .build(),
        )
        classBuilder.addProperty(storageProperty)

        if (builtinClass.hasDestructor) {
            classBuilder.addProperty(
                PropertySpec
                    .builder("closed", BOOLEAN, KModifier.PRIVATE)
                    .mutable(true)
                    .initializer("false")
                    .build(),
            )
        }

        classBuilder.addProperty(
            PropertySpec.builder("rawPtr", COPAQUE_POINTER, KModifier.INTERNAL)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return %N", storageProperty)
                        .build(),
                )
                .build(),
        )
    }

    // ── close() ───────────────────────────────────────────────────────────────

    /**
     * Generates the `close()` override for builtin classes that have a GDExtension destructor.
     *
     * Note: `NativeBuiltinClassGenerator` only calls this when `hasDestructor == true`, so
     * math types (Vector2, Color, …) in `STORAGE_BACKED_BUILTINS` without a destructor
     * never reach here.
     */
    fun buildCloseFunction(builtinClass: ResolvedBuiltinClass): FunSpec {
        if (!builtinClass.hasDestructor || builtinClass.name !in STORAGE_BACKED_BUILTINS) {
            return FunSpec
                .builder("close")
                .addModifiers(KModifier.OVERRIDE)
                .addCode(todoBody())
                .build()
        }

        return FunSpec
            .builder("close")
            .addModifiers(KModifier.OVERRIDE)
            .addCode(
                CodeBlock
                    .builder()
                    .beginControlFlow("if (!closed)")
                    .add(destroyCallFor(builtinClass))
                    .addStatement(
                        "%M(%N)",
                        implPackageRegistry.memberNameForOrDefault("freeBuiltinStorage"),
                        "storage",
                    )
                    .addStatement("closed = true")
                    .endControlFlow()
                    .build(),
            )
            .build()
    }

    // ── Constructor bodies ────────────────────────────────────────────────────

    context(context: Context)
    fun constructorBodyFor(
        builtinClass: ResolvedBuiltinClass,
        ctor: ResolvedBuiltinConstructor,
        ctorBuilder: FunSpec.Builder,
    ): CodeBlock {
        if (builtinClass.name !in STORAGE_BACKED_BUILTINS) return todoBody()

        ctorBuilder.callThisConstructor(
            CodeBlock.of(
                "%M(%L)",
                implPackageRegistry.memberNameForOrDefault("allocateBuiltinStorage"),
                builtinStorageSize(builtinClass),
            ),
        )

        // Any constructor arg that requires an engine class (e.g., Object in Signal/Callable)
        // cannot be handled yet — those classes are not builtin and have no rawPtr defined.
        if (ctor.arguments.any { isEngineClass(it, context) }) return todoBody()

        return CodeBlock
            .builder()
            .add(constructorInvocation(builtinClass, ctor))
            .build()
    }

    context(context: Context)
    fun stringConstructorBodyFor(builtinClass: ResolvedBuiltinClass, ctorBuilder: FunSpec.Builder): CodeBlock {
        if (builtinClass.name in STORAGE_BACKED_BUILTINS) {
            ctorBuilder.callThisConstructor(
                CodeBlock.of(
                    "%M(%L)",
                    implPackageRegistry.memberNameForOrDefault("allocateBuiltinStorage"),
                    builtinStorageSize(builtinClass),
                ),
            )
        }

        return when (builtinClass.name) {
            "String" -> CodeBlock.builder()
                .addStatement(
                    "%T.instance.newWithUtf8Chars(rawPtr, value)",
                    implPackageRegistry.classNameForOrDefault("StringBinding"),
                )
                .build()

            "StringName" -> CodeBlock.builder()
                .addStatement(
                    "%T.instance.nameNewWithUtf8Chars(rawPtr, value)",
                    implPackageRegistry.classNameForOrDefault("StringBinding"),
                )
                .build()

            "NodePath" -> CodeBlock.builder()
                .beginControlFlow(
                    "%T(value).use { godotString ->",
                    context.classNameForOrDefault("String", "GodotString"),
                )
                .add(callBuiltinConstructorSimple(VARIANT_TYPE_NODE_PATH, 2, "godotString.rawPtr"))
                .endControlFlow()
                .build()

            else -> error(
                "Unreachable, the following builtin class doesn't support synthetic kotlin.String constructor: $builtinClass",
            )
        }
    }

    // ── Destructor call ───────────────────────────────────────────────────────

    fun destroyCallFor(builtinClass: ResolvedBuiltinClass): CodeBlock =
        destroyBuiltin(variantTypeConst(builtinClass.name))

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun builtinStorageSize(builtinClass: ResolvedBuiltinClass): Int =
        builtinClass.layout?.size ?: error("Missing layout size for ${builtinClass.name}")

    /**
     * Returns true if this constructor argument's type is an engine class (not a builtin).
     * Engine classes don't have `rawPtr` yet, so constructors using them fall back to `TODO()`.
     */
    private fun isEngineClass(arg: MethodArg, context: Context): Boolean {
        val type = arg.type
        return !isPrimitiveGodotType(type) &&
            type !in STORAGE_BACKED_BUILTINS &&
            type != "Variant" &&
            !type.startsWith("enum::") &&
            !type.startsWith("bitfield::") &&
            !type.startsWith("typedarray::") &&
            context.findResolvedBuiltinClass(type) == null &&
            context.findResolvedEngineClass(type) != null
    }

    private fun isPrimitiveGodotType(type: String): Boolean = type in setOf("float", "double", "int", "bool", "void")

    /**
     * Generates the full constructor invocation CodeBlock.
     *
     * For String-bridge constructors and the known String/StringName/NodePath cases,
     * delegates to the existing specific implementations.
     *
     * For all other storage-backed builtins, uses the **generic** path: resolves each arg's
     * Kotlin type (respecting `meta` hints), emits the correct `*Var` stack-alloc inside
     * `memScoped`, and builds the `getPtrConstructorRaw` + `invoke` call.
     */
    context(context: Context)
    private fun constructorInvocation(
        builtinClass: ResolvedBuiltinClass,
        ctor: ResolvedBuiltinConstructor,
    ): CodeBlock = when {
        ctor.usesKotlinStringBridge -> when (builtinClass.name) {
            "String" -> CodeBlock.of(
                "%T.instance.newWithUtf8Chars(rawPtr, value)",
                implPackageRegistry.classNameForOrDefault("StringBinding"),
            )

            "StringName" -> CodeBlock.of(
                "%T.instance.nameNewWithUtf8Chars(rawPtr, value)",
                implPackageRegistry.classNameForOrDefault("StringBinding"),
            )

            "NodePath" -> CodeBlock.builder()
                .beginControlFlow(
                    "%T(value).use { godotString ->",
                    context.classNameForOrDefault("String", "GodotString"),
                )
                .add(callBuiltinConstructorSimple(VARIANT_TYPE_NODE_PATH, 2, "godotString.rawPtr"))
                .endControlFlow()
                .build()

            else -> todoBody()
        }

        // String-type specific cases (rawPtr args only, no primitives)
        builtinClass.name == "String" -> when (ctor.index) {
            0 -> callBuiltinConstructorSimple(VARIANT_TYPE_STRING, 0)
            1, 2, 3 -> callBuiltinConstructorSimple(VARIANT_TYPE_STRING, ctor.index, "from.rawPtr")
            else -> todoBody()
        }

        builtinClass.name == "StringName" -> when (ctor.index) {
            0 -> callBuiltinConstructorSimple(VARIANT_TYPE_STRING_NAME, 0)
            1, 2 -> callBuiltinConstructorSimple(VARIANT_TYPE_STRING_NAME, ctor.index, "from.rawPtr")
            else -> todoBody()
        }

        builtinClass.name == "NodePath" -> when (ctor.index) {
            0 -> callBuiltinConstructorSimple(VARIANT_TYPE_NODE_PATH, 0)
            1, 2 -> callBuiltinConstructorSimple(VARIANT_TYPE_NODE_PATH, ctor.index, "from.rawPtr")
            else -> todoBody()
        }

        // Generic path for all other storage-backed builtins
        else -> callBuiltinConstructorGeneric(
            variantTypeConst(builtinClass.name),
            ctor.index,
            ctor.arguments,
        )
    }

    /**
     * Generic constructor emitter that resolves each arg's Kotlin type (with meta) to pick
     * the correct `*Var` stack allocation.
     *
     * Type mapping (matches [KotlinNativeTypeResolver.resolveWithMeta]):
     * - `FLOAT`  → `FloatVar`,  `alloc<FloatVar>().also { it.value = arg }`
     * - `DOUBLE` → `DoubleVar`, `alloc<DoubleVar>().also { it.value = arg }`
     * - `INT`    → `IntVar`,    `alloc<IntVar>().also { it.value = arg }`
     * - `LONG`   → `LongVar`,   `alloc<LongVar>().also { it.value = arg }`
     * - `BYTE`   → `ByteVar`    (similarly for Short, UByte, UShort, UInt, ULong)
     * - `BOOLEAN`→ `allocGdBool(arg)` (no reinterpret needed)
     * - everything else → assumed to be a builtin class with `rawPtr`
     */
    context(context: Context)
    private fun callBuiltinConstructorGeneric(
        variantType: String,
        constructorIndex: Int,
        args: List<MethodArg>,
    ): CodeBlock {
        val variantBinding = implPackageRegistry.classNameForOrDefault("VariantBinding")
        val allocConstTypePtrArray = implPackageRegistry.memberNameForOrDefault("allocConstTypePtrArray")
        val allocGdBool = implPackageRegistry.memberNameForOrDefault("allocGdBool")

        val block = CodeBlock.builder().beginControlFlow("%M", memScoped)

        // Resolve each arg's Kotlin type and emit stack-alloc if needed
        val ptrExprs = args.map { arg ->
            val kotlinName = safeIdentifier(arg.name)
            val varName = "${kotlinName}Var"
            val kotlinType = typeResolver.resolve(arg)

            when (kotlinType) {
                FLOAT -> {
                    block.addStatement(
                        "val $varName = %M<%T>().also·{ it.%M = $kotlinName }",
                        cinteropAlloc,
                        FLOAT_VAR,
                        cinteropValue,
                    )
                    CodeBlock.of("%N.%M.%M()", varName, cinteropPtr, cinteropReinterpret)
                }

                DOUBLE -> {
                    block.addStatement(
                        "val $varName = %M<%T>().also·{ it.%M = $kotlinName }",
                        cinteropAlloc,
                        DOUBLE_VAR,
                        cinteropValue,
                    )
                    CodeBlock.of("%N.%M.%M()", varName, cinteropPtr, cinteropReinterpret)
                }

                INT -> {
                    block.addStatement(
                        "val $varName = %M<%T>().also·{ it.%M = $kotlinName }",
                        cinteropAlloc,
                        INT_VAR,
                        cinteropValue,
                    )
                    CodeBlock.of("%N.%M.%M()", varName, cinteropPtr, cinteropReinterpret)
                }

                LONG -> {
                    block.addStatement(
                        "val $varName = %M<%T>().also·{ it.%M = $kotlinName }",
                        cinteropAlloc,
                        LONG_VAR,
                        cinteropValue,
                    )
                    CodeBlock.of("%N.%M.%M()", varName, cinteropPtr, cinteropReinterpret)
                }

                BYTE -> {
                    block.addStatement(
                        "val $varName = %M<%T>().also·{ it.%M = $kotlinName }",
                        cinteropAlloc,
                        BYTE_VAR,
                        cinteropValue,
                    )
                    CodeBlock.of("%N.%M.%M()", varName, cinteropPtr, cinteropReinterpret)
                }

                SHORT -> {
                    block.addStatement(
                        "val $varName = %M<%T>().also·{ it.%M = $kotlinName }",
                        cinteropAlloc,
                        SHORT_VAR,
                        cinteropValue,
                    )
                    CodeBlock.of("%N.%M.%M()", varName, cinteropPtr, cinteropReinterpret)
                }

                U_BYTE -> {
                    block.addStatement(
                        "val $varName = %M<%T>().also·{ it.%M = $kotlinName }",
                        cinteropAlloc,
                        U_BYTE_VAR,
                        cinteropValue,
                    )
                    CodeBlock.of("%N.%M.%M()", varName, cinteropPtr, cinteropReinterpret)
                }

                U_SHORT -> {
                    block.addStatement(
                        "val $varName = %M<%T>().also·{ it.%M = $kotlinName }",
                        cinteropAlloc,
                        U_SHORT_VAR,
                        cinteropValue,
                    )
                    CodeBlock.of("%N.%M.%M()", varName, cinteropPtr, cinteropReinterpret)
                }

                U_INT -> {
                    block.addStatement(
                        "val $varName = %M<%T>().also·{ it.%M = $kotlinName }",
                        cinteropAlloc,
                        U_INT_VAR,
                        cinteropValue,
                    )
                    CodeBlock.of("%N.%M.%M()", varName, cinteropPtr, cinteropReinterpret)
                }

                U_LONG -> {
                    block.addStatement(
                        "val $varName = %M<%T>().also·{ it.%M = $kotlinName }",
                        cinteropAlloc,
                        U_LONG_VAR,
                        cinteropValue,
                    )
                    CodeBlock.of("%N.%M.%M()", varName, cinteropPtr, cinteropReinterpret)
                }

                BOOLEAN -> {
                    block.addStatement(
                        "val $varName = %M($kotlinName)",
                        allocGdBool,
                    )
                    CodeBlock.of("%N", varName)
                }

                else -> CodeBlock.of("%N.rawPtr", kotlinName) // builtin class with rawPtr
            }
        }

        // Constructor lookup + invocation
        block.addStatement(
            "val constructor = %T.instance.getPtrConstructorRaw(%N, %L)",
            variantBinding,
            variantType,
            constructorIndex,
        )
            .indent()
            .addStatement("?: error(%S)", "Missing builtin constructor for $variantType[$constructorIndex]")
            .unindent()

        if (ptrExprs.isEmpty()) {
            block.addStatement("constructor.%M(rawPtr, %M())", cinteropInvoke, allocConstTypePtrArray)
        } else {
            block
                .addStatement("constructor.%M(", cinteropInvoke)
                .indent()
                .addStatement("rawPtr, ")
                .addStatement("%M(%L),", allocConstTypePtrArray, ptrExprs.joinToCode())
                .unindent()
                .addStatement(")")
        }

        return block.endControlFlow().build()
    }

    /**
     * Simple variant for constructors whose args are all pre-built pointer expressions
     * (e.g. `rawPtr` of another builtin). No stack allocs needed.
     */
    private fun callBuiltinConstructorSimple(
        variantType: String,
        constructorIndex: Int,
        vararg argExprs: String,
    ): CodeBlock = CodeBlock
        .builder()
        .beginControlFlow("%M", memScoped)
        .addStatement(
            "val constructor = %T.instance.getPtrConstructorRaw(%N, %L)",
            implPackageRegistry.classNameForOrDefault("VariantBinding"),
            variantType,
            constructorIndex,
        )
        .indent()
        .addStatement("?: error(%S)", "Missing builtin constructor")
        .unindent()
        .addStatement("constructor.%M(", cinteropInvoke)
        .indent()
        .addStatement("rawPtr, ")
        .addStatement(
            "%M(%L),",
            implPackageRegistry.memberNameForOrDefault("allocConstTypePtrArray"),
            argExprs.joinToString(),
        )
        .unindent()
        .addStatement(")")
        .endControlFlow()
        .build()

    private fun destroyBuiltin(variantType: String): CodeBlock = CodeBlock.builder()
        .addStatement(
            "val %N = %T.instance.getPtrDestructorRaw(%N)",
            "destructor",
            implPackageRegistry.classNameForOrDefault("VariantBinding"),
            variantType,
        )
        .indent()
        .addStatement("?: error(%S)", "Missing builtin destructor")
        .unindent()
        .addStatement("%N.%M(rawPtr)", "destructor", cinteropInvoke)
        .build()
}
