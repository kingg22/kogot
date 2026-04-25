package io.github.kingg22.godot.codegen.extensionapi.impl.knative.impl

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.joinToCode
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction
import io.github.kingg22.godot.codegen.types.*

/**
 * Generates lazy-loaded function pointer properties and actual invocation bodies
 * for Godot API utility functions (the `GD` object).
 *
 * Each utility function gets:
 * 1. A private `lazy(PUBLICATION)` property that loads the `GDExtensionPtrUtilityFunction`
 *    via `VariantBinding.instance.getPtrUtilityFunctionRaw`, using a temporary `StringName`.
 * 2. A function body that invokes the loaded pointer, packing arguments appropriately.
 *
 * ## Argument mapping
 * - Godot primitive `float` / `double` → `alloc<DoubleVar>()`, assign, pass `.ptr.reinterpret()`
 * - Godot `int`                        → `alloc<LongVar>()`, assign, pass `.ptr.reinterpret()`
 * - Godot `bool`                       → `allocGdBool(arg)`, pass directly
 * - Builtin class (has `rawPtr`)       → pass `arg.rawPtr` directly
 *
 * ## Return type mapping
 * - `void` (null return type) → pass `null` as `r_return`
 * - `float` / `double`       → `alloc<DoubleVar>()`, read `.value` after invoke
 * - `int`                    → `alloc<LongVar>()`, read `.value` after invoke
 * - `bool`                   → `allocGdBool()`, read via `readGdBool()` after invoke
 */
class UtilityFunctionImplGen {
    private lateinit var implPackageRegistry: ImplementationPackageRegistry

    fun initialize(implRegistry: ImplementationPackageRegistry) {
        implPackageRegistry = implRegistry
    }

    // ── Property: lazy GDExtensionPtrUtilityFunction ──────────────────────────

    context(context: Context)
    fun buildFunctionPointerProperty(fn: UtilityFunction): PropertySpec {
        val ptrUtilityFunctionType = implPackageRegistry.classNameForOrDefault("GDExtensionPtrUtilityFunction")
        val variantBindingClass = implPackageRegistry.classNameForOrDefault("VariantBinding")
        val stringNameClass = context.classNameForOrDefault("StringName")

        val bodyCode = CodeBlock
            .builder()
            .beginControlFlow("%T(%S).use { name ->", stringNameClass, fn.name)
            .addStatement("%T.instance", variantBindingClass)
            .indent()
            .addStatement(".getPtrUtilityFunctionRaw(name.rawPtr, %LL)", fn.hash)
            .addStatement("?: error(%S)", "Missing utility function '${fn.name}'")
            .unindent()
            .endControlFlow()
            .build()

        return PropertySpec
            .builder(functionPointerName(fn), ptrUtilityFunctionType, KModifier.PRIVATE)
            .delegate(
                CodeBlock
                    .builder()
                    .beginControlFlow("%M(PUBLICATION)", lazyMethod)
                    .add(bodyCode)
                    .endControlFlow()
                    .build(),
            )
            .build()
    }

    // ── Body Implementation ───────────────────────────────────────────────────

    context(context: Context)
    fun buildFunctionBody(fn: UtilityFunction): CodeBlock {
        val propName = functionPointerName(fn)
        if (fn.isVararg) return buildVarargBody(fn, propName)
        return buildFixedArgsBody(fn, propName)
    }

    context(context: Context)
    private fun buildVarargBody(fn: UtilityFunction, propName: String): CodeBlock = CodeBlock.builder().apply {
        if (fn.returnType != null) add("return ")

        beginControlFlow("%M", memScoped)

        // Map fixed arguments and varargs into a single list of Variants
        // Note: Vararg utilities in Godot usually take an array of Variant pointers
        addStatement("val args = listOf(")
        indent()
        fn.arguments.forEach { arg ->
            addStatement("%N,", safeIdentifier(arg.name))
        }
        addStatement("*args")
        unindent()
        addStatement(").map { it.rawPtr }")

        addStatement(
            "val argsPtr = %M(*args.toTypedArray())",
            implPackageRegistry.memberNameForOrDefault("allocConstTypePtrArray"),
        )

        if (fn.returnType != null) {
            addStatement("val ret = %T()", context.classNameForOrDefault(fn.returnType!!.renameGodotClass()))
            addStatement("%N.%M(ret.rawPtr, argsPtr, args.size)", propName, cinteropInvoke)
            addStatement("return ret")
        } else {
            addStatement("%N.%M(null, argsPtr, args.size)", propName, cinteropInvoke)
        }

        endControlFlow()
    }.build()

    context(ctx: Context)
    private fun buildFixedArgsBody(fn: UtilityFunction, propName: String): CodeBlock = CodeBlock.builder().apply {
        val returnType = fn.returnType

        if (returnType != null) add("return ")

        beginControlFlow("%M", memScoped)

        // 1. Alloc return buffer
        if (returnType != null) add(buildReturnAlloc(returnType))

        // 2. Alloc arguments
        fn.arguments.forEach { arg -> add(buildArgAlloc(arg)) }

        // 3. Invoke logic
        val argExpressions = fn.arguments.joinToCode("") { arg -> argPointerExpression(arg) }
        val retExpression = when {
            returnType == null -> CodeBlock.of("null")

            returnType == "bool" -> CodeBlock.of("retPtr")

            isGodotPrimitive(returnType) -> CodeBlock.of("retPtr.%M", cinteropPtr)

            ctx.isBuiltin(returnType) -> CodeBlock.of("retPtr.rawPtr")

            ctx.findEngineClass(returnType) != null ->
                CodeBlock.of("retPtr.%M.%M()", cinteropPtr, cinteropReinterpret)

            else -> CodeBlock.of("null")
        }

        if (fn.arguments.isEmpty()) {
            addStatement("%N.%M(%L, null, 0)", propName, cinteropInvoke, retExpression)
        } else {
            addStatement("%N.%M(", propName, cinteropInvoke)
            indent()
            addStatement("%L,", retExpression)
            addStatement("%M(", implPackageRegistry.memberNameForOrDefault("allocConstTypePtrArray"))
            indent()
            add(argExpressions)
            unindent()
            addStatement("),")
            addStatement("%L,", fn.arguments.size)
            unindent()
            addStatement(")")
        }

        // 4. Read return
        if (returnType != null) {
            add(buildReturnRead(returnType))
        }

        endControlFlow()
    }.build()

    private fun isGodotPrimitive(type: String): Boolean =
        type == "float" || type == "int" || type == "bool" || type == "double"

    private fun buildArgAlloc(arg: MethodArg): CodeBlock = CodeBlock.builder().apply {
        val name = safeIdentifier(arg.name)

        when (arg.type) {
            "float", "double", "int" -> {
                val cType = when (arg.type) {
                    "float", "double" -> DOUBLE_VAR
                    "int" -> LONG_VAR
                    else -> error("Invalid type: ${arg.type}")
                }
                addStatement("val %LVar = %M<%T>()", name, cinteropAlloc, cType)
                addStatement("%LVar.%M = %N", name, cinteropValue, name)
            }

            "bool" -> {
                addStatement(
                    "val %LVar = %M(%N)",
                    name,
                    implPackageRegistry.memberNameForOrDefault("allocGdBool"),
                    name,
                )
            }
        }
    }.build()

    context(ctx: Context)
    private fun argPointerExpression(arg: MethodArg): CodeBlock {
        val name = safeIdentifier(arg.name)
        return when {
            isGodotPrimitive(arg.type) -> CodeBlock.builder().addStatement("%LVar.%M,", name, cinteropPtr).build()

            ctx.isBuiltin(arg.type) || arg.type == "Variant" -> CodeBlock.builder()
                .addStatement("%N.rawPtr,", name).build()

            arg.type.startsWith("enum::") -> CodeBlock.builder().addStatement(
                "%M<%T>().also♢{ it.%M = %N.value }.%M,",
                cinteropAlloc,
                LONG_VAR,
                cinteropValue,
                name,
                cinteropPtr,
            ).build()

            else -> error("Invalid type: ${arg.type}")
        }
    }

    context(ctx: Context)
    private fun buildReturnAlloc(returnType: String): CodeBlock = CodeBlock.builder().apply {
        when {
            returnType == "float" || returnType == "double" -> addStatement(
                "val retPtr = %M<%T>()",
                cinteropAlloc,
                DOUBLE_VAR,
            )

            returnType == "int" -> addStatement("val retPtr = %M<%T>()", cinteropAlloc, LONG_VAR)

            returnType == "bool" -> addStatement(
                "val retPtr = %M()",
                implPackageRegistry.memberNameForOrDefault("allocGdBool"),
            )

            ctx.isBuiltin(returnType) -> addStatement(
                "val retPtr = %T()",
                ctx.classNameForOrDefault(returnType.renameGodotClass()),
            )

            ctx.findEngineClass(returnType) != null -> addStatement(
                "val retPtr = %M<%T>()",
                cinteropAlloc,
                C_OPAQUE_POINTER_VAR,
            )

            else -> error("Invalid return type, unknown strategy: '$returnType'")
        }
    }.build()

    context(ctx: Context)
    private fun buildReturnRead(returnType: String): CodeBlock = when {
        isGodotPrimitive(returnType) && returnType != "bool" ->
            CodeBlock.builder().addStatement("retPtr.%M", cinteropValue).build()

        returnType == "bool" -> CodeBlock.builder().addStatement(
            "retPtr.%M()",
            implPackageRegistry.memberNameForOrDefault("readGdBool"),
        ).build()

        ctx.findEngineClass(returnType) != null ->
            CodeBlock
                .builder()
                .addStatement("retPtr.value?.let { %T(it) }", ctx.classNameForOrDefault(returnType.renameGodotClass()))
                .build()

        else -> CodeBlock.builder().addStatement("retPtr").build()
    }

    private fun functionPointerName(fn: UtilityFunction) = safeIdentifier(fn.name) + "Fn"
}
