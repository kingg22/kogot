package io.github.kingg22.godot.codegen.impl.extensionapi.knative.generators

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.K_AUTOCLOSEABLE
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.impl.BuiltinClassImplGen
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.impl.buildLayoutConstants
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinClass
import io.github.kingg22.godot.codegen.utils.filterValuesNotNull

/**
 * Generates Kotlin Native class declarations for Godot builtin classes
 * (Vector2, Color, Array, Dictionary, etc).
 *
 * Responsibilities of this class:
 * - Class structure: name, supertype, modifiers, kdoc
 * - Members: fields, constants, method signatures, operator signatures, constructors
 * - Companion object with static methods and constants
 *
 * Body generation is delegated to [BodyGenerator].
 *
 * Types that are NOT generated as classes (primitives/nil):
 * [NativeBuiltinClassGenerator.SKIPPED_TYPES] — these are handled via typealiases or extension functions elsewhere.
 */
class NativeBuiltinClassGenerator(
    private val typeResolver: TypeResolver,
    private val body: BuiltinClassImplGen,
    private val defaultValueGenerator: DefaultValueGenerator,
    private val methodGen: NativeMethodGenerator,
    private val enumGenerator: NativeEnumGenerator,
    private val genericInterceptor: GenericBuiltinInterceptor,
    private val typeAliasGenerator: TypeAliasGenerator,
) {
    companion object {
        /**
         * Godot builtin "types" that map to Kotlin primitives — no class is generated for these.
         * Operators/constructors for them are handled via extension functions or typealiases.
         */
        val SKIPPED_TYPES = setOf("int", "float", "bool", "nil")

        /**
         * Maps Godot operator symbols to Kotlin operator function names.
         * Operators not present here are delegated to a regular method (infix or plain).
         */
        val OPERATOR_MAP: Map<String, String> = mapOf(
            "==" to "equals",
            "!=" to null, // Kotlin derives != from equals, skip
            "<" to "compareTo", // Note: compareTo covers <, <=, >, >= together
            "<=" to null, // covered by compareTo
            ">" to null, // covered by compareTo
            ">=" to null, // covered by compareTo
            "+" to "plus",
            "-" to "minus",
            "*" to "times",
            "/" to "div",
            "%" to "rem",
            "unary-" to "unaryMinus",
            "unary+" to "unaryPlus",
            "not" to "not",
            "in" to "contains", // right-hand side perspective: a in b → b.contains(a)
        ).filterValuesNotNull()

        /*
        enum class KotlinOperators(val operatorSign: String, val operatorName: String) {
            UNARY_PLUS("+", "unaryPlus"),
            PLUS("+", "plus"),
            UNARY_MINUS("-", "unaryMinus"),
            MINUS("-", "minus"),
            TIMES("*", "times"),
            DIV("/", "div"),
            REMAINDER("%", "rem"),
            PLUS_ASSIGN("+=", "plusAssign"),
            MINUS_ASSIGN("-=", "minusAssign"),
            TIMES_ASSIGN("*=", "timesAssign"),
            DIV_ASSIGN("/=", "divAssign"),
            REMAINDER_ASSIGN("%=", "remAssign"),
            INCREMENT("++", "inc"),
            DECREMENT("--", "dec"),
            EQUALS("==", "equals"),
            NOT_EQUALS("!=", "equals"),
            NOT("!", "not"),
            RANGE_TO("..", "rangeTo"),
            CONTAINS("in", "contains"),
            NOT_CONTAINS("!in", "contains"),
            GREATER_THAN(">", "compareTo"),
            LESS_THAN("<", "compareTo"),
            GREATER_THAN_OR_EQUAL(">=", "compareTo"),
            LESS_THAN_OR_EQUAL("<=", "compareTo"),
        }
         */

        /**
         * Operators where only `compareTo` is generated (Kotlin derives the rest).
         * We generate compareTo only once even if <, <=, >, >= all appear.
         */
        private val COMPARE_OPERATORS = setOf("<", "<=", ">", ">=")
    }

    /** Generates the [FileSpec] for [builtinClass], or null if it belongs to [NativeBuiltinClassGenerator.SKIPPED_TYPES]. */
    context(context: Context)
    fun generateFile(builtinClass: ResolvedBuiltinClass): FileSpec? {
        val spec = generate(builtinClass) ?: return null
        val godotName = builtinClass.name
        // Members that have no storage offset → need fptr lazy props
        val offsetMembers = builtinClass.layout?.memberOffsets?.keys ?: emptySet()
        val utilMembers = builtinClass.raw.members.filter { it.name !in offsetMembers }
        return createFile(spec.name!!, context.packageForOrDefault(godotName)) {
            typeAliasGenerator.generateTypeAliasSpec(builtinClass.raw)?.let { addTypeAlias(it) }
            addType(spec)
            addProperties(body.buildTopLevelFptrProperties(builtinClass, utilMembers))
        }
    }

    /** Generates the [TypeSpec] for [builtinClass], or null if it belongs to [NativeBuiltinClassGenerator.SKIPPED_TYPES]. */
    context(context: Context)
    fun generate(builtinClass: ResolvedBuiltinClass): TypeSpec? {
        val raw = builtinClass.raw
        if (builtinClass.name.lowercase() in SKIPPED_TYPES) return null
        val requiresGenerics = genericInterceptor.requiresGenerics(raw)

        val kotlinName = builtinClass.name.renameGodotClass(requiresGenerics)

        val classBuilder = TypeSpec
            .classBuilder(kotlinName)
            .experimentalApiAnnotation(builtinClass.name)
            .addKdocIfPresent(raw)
            .let { body.configureStorageBackedBuiltin(builtinClass, it) }

        // ── GENERIC INTERCEPTION ──────────────────────────────────────────────
        val genericConfig = if (requiresGenerics) {
            val config = genericInterceptor.getGenericConfig(raw)
            config?.typeVariables?.let {
                classBuilder.addTypeVariables(it)
            }
            config
        } else {
            null
        }

        // ── Destructor annotation / marker ───────────────────────────────────
        if (builtinClass.hasDestructor) {
            classBuilder.addSuperinterface(K_AUTOCLOSEABLE)
            classBuilder.addFunction(body.buildCloseFunction(builtinClass))
        }

        // ── Members (fields like x, y, z) ────────────────────────────────────
        raw.members.forEach { member ->
            val memberMeta = context.resolveBuiltinMemberMeta(builtinClass.name, member.name)
            val memberType = typeResolver.resolve(member.type, memberMeta)

            val propBuilder = PropertySpec
                .builder(safeIdentifier(member.name), memberType)
                .mutable(true)
                .experimentalApiAnnotation(builtinClass.name, member.name)
                .addKdocIfPresent(member)

            if (memberMeta != null) {
                // Has direct storage offset → fast path
                val getter = body.buildMemberGetter(member.name, memberMeta, memberType)
                propBuilder.getter(getter)

                val setter = body.buildMemberSetter(member.name, memberMeta, memberType)
                propBuilder.setter(setter)
            } else {
                // No storage offset → fptr path (utility/computed member)
                propBuilder.getter(body.buildMemberGetterViaFptr(member.name, memberType))
                propBuilder.setter(body.buildMemberSetterViaFptr(member.name, memberType))
            }

            classBuilder.addProperty(propBuilder.build())
        }

        // ── Constructors ─────────────────────────────────────────────────────
        // Godot constructors become secondary constructors (or companion factory funs if static-style).
        // No primary constructor is ever generated for builtins.
        // Index 0 is always the no-arg constructor.
        val constructorsSpecs = builtinClass.constructors.filter { it.raw != null }.map { ctor ->
            val ctorBuilder = FunSpec
                .constructorBuilder()
                .addKdocIfPresent(ctor.raw!!)

            val argumentSpecs = ctor.arguments.map { arg -> methodGen.buildParameter(arg) }

            ctorBuilder.addParameters(argumentSpecs)
            ctorBuilder.addCode(body.constructorBodyFor(builtinClass, ctor))

            ctorBuilder.build()
        }
        classBuilder.addFunctions(constructorsSpecs)

        // ── Special constructors for String types ─────────────────────────────
        when (builtinClass.name) {
            "String", "StringName", "NodePath" -> {
                classBuilder.addFunction(
                    FunSpec
                        .constructorBuilder()
                        .addParameter("value", STRING) // kotlin.String
                        .addKdoc("Creates a %L from a Kotlin String.", kotlinName)
                        .addCode(body.stringConstructorBodyFor(builtinClass))
                        .build(),
                )
            }
        }

        // ── Operators ────────────────────────────────────────────────────────
        classBuilder.addFunctions(generateOperators(raw, genericConfig))

        // ── Instance methods ──────────────────────────────────────────────────
        val (staticMethods, instanceMethods) = raw.methods.partition { it.isStatic }

        val methodSpecs = instanceMethods.mapNotNull { method ->
            val methodSpec = buildMethodWithGenericTransform(method, builtinClass.name, genericConfig)

            if (raw.operators.any { compareMethodOperator(method, it) }) {
                val existingOperator = raw.operators.first { compareMethodOperator(method, it) }
                println(
                    "INFO: Skipping operator overload for ${builtinClass.name}.${method.name}(${existingOperator.rightType}): ${existingOperator.returnType} because it's already defined as operator",
                )
                return@mapNotNull null
            }

            methodSpec
        }
        classBuilder.addFunctions(methodSpecs)

        // ── Companion object (constants + static methods + layout offsets) ─────
        val companionBuilder = TypeSpec.companionObjectBuilder()

        // Layout offset constants (OFFSET_X, OFFSET_Y, …)
        builtinClass.layout?.takeIf { it.memberOffsets.isNotEmpty() }?.let { layout ->
            buildLayoutConstants(layout).forEach { companionBuilder.addProperty(it) }
        }

        // Constants from JSON
        raw.constants.forEach { constant ->
            val constType = typeResolver.resolve(constant.type)

            companionBuilder.addProperty(
                PropertySpec
                    .builder(constant.name, constType)
                    .experimentalApiAnnotation(builtinClass.name, constant.name)
                    .addKdocIfPresent(constant)
                    .initializer("%L", defaultValueGenerator.generate(constant.value, constant.type, constType))
                    .build(),
            )
        }

        // Static methods
        val staticMethodSpecs = staticMethods.map { method ->
            methodGen.buildMethod(method, builtinClass.name, codeBody = body.buildMethodBody(method, builtinClass.name))
        }
        companionBuilder.addFunctions(staticMethodSpecs)

        val companionHasContent = builtinClass.layout?.memberOffsets?.isNotEmpty() == true ||
            raw.constants.isNotEmpty() ||
            staticMethods.isNotEmpty()

        if (companionHasContent) {
            classBuilder.addType(companionBuilder.build())
        }

        val enumSpecs = builtinClass.enums.mapNotNull { enum ->
            if (context.isSpecializedClass(enum.shortName)) return@mapNotNull null
            enumGenerator.generateSpec(enum)
        }
        classBuilder.addTypes(enumSpecs)

        return classBuilder.build()
    }

    // ── Method generation with generic transformation ─────────────────────────
    context(_: Context)
    private fun buildMethodWithGenericTransform(
        method: BuiltinClass.BuiltinMethod,
        className: String,
        genericConfig: GenericBuiltinInterceptor.GenericConfig?,
    ): FunSpec {
        // Si no hay config genérica, usar generador normal
        if (genericConfig == null) {
            return methodGen.buildMethod(method, className, codeBody = body.buildMethodBody(method, className)) {
                if ((method.name == "get" && method.arguments.size == 1) ||
                    (method.name == "set" && method.arguments.size == 2)
                ) {
                    addModifiers(KModifier.OPERATOR)
                }
            }
        }

        // Resolver tipo de retorno original
        val originalReturnType = method.returnType?.let { typeResolver.resolve(it) }

        // Transformar con config genérica
        val transformedReturnType = genericConfig.transformReturnType(method, originalReturnType)

        // Construir método con tipo transformado
        return methodGen.buildMethod(method, className, codeBody = body.buildMethodBody(method, className)) {
            if ((method.name == "get" && method.arguments.size == 1) ||
                (method.name == "set" && method.arguments.size == 2)
            ) {
                addModifiers(KModifier.OPERATOR)
            }

            if (transformedReturnType != null && transformedReturnType != originalReturnType) {
                returns(transformedReturnType)
            }

            // Transformar parámetros
            parameters.clear()
            method.arguments.forEachIndexed { index, arg ->
                val originalType = typeResolver.resolve(arg)
                val transformedType = genericConfig.transformParameterType(method, index, originalType)
                addParameter(
                    methodGen
                        .buildParameter(arg)
                        .toBuilder(type = transformedType)
                        .build(),
                )
            }
        }
    }

    // ── Operator generation ───────────────────────────────────────────────────
    context(_: Context)
    private fun generateOperators(
        builtinClass: BuiltinClass,
        genericConfig: GenericBuiltinInterceptor.GenericConfig?,
    ): List<FunSpec> {
        val result = mutableListOf<FunSpec>()
        var compareToGenerated = false

        // Group operators by symbol to handle overloads (e.g. * with int and with Vector2)
        for (op in builtinClass.operators) {
            val symbol = op.name
            val kotlinOpName = OPERATOR_MAP[symbol]

            when {
                // compareTo covers <, <=, >, >= — generate once, first occurrence wins
                symbol in COMPARE_OPERATORS -> {
                    if (!compareToGenerated) {
                        result += buildCompareToOperator(builtinClass)
                        compareToGenerated = true
                    }
                }

                // != is derived from equals in Kotlin — skip
                symbol == "!=" -> Unit

                // Recognized operator with a Kotlin keyword
                kotlinOpName != null -> {
                    result += buildKotlinOperator(
                        name = kotlinOpName,
                        rightType = op.rightType,
                        returnType = op.returnType,
                        genericConfig = genericConfig,
                        operator = op,
                    )
                }

                // Unknown operator — delegate to named method (infix if binary)
                else -> {
                    println(
                        "WARNING: Unknown operator found ${builtinClass.name}.${op.name}(${op.rightType.orEmpty()}): ${op.returnType}",
                    )
                    result += buildFallbackOperatorMethod(op, builtinClass.name)
                }
            }
        }

        return result
    }

    /** Builds a standard Kotlin operator fun (plus, minus, times, equals, not, etc). */
    context(_: Context)
    private fun buildKotlinOperator(
        name: String,
        rightType: String?,
        returnType: String,
        genericConfig: GenericBuiltinInterceptor.GenericConfig?,
        operator: BuiltinClass.Operator,
    ): FunSpec {
        val originalReturnType = typeResolver.resolve(returnType)

        // Transformar tipo de retorno si hay config genérica
        val returnTypeName = genericConfig?.transformOperatorReturnType(operator, originalReturnType)
            ?: originalReturnType

        val builder = FunSpec
            .builder(name)
            .apply {
                if (name != "equals") addModifiers(KModifier.OPERATOR)
            }.returns(returnTypeName)
            .addCode(body.todoBody("Missing operator implementation: $name"))
            .addKdocIfPresent(operator)

        if (rightType != null) {
            val rightTypeName = typeResolver.resolve(rightType)
            builder.addParameter("other", rightTypeName)
        }

        return builder.build()
    }

    /**
     * Builds `override fun compareTo(other: T): Int` for ordering operators.
     * Kotlin derives <, <=, >, >= from a single compareTo.
     */
    context(_: Context)
    private fun buildCompareToOperator(builtinClass: BuiltinClass): FunSpec {
        val selfType = typeResolver.resolve(builtinClass.name)
        return FunSpec
            .builder("compareTo")
            .addModifiers(KModifier.OPERATOR)
            .addParameter("other", selfType)
            .returns(INT)
            .addKdoc("Supports `<`, `<=`, `>`, `>=` operators via Kotlin compareTo convention.")
            .addCode(body.todoBody("Missing operator implementation: compareTo"))
            .build()
    }

    /**
     * Builds a named fallback for operators Kotlin has no keyword for (e.g. `in` as infix,
     * or any future unknown symbol).
     *
     * Binary ops become `infix fun`, unary ops become regular funs.
     */
    context(_: Context)
    private fun buildFallbackOperatorMethod(op: BuiltinClass.Operator, className: String): FunSpec {
        val safeName = safeIdentifier(op.name) // .replace(" ", "_")
        val returnTypeName = typeResolver.resolve(op.returnType)
        val builder = FunSpec
            .builder(safeName)
            .returns(returnTypeName)
            .addCode(body.todoBody("Missing operator implementation: $safeName"))
            .experimentalApiAnnotation(className, op.name)
            .addKdocIfPresent(op)
            .addKdoc("\nGodot operator: `%L`", op.name)

        if (op.rightType != null) {
            val rightTypeName = typeResolver.resolve(op.rightType)
            builder.addParameter("other", rightTypeName)
            builder.addModifiers(KModifier.INFIX)
        }

        return builder.build()
    }

    // Helpers
    private fun isOperatorMethod(method: BuiltinClass.BuiltinMethod): Boolean = method.arguments.size <= 1

    /** @return `true` if the method is equals to operator, `false` otherwise */
    private fun compareMethodOperator(method: BuiltinClass.BuiltinMethod, operator: BuiltinClass.Operator): Boolean {
        if (!isOperatorMethod(method)) return false
        val opMethodName = OPERATOR_MAP[operator.name] ?: return false
        if (method.name != opMethodName) return false
        if (operator.rightType != null && method.arguments.size != 1) return false
        if (method.returnType != operator.returnType) return false
        if (operator.rightType != null && method.arguments[0].type != operator.rightType) return false
        return true
    }
}
