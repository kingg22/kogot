package io.github.kingg22.godot.codegen.extensionapi.impl.knative.impl

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.withIndent
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.NativeBuiltinClassGenerator
import io.github.kingg22.godot.codegen.impl.renameAllUpperCaseToCamelCase
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.impl.toScreamingSnakeCase
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinClass
import io.github.kingg22.godot.codegen.types.*

/**
 * Generates lazy-loaded fptr properties and invocation bodies for builtin class methods.
 *
 * Mirrors [UtilityFunctionImplGen] but targets `GDExtensionPtrBuiltInMethod`:
 *   `(p_base, p_args, r_return, p_argument_count) → Unit`
 *
 * - **Static methods**: `p_base = null`
 * - **Instance methods**: `p_base = rawPtr`
 *
 * The fptr is loaded via `VariantBinding.instance.getPtrBuiltinMethodRaw(variantType, name, hash)`.
 * Properties are emitted as top-level `private val` lazy delegates in the class file.
 */
class BuiltinMethodImplGen(private val typeResolver: TypeResolver) {
    private lateinit var implPackageRegistry: ImplementationPackageRegistry

    fun initialize(implRegistry: ImplementationPackageRegistry) {
        implPackageRegistry = implRegistry
    }

    // ── Top-level lazy fptr property ──────────────────────────────────────────

    context(context: Context)
    fun buildMethodFptrProperty(
        method: BuiltinClass.BuiltinMethod,
        variantType: String,
        className: String,
    ): PropertySpec {
        val ptrType = implPackageRegistry.classNameForOrDefault("GDExtensionPtrBuiltInMethod")
        val variantBinding = implPackageRegistry.classNameForOrDefault("VariantBinding")
        val stringNameClass = context.classNameForOrDefault("StringName")

        val body = buildLazyBlock {
            beginControlFlow("%T(%S).use { name ->", stringNameClass, method.name)
                .addStatement(
                    "%T.instance.getPtrBuiltinMethodRaw(%N, name.rawPtr, %LL)",
                    variantBinding,
                    variantType,
                    method.hash,
                )
                .withIndent {
                    addStatement(
                        "?: error(%S)",
                        "Missing builtin method '$className.${method.name}' hash: ${method.hash}",
                    )
                }
            endControlFlow()
        }

        return PropertySpec
            .builder(methodFptrName(className, method), ptrType, KModifier.PRIVATE)
            .delegate(body)
            .build()
    }

    // ── Method body ───────────────────────────────────────────────────────────

    context(context: Context)
    fun buildMethodBody(method: BuiltinClass.BuiltinMethod, className: String): CodeBlock =
        buildFixedArgsBody(method, methodFptrName(className, method))

    // ── Operator bodies & fptr naming ────────────────────────────────────────

    /**
     * Stable name for the top-level lazy fptr property of a Godot operator evaluator.
     * Format: `operatorFptr_<OP>_<RIGHT>` — e.g. `operatorFptr_ADD_Vector2`, `operatorFptr_NEGATE_NIL`.
     */
    fun operatorFptrName(op: BuiltinClass.Operator): String {
        val opPart = when (op.name) {
            "==" -> "EQUAL"
            "!=" -> "NOT_EQUAL"
            "<" -> "LESS"
            "<=" -> "LESS_EQUAL"
            ">" -> "GREATER"
            ">=" -> "GREATER_EQUAL"
            "+" -> "ADD"
            "-" -> "SUBTRACT"
            "*" -> "MULTIPLY"
            "/" -> "DIVIDE"
            "%" -> "MODULE"
            "unary-" -> "NEGATE"
            "unary+" -> "POSITIVE"
            "not" -> "NOT"
            "in" -> "IN"
            else -> safeIdentifier(op.name)
        }
        val rightPart = op.rightType?.let { safeIdentifier(it) } ?: "NIL"
        return "operatorFptr_${opPart}_$rightPart"
    }

    fun godotOpToVariantOp(symbol: String): String? = when (symbol) {
        "==" -> "GDEXTENSION_VARIANT_OP_EQUAL"
        "<" -> "GDEXTENSION_VARIANT_OP_LESS"
        "<=" -> "GDEXTENSION_VARIANT_OP_LESS_EQUAL"
        ">" -> "GDEXTENSION_VARIANT_OP_GREATER"
        ">=" -> "GDEXTENSION_VARIANT_OP_GREATER_EQUAL"
        "+" -> "GDEXTENSION_VARIANT_OP_ADD"
        "-" -> "GDEXTENSION_VARIANT_OP_SUBTRACT"
        "*" -> "GDEXTENSION_VARIANT_OP_MULTIPLY"
        "/" -> "GDEXTENSION_VARIANT_OP_DIVIDE"
        "%" -> "GDEXTENSION_VARIANT_OP_MODULE"
        "unary-" -> "GDEXTENSION_VARIANT_OP_NEGATE"
        "unary+" -> "GDEXTENSION_VARIANT_OP_POSITIVE"
        "not" -> "GDEXTENSION_VARIANT_OP_NOT"
        "in" -> "GDEXTENSION_VARIANT_OP_IN"
        else -> null
    }

    /**
     * Builds the invocation body for a Godot operator evaluator.
     *
     * GDExtension evaluator signature: `(p_left, p_right, r_return) → Unit`
     *
     * - **equals**: emits `if (other !is T) return false` before `memScoped` (Kotlin's `Any?` signature).
     * - **Unary** (`rightType == null`): passes `null` for `p_right`.
     * - **Primitive right** (`int`, `float`, `bool`): stack-allocates a `*Var` inside `memScoped`.
     * - **Builtin right**: passes `other.rawPtr`.
     *
     * `memScoped` is `inline`, so `return` inside the block returns from the enclosing Kotlin fun.
     */
    context(ctx: Context)
    fun buildOperatorBody(op: BuiltinClass.Operator): CodeBlock = buildCodeBlock {
        beginControlFlow("return %M", memScoped)

        // ── r_return allocation ───────────────────────────────────────────
        when {
            op.returnType == "bool" -> addStatement(
                "val retPtr = %M()",
                implPackageRegistry.memberNameForOrDefault("allocGdBool"),
            )

            op.returnType == "float" || op.returnType == "double" -> addStatement(
                "val retPtr = %M<%T>()",
                cinteropAlloc,
                DOUBLE_VAR,
            )

            op.returnType == "int" -> addStatement("val retPtr = %M<%T>()", cinteropAlloc, LONG_VAR)

            ctx.isBuiltin(op.returnType) -> addStatement(
                "val retPtr = %T()",
                ctx.classNameForOrDefault(op.returnType.renameGodotClass()),
            )

            else -> addStatement("val retPtr = %M<%T>()", cinteropAlloc, C_OPAQUE_POINTER_VAR)
        }

        // ── p_right allocation & expression ──────────────────────────────
        // pRightExpr: the literal to pass as p_right in the invoke call.
        // Sentinel "otherVar.PTR" means we need cinteropPtr substitution in invoke.
        val pRightNeedsPtr: Boolean
        val pRightLiteral: String
        when (op.rightType) {
            null -> {
                pRightNeedsPtr = false
                pRightLiteral = "null"
            }

            "float", "double" -> {
                addStatement("val otherVar = %M<%T>()", cinteropAlloc, DOUBLE_VAR)
                addStatement("otherVar.%M = other", cinteropValue)
                pRightNeedsPtr = true
                pRightLiteral = "otherVar"
            }

            "int" -> {
                addStatement("val otherVar = %M<%T>()", cinteropAlloc, LONG_VAR)
                addStatement("otherVar.%M = other", cinteropValue)
                pRightNeedsPtr = true
                pRightLiteral = "otherVar"
            }

            "bool" -> {
                addStatement(
                    "val otherVar = %M(other)",
                    implPackageRegistry.memberNameForOrDefault("allocGdBool"),
                )
                pRightNeedsPtr = false
                pRightLiteral = "otherVar"
            }

            else -> {
                pRightNeedsPtr = false
                pRightLiteral = "other.rawPtr"
            }
        }

        // ── r_return expression ───────────────────────────────────────────
        val rReturnNeedsPtr: Boolean
        val rReturnLiteral: String

        when {
            op.returnType == "bool" -> {
                rReturnNeedsPtr = false
                rReturnLiteral = "retPtr"
            }

            op.returnType == "float" || op.returnType == "double" || op.returnType == "int" -> {
                rReturnNeedsPtr = true
                rReturnLiteral = "retPtr"
            }

            ctx.isBuiltin(op.returnType) -> {
                rReturnNeedsPtr = false
                rReturnLiteral = "retPtr.rawPtr"
            }

            else -> {
                rReturnNeedsPtr = true
                rReturnLiteral = "retPtr"
            }
        }

        val fptrName = operatorFptrName(op)

        // ── invoke ────────────────────────────────────────────────────────
        when {
            pRightNeedsPtr && rReturnNeedsPtr -> addStatement(
                "%N.%M(rawPtr, %L.%M, %L.%M)",
                fptrName,
                cinteropInvoke,
                pRightLiteral,
                cinteropPtr,
                rReturnLiteral,
                cinteropPtr,
            )

            pRightNeedsPtr && !rReturnNeedsPtr -> addStatement(
                "%N.%M(rawPtr, %L.%M, %L)",
                fptrName,
                cinteropInvoke,
                pRightLiteral,
                cinteropPtr,
                rReturnLiteral,
            )

            !pRightNeedsPtr && rReturnNeedsPtr -> addStatement(
                "%N.%M(rawPtr, %L, %L.%M)",
                fptrName,
                cinteropInvoke,
                pRightLiteral,
                rReturnLiteral,
                cinteropPtr,
            )

            else -> addStatement(
                "%N.%M(rawPtr, %L, %L)",
                fptrName,
                cinteropInvoke,
                pRightLiteral,
                rReturnLiteral,
            )
        }

        // ── return ────────────────────────────────────────────────────────
        when {
            op.returnType == "bool" -> addStatement(
                "return retPtr.%M()",
                implPackageRegistry.memberNameForOrDefault("readGdBool"),
            )

            op.returnType == "float" || op.returnType == "double" || op.returnType == "int" -> addStatement(
                "return retPtr.%M",
                cinteropValue,
            )

            ctx.isBuiltin(op.returnType) -> addStatement("return retPtr")

            else -> addStatement("return retPtr.%M", cinteropValue)
        }

        endControlFlow()
    }

    context(ctx: Context)
    fun buildEqualsOperatorBody(resolvedClass: ResolvedBuiltinClass): CodeBlock = buildCodeBlock {
        val equalsOps = resolvedClass.raw.operators.filter { it.name == "==" }
        require(equalsOps.isNotEmpty()) { "No equals operators found for ${resolvedClass.name}" }

        val concreteOps = equalsOps.filter { it.rightType != null && it.rightType != "Variant" }
        val hasVariant = equalsOps.any { it.rightType == "Variant" }

        // ── fast paths ───────────────────────────────────────────────────────
        addStatement("// Fast paths")
        addStatement("if (this === other) return true")
        addStatement("if (other == null) return false")
        add("\n")

        // ── vars ─────────────────────────────────────────────────────────────
        addStatement("val ftptr: %T", implPackageRegistry.classNameForOrDefault("GDExtensionPtrOperatorEvaluator"))
        addStatement("val rhsPtr: %T", COPAQUE_POINTER)
        addStatement("")

        // ── when(other) ──────────────────────────────────────────────────────
        beginControlFlow("when (other)")

        // ── concrete types ───────────────────────────────────────────────────
        for (op in concreteOps) {
            val rightType = op.rightType!!
            val kotlinType = when {
                rightType.equals("Array", true) ->
                    ctx.classNameForOrDefault("Array", typedClass = true).parameterizedBy(STAR)

                rightType.equals("Dictionary", true) ->
                    ctx.classNameForOrDefault("Dictionary", typedClass = true).parameterizedBy(STAR, STAR)

                else -> ctx.classNameForOrDefault(rightType.renameGodotClass())
            }

            val fptrName = operatorFptrName(op)

            beginControlFlow("is %T ->", kotlinType)
                .addStatement("ftptr = %N", fptrName)
                .addStatement("rhsPtr = other.rawPtr")
            endControlFlow()
        }

        // ── Variant branch ───────────────────────────────────────────────────
        if (hasVariant) {
            val variantTypeName = ctx.classNameForOrDefault("Variant")
            beginControlFlow("is %T ->", variantTypeName)
                .addStatement("val type = other.getType()")
            beginControlFlow("when (type)")

            for (op in concreteOps) {
                val rightType = op.rightType!!
                val variantType = rightType.toScreamingSnakeCase()
                val fptrName = operatorFptrName(op)

                beginControlFlow("%T.Type.%L ->", variantTypeName, variantType)
                    // FIXME here can lead memory leaks
                    .addStatement("val tmp = other.as%L()", rightType.renameAllUpperCaseToCamelCase())
                    .addStatement("ftptr = %N", fptrName)
                    .addStatement("rhsPtr = tmp.rawPtr")
                endControlFlow()
            }

            addStatement("else -> return false")
            endControlFlow() // when(type)

            endControlFlow() // is Variant
        }

        // ── fallback ─────────────────────────────────────────────────────────
        addStatement("else -> return false")

        endControlFlow() // when(other)

        add("\n")

        // ── invoke ───────────────────────────────────────────────────────────
        beginControlFlow("%M", memScoped)
            .addStatement("val retPtr = %M()", implPackageRegistry.memberNameForOrDefault("allocGdBool"))
            .addStatement("ftptr.%M(rawPtr, rhsPtr, retPtr)", cinteropInvoke)
            .addStatement("return retPtr.%M()", implPackageRegistry.memberNameForOrDefault("readGdBool"))
        endControlFlow()
    }

    /**
     * Builds the `compareTo(other: T): Int` body using only the `<` (LESS) fptr.
     *
     * Invokes the same LESS evaluator twice — once normal, once with args swapped — to
     * determine ordering without loading all four comparison fptrs. `memScoped` is `inline`,
     * so `return -1` / `return 1` return directly from the enclosing `compareTo` function.
     *
     * ```kotlin
     * memScoped {
     *     val retPtr = allocGdBool()
     *     operatorFptr_LESS_T.invoke(rawPtr, other.rawPtr, retPtr)   // this < other?
     *     if (retPtr.readGdBool()) return -1
     *     operatorFptr_LESS_T.invoke(other.rawPtr, rawPtr, retPtr)   // other < this?
     *     if (retPtr.readGdBool()) return 1
     *     0
     * }
     * ```
     */
    context(_: Context)
    fun buildCompareToBody(builtinClass: ResolvedBuiltinClass): CodeBlock {
        // Prefer homogeneous same-type < operator; fall back to any < operator
        val lessOp = builtinClass.raw.operators.firstOrNull {
            it.name == "<" && it.rightType == builtinClass.name
        } ?: builtinClass.raw.operators.firstOrNull { it.name == "<" }
            ?: error("No '<' operator found for ${builtinClass.name} — cannot generate compareTo body")

        val fptrName = operatorFptrName(lessOp)
        requireNotNull(lessOp.rightType) { "Expected '<' operator with right type for ${builtinClass.name}" }

        return buildCodeBlock {
            beginControlFlow("return %M", memScoped)
                .addStatement("val retPtr = %M()", implPackageRegistry.memberNameForOrDefault("allocGdBool"))
                .addStatement("%N.%M(rawPtr, other.rawPtr, retPtr)", fptrName, cinteropInvoke)
                .addStatement("if (retPtr.%M()) return -1", implPackageRegistry.memberNameForOrDefault("readGdBool"))
                .addStatement("%N.%M(other.rawPtr, rawPtr, retPtr)", fptrName, cinteropInvoke)
                .addStatement("if (retPtr.%M()) return 1", implPackageRegistry.memberNameForOrDefault("readGdBool"))
                .addStatement("0")
            endControlFlow()
        }
    }

    /**
     * Builds the `hashCode(): Int` body for a builtin class.
     *
     * - If the class has a Godot `hash` method, invokes it via its fptr.
     * - Otherwise, delegate to `Variant.hash`
     *
     * DRY: For types with a Godot `hash` method, we delegate to `hash().toInt()` instead of
     * duplicating the fptr invocation logic.
     */
    fun buildHashCodeBody(resolvedClass: ResolvedBuiltinClass): CodeBlock {
        val hashMethod = resolvedClass.raw.methods.any { it.name == "hash" }

        // DRY: Delegate to hash() method which is generated separately
        return CodeBlock.of("return %L.hash().toInt()", if (hashMethod) "this" else "Variant(this)")
    }

    context(ctx: Context)
    private fun buildFixedArgsBody(method: BuiltinClass.BuiltinMethod, propName: String): CodeBlock = buildCodeBlock {
        val returnType = method.returnType
        if (returnType != null && returnType != "void") add("return ")

        beginControlFlow("%M", memScoped)

        // 1. Alloc return buffer
        if (returnType != null && returnType != "void") add(buildReturnAlloc(returnType))

        // 2. Alloc primitive args
        method.arguments.forEach { arg -> add(buildArgAlloc(arg)) }

        // 3. Build p_base expression
        val pBase = if (method.isStatic) "null" else "rawPtr"

        // 4. Build r_return expression
        val rReturn = when {
            returnType == null || returnType == "void" -> "null"

            returnType == "bool" -> "retPtr"

            isGodotPrimitive(returnType) -> "retPtr.%M"

            // .ptr
            ctx.isBuiltin(returnType) -> "retPtr.rawPtr"

            ctx.findEngineClass(returnType) != null -> "retPtr.%M.%M()"

            // .ptr.reinterpret()
            else -> "null"
        }

        // 4. Invoke
        val argExpressions = method.arguments.joinToCode("") { arg -> argPointerExpression(arg) }.let {
            if (method.isVararg) {
                it.toBuilder().addStatement("*args.map·{·it.rawPtr·}.toTypedArray(),").build()
            } else {
                it
            }
        }

        if (argExpressions.isEmpty()) {
            if (rReturn == "retPtr.%M" || rReturn == "retPtr.%M.%M()") {
                addStatement(
                    "%N.%M(%L, null, retPtr.%M, 0)",
                    propName,
                    cinteropInvoke,
                    pBase,
                    cinteropPtr,
                )
            } else {
                addStatement("%N.%M(%L, null, %L, 0)", propName, cinteropInvoke, pBase, rReturn)
            }
        } else {
            val allocConstTypePtrArray = implPackageRegistry.memberNameForOrDefault("allocConstTypePtrArray")

            addStatement("%N.%M(", propName, cinteropInvoke)

            withIndent {
                addStatement("%L,", pBase)
                addStatement("%M(", allocConstTypePtrArray)
                withIndent { add(argExpressions) }
                addStatement("),")

                if (rReturn == "retPtr.%M" || rReturn == "retPtr.%M.%M()") {
                    addStatement("retPtr.%M,", cinteropPtr)
                } else {
                    addStatement("%L,", rReturn)
                }

                addStatement("%L,", method.arguments.size)
            }
            addStatement(")")
        }

        // 5. Read return
        if (returnType != null && returnType != "void") add(buildReturnRead(returnType))

        endControlFlow()
    }

    context(ctx: Context)
    private fun buildArgAlloc(arg: MethodArg): CodeBlock = buildCodeBlock {
        val name = safeIdentifier(arg.name)
        when {
            arg.type == "float" || arg.type == "double" -> {
                addStatement("val %LVar = %M<%T>()", name, cinteropAlloc, DOUBLE_VAR)
                addStatement("%LVar.%M = %N", name, cinteropValue, name)
            }

            arg.type == "int" -> {
                addStatement("val %LVar = %M<%T>()", name, cinteropAlloc, LONG_VAR)
                addStatement("%LVar.%M = %N", name, cinteropValue, name)
            }

            arg.type == "bool" -> {
                addStatement(
                    "val %LVar = %M(%N)",
                    name,
                    implPackageRegistry.memberNameForOrDefault("allocGdBool"),
                    name,
                )
            }

            ctx.isEngineClass(arg.type) || ctx.isSingleton(arg.type) -> {
                addStatement("val %LVar = %M<%T>()", name, cinteropAlloc, C_OPAQUE_POINTER_VAR)
                addStatement(
                    "%LVar.%M = %N${if (arg.isNullable) "?" else ""}.rawPtr",
                    name,
                    cinteropValue,
                    name,
                )
            }
            // builtin classes and everything else — rawPtr passed directly, no alloc needed
        }
    }

    context(ctx: Context)
    private fun argPointerExpression(arg: MethodArg): CodeBlock {
        val name = safeIdentifier(arg.name)
        return when {
            arg.type == "float" || arg.type == "double" || arg.type == "int" -> CodeBlock.ofStatement(
                "%LVar.%M,",
                name,
                cinteropPtr,
            )

            arg.type == "bool" -> CodeBlock.ofStatement("%LVar,", name)

            ctx.isBuiltin(arg.type) || arg.type == "Variant" -> CodeBlock.ofStatement("%N.rawPtr,", name)

            arg.type.startsWith("enum::") -> CodeBlock.ofStatement(
                "%M<%T>().also·{ it.%M = %N.value }.%M,",
                cinteropAlloc,
                LONG_VAR,
                cinteropValue,
                name,
                cinteropPtr,
            )

            ctx.isEngineClass(arg.type) || ctx.isSingleton(arg.type) -> CodeBlock.ofStatement(
                "%LVar.%M,",
                name,
                cinteropPtr,
            )

            else -> CodeBlock.ofStatement("%N.rawPtr,", name)
        }
    }

    context(ctx: Context)
    private fun buildReturnAlloc(returnType: String): CodeBlock = buildCodeBlock {
        when {
            returnType == "float" || returnType == "double" ->
                addStatement("val retPtr = %M<%T>()", cinteropAlloc, DOUBLE_VAR)

            returnType == "int" ->
                addStatement("val retPtr = %M<%T>()", cinteropAlloc, LONG_VAR)

            returnType == "bool" ->
                addStatement(
                    "val retPtr = %M()",
                    implPackageRegistry.memberNameForOrDefault("allocGdBool"),
                )

            ctx.isBuiltin(returnType) ->
                addStatement(
                    "val retPtr = %T()",
                    ctx.classNameForOrDefault(returnType.renameGodotClass()),
                )

            ctx.findEngineClass(returnType) != null ->
                addStatement("val retPtr = %M<%T>()", cinteropAlloc, C_OPAQUE_POINTER_VAR)

            else -> error("Unknown return type for builtin method: '$returnType'")
        }
    }

    context(ctx: Context)
    private fun buildReturnRead(returnType: String): CodeBlock = when {
        returnType == "float" || returnType == "double" || returnType == "int" ->
            CodeBlock.ofStatement("retPtr.%M", cinteropValue)

        returnType == "bool" ->
            CodeBlock.ofStatement(
                "retPtr.%M()",
                implPackageRegistry.memberNameForOrDefault("readGdBool"),
            )

        ctx.isBuiltin(returnType) -> CodeBlock.ofStatement("retPtr")

        ctx.findEngineClass(returnType) != null -> CodeBlock.ofStatement(
            "%T(%M(retPtr.%M)·{·%S·})",
            ctx.classNameForOrDefault(returnType.renameGodotClass()),
            K_REQUIRE_NOT_NULL,
            cinteropValue,
            "Return pointer value of $returnType was null",
        )

        else -> CodeBlock.ofStatement("retPtr")
    }

    private fun isGodotPrimitive(type: String) = NativeBuiltinClassGenerator.SKIPPED_TYPES.contains(type)

    private fun methodFptrName(className: String, method: BuiltinClass.BuiltinMethod): String =
        "method${className}${safeIdentifier(method.name).replaceFirstChar(Char::uppercase)}_${method.hash.toULong()}_Fn"
}
