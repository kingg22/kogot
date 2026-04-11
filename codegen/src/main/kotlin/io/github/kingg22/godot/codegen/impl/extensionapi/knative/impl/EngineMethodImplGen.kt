package io.github.kingg22.godot.codegen.impl.extensionapi.knative.impl

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.impl.K_REQUIRE_NOT_NULL
import io.github.kingg22.godot.codegen.impl.K_TODO
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.COPAQUE_POINTER
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.C_OPAQUE_POINTER_VAR
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.C_POINTER
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.C_POINTER_VAR
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.LONG_VAR
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.cinteropAlloc
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.cinteropPtr
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.cinteropValue
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.memScoped
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensionapi.EngineClass
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedEngineClass

/**
 * Generates lazy-loaded method bind properties and ptrcall bodies for engine class methods/properties.
 *
 * ## Calling convention
 *
 * Engine methods use `GDExtensionMethodBindPtr` retrieved via
 * `ClassDBBinding.instance.getMethodBindRaw(className, methodName, hash)`.
 * Invocation: `ObjectBinding.instance.methodBindPtrcallRaw(bind, p_object, p_args, r_ret)`.
 *
 * ## Type resolution
 *
 * Always delegate to [typeResolver] — it carries full meta-aware logic. Never dispatch on
 * raw type strings for CVar selection; use [primitiveKotlinToCVar] on the resolved [TypeName].
 *
 * Examples:
 * - `type=int, meta=uint64` → resolver returns `ULong` → `ULongVar` (no `.toLong()`)
 * - `type=int, meta=int32`  → resolver returns `Int`   → `IntVar`
 * - `type=float` (no meta)  → resolver returns `Double`→ `DoubleVar`
 * - `type=float, meta=float`→ resolver returns `Float` → `FloatVar`
 *
 * ## Vararg methods
 *
 * Fixed args are collected together with the trailing `vararg args: Variant` into a single
 * list, mapped to `.rawPtr`, and passed via `methodBindCall` (varcall path), because
 * ptrcall is only safe for statically-known arg counts.
 *
 * ## Enum return values
 *
 * When the resolved return type is an enum, `retPtr` is a `LongVar` and the read emits
 * `godotEnumFrom<TheEnum>(retPtr.value)`.
 *
 * ## Abstract / non-instantiable return types
 *
 * Cannot do `AbstractClass(ptr)` — emits `TODO()`. Follow-up: Godot runtime-type cast helper.
 *
 * ## Nullable vs non-null engine class return
 *
 * `COpaquePointerVar.value` is nullable. For non-null declared returns we emit
 * `requireNotNull(retPtr.value) { "…" }.let { T(it) }`.
 */
class EngineMethodImplGen(private val typeResolver: TypeResolver) {
    private lateinit var implPackageRegistry: ImplementationPackageRegistry

    fun initialize(implRegistry: ImplementationPackageRegistry) {
        implPackageRegistry = implRegistry
    }

    // ── Top-level lazy fptr properties ────────────────────────────────────────

    /**
     * One top-level `private val` lazy property per engine method that has a method bind.
     *
     * Virtual methods without a hash have no bind on the Godot side and are skipped.
     */
    context(ctx: Context)
    fun buildTopLevelFptrProperties(cls: ResolvedEngineClass): List<PropertySpec> {
        val classDBBinding = implPackageRegistry.classNameForOrDefault("ClassDBBinding")
        val stringNameClass = ctx.classNameForOrDefault("StringName")
        return cls.raw.methods.mapNotNull { buildMethodBindProperty(it, cls.name, classDBBinding, stringNameClass) }
    }

    private fun buildMethodBindProperty(
        method: EngineClass.ClassMethod,
        className: String,
        classDBBinding: ClassName,
        stringNameClass: ClassName,
    ): PropertySpec? {
        if (method.isVirtual) return null
        val bindType = implPackageRegistry.classNameForOrDefault("GDExtensionMethodBindPtr")
        val body = buildLazyBlock {
            beginControlFlow("%T(%S).use { cn ->", stringNameClass, className)
                .beginControlFlow("%T(%S).use { mn ->", stringNameClass, method.name)
                .addStatement(
                    "%T.instance.getMethodBindRaw(cn.rawPtr, mn.rawPtr, %LL)",
                    classDBBinding,
                    method.hash,
                )
                .withIndent {
                    addStatement(
                        "?: error(\"Missing method bind '%L', hash: %L\")",
                        "$className.${method.name}",
                        method.hash,
                    )
                }
                .endControlFlow()
            endControlFlow()
        }
        return PropertySpec
            .builder(methodBindName(className, method), bindType, KModifier.PRIVATE)
            .delegate(body)
            .build()
    }

    // ── Method bodies ─────────────────────────────────────────────────────────

    context(_: Context)
    fun buildMethodBody(method: EngineClass.ClassMethod, className: String): CodeBlock {
        if (method.isVirtual) {
            val rv = method.returnValue
            val returnType = rv?.type ?: rv?.meta
            val hasReturn = returnType != null && returnType != "void"
            return if (hasReturn) {
                CodeBlock.of(
                    "%M(%P)",
                    K_TODO,
                    $$"Virtual method '$${method.name}' of '$$className' requires override in ${this::class.simpleName}",
                )
            } else {
                CodeBlock.of("")
            }
        }
        return buildPtrcallBody(method, className)
    }

    // ── Property bodies ───────────────────────────────────────────────────────

    context(_: Context)
    fun buildPropertyGetterBody(getter: EngineClass.ClassMethod, cls: ResolvedEngineClass): CodeBlock {
        if (getter.isVirtual) println("WARNING: Found virtual getter: $getter in $cls")
        return buildPtrcallBody(getter, cls.name)
    }

    context(_: Context)
    fun buildPropertySetterBody(setter: EngineClass.ClassMethod, cls: ResolvedEngineClass): CodeBlock {
        if (setter.isVirtual) println("WARNING: Found virtual setter: $setter in $cls")
        return buildPtrcallBody(setter, cls.name, true)
    }

    // ── ptrcall body ───────────────────────────────────────────────

    context(ctx: Context)
    private fun buildPtrcallBody(
        method: EngineClass.ClassMethod,
        className: String,
        setterMode: Boolean = false,
    ): CodeBlock {
        val objectBinding = implPackageRegistry.classNameForOrDefault("ObjectBinding")
        val propName = methodBindName(className, method)
        val allocConstTypePtrArray = implPackageRegistry.memberNameForOrDefault("allocConstTypePtrArray")

        val rv = method.returnValue
        val returnType = rv?.type
        val hasReturn = returnType != null && returnType != "void"
        val resolvedReturn = if (hasReturn) typeResolver.resolve(rv) else null

        return buildCodeBlock {
            if (hasReturn && !setterMode) add("return ")

            // TODO check if this is still needed when doesn't have to alloc anything
            beginControlFlow("%M", memScoped)

            // 1. Return buffer
            if (hasReturn && resolvedReturn != null) add(buildReturnAlloc(returnType, resolvedReturn))

            // 2. Arg allocs — last arg uses name "value" in setter mode
            method.arguments.forEach { arg ->
                val resolved = typeResolver.resolve(arg)
                add(buildArgAlloc(arg, resolved))
            }

            // 3. p_object
            val pObject = if (method.isStatic) "null" else "rawPtr"

            // 4. Invocation
            val argPtrs = method.arguments.map { arg ->
                val resolved = typeResolver.resolve(arg)
                argPointerExpression(arg, resolved)
            } + buildList {
                if (method.isVararg) {
                    this.add(CodeBlock.ofStatement("*args.map·{·it.rawPtr·}.toTypedArray(),"))
                }
            }

            addStatement("%T.instance.methodBindPtrcallRaw(", objectBinding)
            withIndent {
                addStatement("%N,", propName)
                addStatement("%L,", pObject)
                if (argPtrs.isEmpty()) {
                    addStatement("null,")
                } else {
                    addStatement("%M(", allocConstTypePtrArray)
                    withIndent { add(argPtrs.joinToCode("")) }
                    addStatement("),")
                }
                if (hasReturn && resolvedReturn != null) {
                    val rRet = rRetLiteral(returnType, resolvedReturn)
                    if (rRet == "retPtr.ptr") {
                        addStatement("retPtr.%M,", cinteropPtr)
                    } else {
                        addStatement("%L,", rRet)
                    }
                } else {
                    addStatement("null,")
                }
            }
            addStatement(")")

            // 5. Return read
            if (hasReturn && resolvedReturn != null) {
                if (setterMode) {
                    val contextInfo = "$className.${method.name}"

                    when (resolvedReturn) {
                        // Caso especial: GodotError
                        ctx.classNameForOrDefault("GodotError") -> {
                            addStatement("%M(", implPackageRegistry.memberNameForOrDefault("checkGodotError"))
                            withIndent {
                                addStatement("%S,", contextInfo)
                                // Aquí insertamos la lectura del retorno como segundo argumento
                                add(buildReturnRead(returnType, resolvedReturn))
                            }
                            addStatement(")")
                        }

                        // Caso especial: Boolean check
                        BOOLEAN -> {
                            addStatement("check(")
                            withIndent { add(buildReturnRead(returnType, resolvedReturn)) }
                            addStatement(")·{♢%S♢}", "$contextInfo doesn't return true")
                        }

                        // Fallback para otros tipos en setterMode (si aplica)
                        else -> {
                            add(buildReturnRead(returnType, resolvedReturn))
                        }
                    }
                } else {
                    // No es setterMode: Solo emitimos la lectura normal del retorno
                    add("return ")
                    add(buildReturnRead(returnType, resolvedReturn))
                }
            }

            endControlFlow()
        }
    }

    // ── Arg marshalling ───────────────────────────────────────────────────────

    private fun buildArgAlloc(arg: MethodArg, resolvedType: TypeName): CodeBlock {
        val name = safeIdentifier(arg.name)
        val varName = "${name}Var"
        val cVarType = primitiveKotlinToCVar(resolvedType)
        return buildCodeBlock {
            when {
                resolvedType == BOOLEAN -> addStatement(
                    "val %N = %M(%N)",
                    varName,
                    implPackageRegistry.memberNameForOrDefault("allocGdBool"),
                    name,
                )

                cVarType != null -> {
                    addStatement("val %N = %M<%T>()", varName, cinteropAlloc, cVarType)
                    addStatement("%N.%M = %N", varName, cinteropValue, name)
                }

                arg.type.startsWith("enum::") || arg.type.startsWith("bitfield::") -> {
                    addStatement("val %N = %M<%T>()", varName, cinteropAlloc, LONG_VAR)
                    addStatement("%N.%M = %N.value", varName, cinteropValue, name)
                }
                // Builtin / engine class → rawPtr, no alloc
            }
        }
    }

    context(ctx: Context)
    private fun argPointerExpression(arg: MethodArg, resolvedType: TypeName): CodeBlock {
        val name = safeIdentifier(arg.name)
        val varName = "${name}Var"
        return when {
            resolvedType == BOOLEAN -> CodeBlock.ofStatement("%L,", varName)

            resolvedType == COPAQUE_POINTER ||
                resolvedType == ctx.classNameForOrDefault("GDExtensionInitializationFunction") ||
                (resolvedType is ParameterizedTypeName && resolvedType.rawType == C_POINTER)
            -> CodeBlock.ofStatement("%N,", name)

            // allocGdBool returns CArrayPointer directly

            primitiveKotlinToCVar(resolvedType) != null -> CodeBlock.ofStatement("%N.%M,", varName, cinteropPtr)

            ctx.isNativeStructure(arg.type) -> CodeBlock.ofStatement("%N.%M,", name, cinteropPtr)

            arg.type.startsWith("enum::") || arg.type.startsWith("bitfield::") ->
                CodeBlock.ofStatement("%N.%M,", varName, cinteropPtr)

            ctx.isGodotType(arg.type) ||
                arg.type.startsWith("array") ||
                arg.type.startsWith("dictionary") ||
                arg.type.startsWith("typeddictionary") ||
                arg.type.startsWith("typedarray")
            -> CodeBlock.ofStatement("%N${if (arg.isNullable) "?" else ""}.rawPtr,", name)

            else -> error("Invalid arg type, unknown strategy to invoke: '${arg.type}' (resolved: $resolvedType)")
        }
    }

    // ── Return marshalling ────────────────────────────────────────────────────

    context(ctx: Context)
    private fun buildReturnAlloc(returnType: String, resolvedReturn: TypeName): CodeBlock {
        val cVarType = primitiveKotlinToCVar(resolvedReturn)
        return when {
            resolvedReturn == BOOLEAN -> CodeBlock.ofStatement(
                "val retPtr = %M()",
                implPackageRegistry.memberNameForOrDefault("allocGdBool"),
            )

            cVarType != null -> CodeBlock.ofStatement("val retPtr = %M<%T>()", cinteropAlloc, cVarType)

            ctx.isNativeStructure(returnType) ->
                CodeBlock.ofStatement("val retPtr = %M<%T>()", cinteropAlloc, resolvedReturn)

            returnType.startsWith("enum::") || returnType.startsWith("bitfield::") ->
                CodeBlock.ofStatement("val retPtr = %M<%T>()", cinteropAlloc, LONG_VAR)

            ctx.isBuiltin(returnType) ||
                returnType.startsWith("array") ||
                returnType.startsWith("dictionary") ||
                returnType.startsWith("typeddictionary") ||
                returnType.startsWith("typedarray") -> CodeBlock.ofStatement("val retPtr = %T()", resolvedReturn)

            resolvedReturn == COPAQUE_POINTER || ctx.isEngineClass(returnType) -> CodeBlock.ofStatement(
                "val retPtr = %M<%T>()",
                cinteropAlloc,
                C_OPAQUE_POINTER_VAR,
            )

            resolvedReturn is ParameterizedTypeName && resolvedReturn.rawType == C_POINTER -> CodeBlock.ofStatement(
                "val retPtr = %M<%T>()",
                cinteropAlloc,
                C_POINTER_VAR.parameterizedBy(resolvedReturn.typeArguments),
            )

            else -> error("Invalid return type, unknown strategy: '$returnType' (resolved: $resolvedReturn)")
        }
    }

    /**
     * Returns the r_ret expression string for the inline (single-line) invocation case.
     *
     * Callers that need the `.ptr` suffix must emit `retPtr.%M` with [cinteropPtr] as the
     * MemberName — so this returns the sentinel string `"retPtr.ptr"` for that case, which
     * the caller detects and handles.
     */
    context(ctx: Context)
    private fun rRetLiteral(returnType: String, resolvedReturn: TypeName): String {
        val cVarType = primitiveKotlinToCVar(resolvedReturn)
        return when {
            resolvedReturn == BOOLEAN -> "retPtr"

            cVarType != null ||
                returnType.startsWith("enum::") ||
                returnType.startsWith("bitfield::") ||
                ctx.findEngineClass(returnType) != null ||
                (resolvedReturn is ParameterizedTypeName && resolvedReturn.rawType == C_POINTER) ||
                resolvedReturn == COPAQUE_POINTER
            -> "retPtr.ptr"

            ctx.isBuiltin(returnType) ||
                returnType.startsWith("array") ||
                returnType.startsWith("dictionary") ||
                returnType.startsWith("typeddictionary") ||
                returnType.startsWith("typedarray") -> "retPtr.rawPtr"

            else -> "retPtr"
        }
    }

    context(ctx: Context)
    private fun buildReturnRead(returnType: String, resolvedReturn: TypeName): CodeBlock = buildCodeBlock {
        val cVarType = primitiveKotlinToCVar(resolvedReturn)
        when {
            resolvedReturn == BOOLEAN -> addStatement(
                "retPtr.%M()",
                implPackageRegistry.memberNameForOrDefault("readGdBool"),
            )

            // Primitive numeric: CVar.value already has the exact Kotlin type from the resolver
            cVarType != null -> addStatement("retPtr.%M", cinteropValue)

            resolvedReturn == COPAQUE_POINTER ||
                (resolvedReturn is ParameterizedTypeName && resolvedReturn.rawType == C_POINTER) -> {
                addStatement("%M(retPtr.%M) {", K_REQUIRE_NOT_NULL, cinteropValue)
                indent()
                addStatement(
                    "%S",
                    "${returnType.removePrefix("const ").removeSuffix("*").trim()} pointer value was null",
                )
                endControlFlow()
            }

            returnType.startsWith("enum::") -> {
                val godotEnum = ctx.classNameForOrDefault("GodotEnum", "GodotEnum")
                addStatement("%T.fromValue<%T>(retPtr.%M)", godotEnum, resolvedReturn, cinteropValue)
            }

            returnType.startsWith("bitfield::") -> {
                addStatement("%T(retPtr.%M)", resolvedReturn, cinteropValue)
            }

            ctx.isBuiltin(returnType) -> addStatement("retPtr")

            ctx.findResolvedEngineClass(returnType) != null -> {
                addStatement("%T(", resolvedReturn)
                withIndent {
                    addStatement(
                        "%M(retPtr.%M) {",
                        K_REQUIRE_NOT_NULL,
                        cinteropValue,
                    )
                    withIndent { addStatement("%S", "$returnType pointer value was null") }
                    addStatement("},")
                }
                addStatement(")")
            }

            else -> addStatement("retPtr")
        }
    }

    // ── Naming ────────────────────────────────────────────────────────────────

    fun methodBindName(className: String, method: EngineClass.ClassMethod): String = "method$className" +
        safeIdentifier(method.name).replaceFirstChar(Char::uppercase) + "_Bind"
}
