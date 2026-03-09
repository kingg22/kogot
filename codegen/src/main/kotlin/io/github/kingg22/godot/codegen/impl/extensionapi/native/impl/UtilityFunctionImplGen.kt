package io.github.kingg22.godot.codegen.impl.extensionapi.native.impl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.native.DOUBLE_VAR
import io.github.kingg22.godot.codegen.impl.extensionapi.native.FLOAT_VAR
import io.github.kingg22.godot.codegen.impl.extensionapi.native.INT_VAR
import io.github.kingg22.godot.codegen.impl.extensionapi.native.alloc
import io.github.kingg22.godot.codegen.impl.extensionapi.native.cinteropInvoke
import io.github.kingg22.godot.codegen.impl.extensionapi.native.cpointerValue
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.BodyGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.lazyMethod
import io.github.kingg22.godot.codegen.impl.extensionapi.native.memScoped
import io.github.kingg22.godot.codegen.impl.extensionapi.native.ptr
import io.github.kingg22.godot.codegen.impl.extensionapi.native.reinterpret
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction

private const val VARIANT_TYPE_STRING_NAME = "GDEXTENSION_VARIANT_TYPE_STRING_NAME"

/**
 * Generates lazy-loaded function pointer properties and actual invocation bodies
 * for Godot API utility functions (the `GD` object).
 *
 * Each utility function gets:
 * 1. A private `lazy(PUBLICATION)` property that loads the `GDExtensionPtrUtilityFunction`
 *    via `VariantBinding.instance.getPtrUtilityFunctionRaw`, using a temporary `StringName`
 *    that is properly allocated and destroyed after lookup.
 * 2. A function body that invokes the loaded pointer, packing arguments appropriately.
 *
 * ## Argument mapping
 * - Godot primitive `float` / `double` → `alloc<DoubleVar>()`, assign, pass `.ptr.reinterpret()`
 * - Godot `int`                        → `alloc<LongVar>()`, assign, pass `.ptr.reinterpret()`
 * - Godot `bool`                       → `allocGdBool(arg)`, pass directly
 * - Builtin class (has `rawPtr`)       → pass `arg.rawPtr` directly
 * - `Variant` / vararg `Variant`       → deferred (`TODO()`), requires Variant-encoding infrastructure
 *
 * ## Return type mapping
 * - `void` (null return type) → pass `null` as `r_return`
 * - `float` / `double`       → `alloc<DoubleVar>()`, read `.value` after invoke
 * - `int`                    → `alloc<LongVar>()`, read `.value` after invoke
 * - `bool`                   → `allocGdBool()`, read via `readGdBool()` after invoke
 * - Builtin / `Variant`      → deferred (`TODO()`)
 */
class UtilityFunctionImplGen(private val delegate: BodyGenerator) {
    private lateinit var implPackageRegistry: ImplementationPackageRegistry

    fun todoBody(): CodeBlock = delegate.todoBody()

    fun initialize(implRegistry: ImplementationPackageRegistry) {
        implPackageRegistry = implRegistry
    }

    // ── Property: lazy GDExtensionPtrUtilityFunction ──────────────────────────

    /**
     * Builds the private `lazy(PUBLICATION)` property that loads the utility function pointer.
     *
     * Generated code example for `print` (hash `2042093114`):
     * ```kotlin
     * private val printFn: GDExtensionPtrUtilityFunction by lazy(LazyThreadSafetyMode.PUBLICATION) {
     *     memScoped {
     *         val nameStorage = allocateBuiltinStorage(/* StringName size */)
     *         StringBinding.instance.nameNewWithUtf8Chars(nameStorage, "print")
     *         val fn = VariantBinding.instance.getPtrUtilityFunctionRaw(nameStorage, 2042093114L)
     *             ?: error("Missing utility function 'print'")
     *         VariantBinding.instance.getPtrDestructorRaw(GDEXTENSION_VARIANT_TYPE_STRING_NAME)
     *             ?.invoke(nameStorage)
     *         freeBuiltinStorage(nameStorage)
     *         fn
     *     }
     * }
     * ```
     */
    context(context: Context)
    fun buildFunctionPointerProperty(fn: UtilityFunction): PropertySpec {
        val stringNameSize = context.findResolvedBuiltinClass("StringName")?.layout?.size
            ?: error("StringName layout size not found — cannot generate pointer for '${fn.name}'")

        // rootPackage = "…internal.binding" → ffiPackage = "…internal.ffi"
        val ffiPackage = implPackageRegistry.rootPackage.substringBeforeLast(".") + ".ffi"
        val ptrUtilityFunctionType = ClassName(ffiPackage, "GDExtensionPtrUtilityFunction")

        val variantBindingClass = implPackageRegistry.classNameForOrDefault("VariantBinding")
        val stringBindingClass = implPackageRegistry.classNameForOrDefault("StringBinding")
        val allocBuiltinStorage = implPackageRegistry.memberNameForOrDefault("allocateBuiltinStorage")
        val freeBuiltinStorage = implPackageRegistry.memberNameForOrDefault("freeBuiltinStorage")

        val lazyBlock = CodeBlock.builder()
            .beginControlFlow("%M(PUBLICATION)", lazyMethod)
            .beginControlFlow("%M", memScoped)
            // allocate a temporary StringName for the lookup
            .addStatement("val nameStorage = %M(%L)", allocBuiltinStorage, stringNameSize)
            .addStatement(
                "%T.instance.nameNewWithUtf8Chars(nameStorage, %S)",
                stringBindingClass,
                fn.name,
            )
            // look up the function pointer by name + hash
            .addStatement(
                "val fn = %T.instance.getPtrUtilityFunctionRaw(nameStorage, %LL)",
                variantBindingClass,
                fn.hash,
            )
            .indent()
            .addStatement("?: error(%S)", "Missing utility function '${fn.name}'")
            .unindent()
            // destroy the temporary StringName
            .addStatement(
                "%T.instance.getPtrDestructorRaw(%N)?.%M(nameStorage)",
                variantBindingClass,
                VARIANT_TYPE_STRING_NAME,
                cinteropInvoke,
            )
            .addStatement("%M(nameStorage)", freeBuiltinStorage)
            .addStatement("fn")
            .endControlFlow()
            .endControlFlow()
            .build()

        return PropertySpec
            .builder(functionPointerName(fn), ptrUtilityFunctionType, KModifier.PRIVATE)
            .delegate(lazyBlock)
            .build()
    }

    // ── Body: invocation of the loaded pointer ────────────────────────────────

    /**
     * Builds the function body for a utility function.
     *
     * Dispatches based on the function's argument types and vararg status.
     * Falls back to [todoBody] for cases that require Variant-encoding infrastructure
     * (vararg Variant, Variant return, etc.).
     */
    context(context: Context)
    fun buildFunctionBody(fn: UtilityFunction): CodeBlock {
        val allocConstTypePtrArray = implPackageRegistry.memberNameForOrDefault("allocConstTypePtrArray")
        val propName = functionPointerName(fn)

        // ── Vararg functions ──────────────────────────────────────────────────
        // All vararg Variant calls need Variant encoding infrastructure.
        // Deferred until the Variant marshalling layer is implemented.
        if (fn.isVararg) {
            return buildVarargBody(fn, propName, allocConstTypePtrArray)
        }

        // ── Fixed-arg functions ───────────────────────────────────────────────
        return buildFixedArgsBody(fn, propName, allocConstTypePtrArray)
    }

    // ── Vararg dispatch ───────────────────────────────────────────────────────
    private fun buildVarargBody(fn: UtilityFunction, propName: String, allocConstTypePtrArray: MemberName): CodeBlock {
        val fixedArgs = fn.arguments
        val hasReturn = fn.returnType != null

        // If there are no fixed args and no return: pure vararg void (e.g. print, print_rich).
        // The vararg args are all Variant, which still needs encoding — TODO for now.
        if (fixedArgs.isEmpty() && !hasReturn) {
            return buildPureVarargVoidTodo(propName, fn.name, allocConstTypePtrArray)
        }

        // Mixed fixed + vararg, or vararg with return: always deferred.
        return delegate.todoBody()
    }

    /**
     * Generates the skeletal body for a pure vararg-void utility (like `print`).
     *
     * The body is a correct structural template — the spread over `args.map { it.rawPtr }`
     * will compile once `Variant` (the sealed class) exposes a `rawPtr` property pointing
     * to the underlying Godot Variant memory block.
     *
     * Generated:
     * ```kotlin
     * memScoped {
     *     val argPtrs = args.map { it.rawPtr }.toTypedArray()
     *     printFn.invoke(null, if (argPtrs.isEmpty()) null else allocConstTypePtrArray(*argPtrs), args.size)
     * }
     * ```
     */
    private fun buildPureVarargVoidTodo(
        propName: String,
        fnGodotName: String,
        allocConstTypePtrArray: MemberName,
    ): CodeBlock = CodeBlock.builder()
        .addStatement("// TODO: requires Variant.rawPtr — encode each Kotlin Variant to a Godot Variant memory block")
        .beginControlFlow("%M", memScoped)
        .addStatement("val argPtrs = args.map { it.rawPtr }.toTypedArray()")
        .addStatement(
            "%N.%M(null, if (argPtrs.isEmpty()) null else %M(*argPtrs), args.size)",
            propName,
            cinteropInvoke,
            allocConstTypePtrArray,
        )
        .endControlFlow()
        .build()

    // ── Fixed-args dispatch ───────────────────────────────────────────────────

    context(ctx: Context)
    private fun buildFixedArgsBody(
        fn: UtilityFunction,
        propName: String,
        allocConstTypePtrArray: MemberName,
    ): CodeBlock {
        // Check if every fixed arg can be marshalled without Variant encoding
        if (!fn.arguments.all { isMarshallable(it) }) return delegate.todoBody()

        // Check if the return type can be marshalled without Variant encoding
        val returnType = fn.returnType
        if (returnType != null && !isReturnMarshallable(returnType)) return delegate.todoBody()

        return CodeBlock
            .builder()
            .apply {
                if (returnType != null) {
                    add("return ")
                }
            }
            .beginControlFlow("%M", memScoped)
            .apply {
                // --- Allocate return buffer (if needed) ---
                if (returnType != null) {
                    add(buildReturnAlloc(returnType))
                }
                // --- Allocate primitive arg buffers ---
                fn.arguments.forEach { arg ->
                    add(buildArgAlloc(arg))
                }
                // --- Invoke ---
                val argExpressions = fn.arguments.joinToString(", ") { arg ->
                    argPointerExpression(arg)
                }
                val retExpression = if (returnType != null) {
                    if (returnType != "bool") {
                        CodeBlock.of("retPtr.%M.%M()", ptr, reinterpret)
                    } else {
                        CodeBlock.of("retPtr")
                    }
                } else {
                    CodeBlock.of("null")
                }
                if (fn.arguments.isEmpty()) {
                    addStatement("%N.%M(%L, null, 0)", propName, cinteropInvoke, retExpression)
                } else {
                    addStatement(
                        "%N.%M(%L, %M(%L), %L)",
                        propName,
                        cinteropInvoke,
                        retExpression,
                        allocConstTypePtrArray,
                        argExpressions,
                        fn.arguments.size,
                    )
                }
                // --- Return value ---
                if (returnType != null) {
                    add(buildReturnRead(returnType))
                }
            }
            .endControlFlow()
            .build()
    }

    // ── Marshallability checks ────────────────────────────────────────────────

    context(ctx: Context)
    private fun isMarshallable(arg: MethodArg): Boolean = when {
        arg.type == "Variant" -> false

        arg.type.startsWith("enum::") || arg.type.startsWith("bitfield::") -> false

        isGodotPrimitive(arg.type) -> true

        // float, int, bool
        ctx.isBuiltin(arg.type) -> true

        // builtin classes with rawPtr
        else -> false
    }

    context(ctx: Context)
    private fun isReturnMarshallable(returnType: String): Boolean = when {
        returnType == "Variant" -> false

        returnType.startsWith("enum::") || returnType.startsWith("bitfield::") -> false

        isGodotPrimitive(returnType) -> true

        ctx.isBuiltin(returnType) -> false

        // deferred: needs factory/constructor
        else -> false
    }

    private fun isGodotPrimitive(type: String): Boolean = type == "float" || type == "int" || type == "bool"

    // ── Argument allocation (for primitives that need stack vars) ────────────
    private fun buildArgAlloc(arg: MethodArg): CodeBlock {
        val name = safeIdentifier(arg.name)
        return when (arg.type) {
            "float" -> CodeBlock.ofStatement(
                "val %LVar = %M<%T>().also { it.%M = %N }",
                name,
                alloc,
                FLOAT_VAR,
                cpointerValue,
                name,
            )

            "double" -> CodeBlock.ofStatement(
                "val %LVar = %M<%T>().also { it.%M = %N }",
                name,
                alloc,
                DOUBLE_VAR,
                cpointerValue,
                name,
            )

            "int" -> CodeBlock.ofStatement(
                "val %LVar = %M<%T>().also { it.%M = %N }",
                name,
                alloc,
                INT_VAR,
                cpointerValue,
                name,
            )

            "bool" -> CodeBlock.ofStatement(
                "val %LVar = %M(%N)",
                name,
                implPackageRegistry.memberNameForOrDefault("allocGdBool"),
                name,
            )

            else -> CodeBlock.of("") // builtin: use rawPtr directly, no alloc needed
        }
    }

    // ── Argument pointer expression ───────────────────────────────────────────
    context(ctx: Context)
    private fun argPointerExpression(arg: MethodArg): String {
        val name = safeIdentifier(arg.name)
        return when {
            isGodotPrimitive(arg.type) -> "${name}Var.ptr.reinterpret()"
            ctx.isBuiltin(arg.type) -> "$name.rawPtr"
            else -> error("Unexpected arg type in marshallable check: ${arg.type}")
        }
    }

    // ── Return buffer allocation ──────────────────────────────────────────────
    private fun buildReturnAlloc(returnType: String): CodeBlock = when (returnType) {
        "float" -> CodeBlock.ofStatement("val retPtr = %M<%T>()", alloc, FLOAT_VAR)

        "double" -> CodeBlock.ofStatement("val retPtr = %M<%T>()", alloc, DOUBLE_VAR)

        "int" -> CodeBlock.ofStatement("val retPtr = %M<%T>()", alloc, INT_VAR)

        "bool" -> CodeBlock.ofStatement(
            "val retPtr = %M()",
            implPackageRegistry.memberNameForOrDefault("allocGdBool"),
        )

        else -> CodeBlock.of("") // should not reach here if isReturnMarshallable is correct
    }

    // ── Return value read ─────────────────────────────────────────────────────
    private fun buildReturnRead(returnType: String): CodeBlock = when (returnType) {
        "float", "double", "int" -> CodeBlock.ofStatement("retPtr.%M", cpointerValue)
        "bool" -> CodeBlock.ofStatement("retPtr.%M()", implPackageRegistry.memberNameForOrDefault("readGdBool"))
        else -> CodeBlock.ofStatement("")
    }

    private fun CodeBlock.Companion.ofStatement(format: String, vararg args: Any?) =
        CodeBlock.builder().addStatement(format, *args).build()

    companion object {
        /** Derives the lazy property name for [fn]'s function pointer. */
        private fun functionPointerName(fn: UtilityFunction): String =
            "${safeIdentifier(fn.name).removeSurrounding("`")}Fn"
    }
}
