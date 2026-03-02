package io.github.kingg22.godot.codegen.impl.extensionapi.native

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.godot.codegen.impl.K_AUTOCLOSEABLE
import io.github.kingg22.godot.codegen.impl.K_TODO
import io.github.kingg22.godot.codegen.impl.addKdocForBitfield
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.shared.EnumGenerator
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
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
 * [KotlinNativeBuiltinClassGenerator.SKIPPED_TYPES] — these are handled via typealiases or extension functions elsewhere.
 */
class KotlinNativeBuiltinClassGenerator(
    private val typeResolver: TypeResolver,
    private val enums: EnumGenerator,
    private val body: BodyGenerator = BodyGenerator(),
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

    /** Generates the [FileSpec] for [builtinClass], or null if it belongs to [KotlinNativeBuiltinClassGenerator.SKIPPED_TYPES]. */
    context(context: Context)
    fun generateFile(builtinClass: BuiltinClass): FileSpec? {
        val spec = generate(builtinClass) ?: return null
        val godotName = builtinClass.name
        return createFile(spec, spec.name!!, context.packageForOrDefault(godotName))
    }

    /** Generates the [TypeSpec] for [builtinClass], or null if it belongs to [KotlinNativeBuiltinClassGenerator.SKIPPED_TYPES]. */
    context(_: Context)
    fun generate(builtinClass: BuiltinClass): TypeSpec? {
        if (builtinClass.name.lowercase() in SKIPPED_TYPES) return null

        val kotlinName = builtinClass.name.renameGodotClass()
        val classBuilder = TypeSpec.classBuilder(kotlinName)

        /* KDOC
        val kdoc = buildKdoc(
            description = listOfNotNull(builtinClass.briefDescription, builtinClass.description),
        )
        if (kdoc.isNotBlank()) classBuilder.addKdoc(kdoc)
         */

        // Indexable builtins implement operator get/set; keyed ones implement Map-like access.

        // ── Destructor annotation / marker ───────────────────────────────────
        if (builtinClass.hasDestructor) {
            classBuilder.addSuperinterface(K_AUTOCLOSEABLE)
            classBuilder.addFunction(
                FunSpec
                    .builder("close")
                    .addModifiers(KModifier.OVERRIDE)
                    .addCode(body.todoBody())
                    .build(),
            )
        }

        // ── Members (fields like x, y, z) ────────────────────────────────────
        // FIXME needs to be mutable?
        builtinClass.members.forEach { member ->
            val memberType = typeResolver.resolve(member.type)
            val propBuilder = PropertySpec
                .builder(safeIdentifier(member.name), memberType)
                .mutable(true)
            // member.description?.takeIf { it.isNotBlank() }?.let { propBuilder.addKdoc(it) }
            propBuilder.getter(body.todoGetter())
            propBuilder.setter(
                FunSpec
                    .setterBuilder()
                    .addParameter("value", memberType)
                    .addCode(body.todoBody())
                    .build(),
            )
            classBuilder.addProperty(propBuilder.build())
        }

        // ── Constructors ─────────────────────────────────────────────────────
        // Godot constructors become secondary constructors (or companion factory funs if static-style).
        // No primary constructor is ever generated for builtins.
        // Index 0 is always the no-arg constructor.
        builtinClass.constructors.forEach { ctor ->
            val ctorBuilder = FunSpec.constructorBuilder()
            // ctor.description?.takeIf { it.isNotBlank() }?.let { ctorBuilder.addKdoc(it) }
            ctor.arguments.forEach { arg ->
                ctorBuilder.addParameter(buildParameter(arg))
            }
            ctorBuilder.addCode(body.todoBody())
            classBuilder.addFunction(ctorBuilder.build())
        }

        // ── Operators ────────────────────────────────────────────────────────
        classBuilder.addFunctions(generateOperators(builtinClass))

        // ── Instance methods ──────────────────────────────────────────────────
        val (staticMethods, instanceMethods) = builtinClass.methods.partition { it.isStatic }

        instanceMethods.forEach { method ->
            var methodSpec = generateMethod(method)
            if (builtinClass.isKeyed && (method.name == "get" || method.name == "set")) {
                methodSpec = methodSpec.toBuilder().addModifiers(KModifier.OPERATOR).build()
            }
            if (builtinClass.operators.any { compareMethodOperator(method, it) }) {
                val existingOperator = builtinClass.operators.first { compareMethodOperator(method, it) }
                println(
                    "INFO: Skipping operator overload for ${builtinClass.name}.${method.name}(${existingOperator.rightType}): ${existingOperator.returnType} because it's already defined as operator",
                )
                return@forEach
            }
            classBuilder.addFunction(methodSpec)
        }

        // ── Companion object (constants + static methods) ─────────────────────
        val companionBuilder = TypeSpec.companionObjectBuilder()
        val companionHasContent = staticMethods.isNotEmpty() || builtinClass.constants.isNotEmpty()

        // Constants
        builtinClass.constants.forEach { constant ->
            val constType = typeResolver.resolve(constant.type)
            val propBuilder = PropertySpec.builder(constant.name, constType)
            // constant.description?.takeIf { it.isNotBlank() }?.let { propBuilder.addKdoc(it) }
            // Value is provided as a Godot expression string (e.g. "Vector2(0, 0)").
            // For now we use TODO() getter; impl layer will replace with actual value.
            propBuilder.getter(body.todoGetter())
            companionBuilder.addProperty(propBuilder.build())
        }

        // Static methods
        staticMethods.forEach { method ->
            companionBuilder.addFunction(generateMethod(method))
        }

        if (companionHasContent) {
            classBuilder.addType(companionBuilder.build())
        }

        // ── Enums (generated externally, but nested types are added here if present) ──
        // Enums are handled by a separate EnumGenerator and linked externally.
        for (enum in builtinClass.enums) {
            val enumTypeSpec = enums.generateSpec(enum)
            classBuilder.addType(enumTypeSpec)
        }

        return classBuilder.build()
    }

    // ── Operator generation ───────────────────────────────────────────────────

    context(_: Context)
    private fun generateOperators(builtinClass: BuiltinClass): List<FunSpec> {
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
                        result += buildCompareToOperator(builtinClass, op.returnType)
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
                        description = op.description,
                    )
                }

                // Unknown operator — delegate to named method (infix if binary)
                else -> {
                    println(
                        "WARNING: Unknown operator found ${builtinClass.name}.${op.name}(${op.rightType.orEmpty()}): ${op.returnType}",
                    )
                    result += buildFallbackOperatorMethod(op)
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
        description: String?,
    ): FunSpec {
        val returnTypeName = typeResolver.resolve(returnType)
        val builder = FunSpec
            .builder(name)
            .apply {
                if (name != "equals") addModifiers(KModifier.OPERATOR)
            }.returns(returnTypeName)
            .addCode(body.todoBody())

        // description?.takeIf { it.isNotBlank() }?.let { builder.addKdoc(it) }
        builder.addKdocForBitfield(returnType)

        if (rightType != null) {
            val rightTypeName = typeResolver.resolve(rightType)
            builder.addParameter("other", rightTypeName)
        }

        /*
        equals must override Any.equals
        if (name == "equals") {
            builder
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(
                    ParameterSpec
                        .builder("other", ANY.copy(nullable = true))
                        .build(),
                )
        }
         */

        return builder.build()
    }

    /**
     * Builds `override fun compareTo(other: T): Int` for ordering operators.
     * Kotlin derives <, <=, >, >= from a single compareTo.
     */
    context(_: Context)
    private fun buildCompareToOperator(builtinClass: BuiltinClass, returnType: String): FunSpec {
        val selfType = typeResolver.resolve(builtinClass.name)
        return FunSpec
            .builder("compareTo")
            .addModifiers(KModifier.OPERATOR)
            .addParameter("other", selfType)
            .returns(INT)
            .addKdoc("Supports `<`, `<=`, `>`, `>=` operators via Kotlin compareTo convention.")
            .addCode(body.todoBody())
            .build()
    }

    /**
     * Builds a named fallback for operators Kotlin has no keyword for (e.g. `in` as infix,
     * or any future unknown symbol).
     *
     * Binary ops become `infix fun`, unary ops become regular funs.
     */
    context(_: Context)
    private fun buildFallbackOperatorMethod(op: BuiltinClass.Operator): FunSpec {
        val safeName = safeIdentifier(op.name) // .replace(" ", "_")
        val returnTypeName = typeResolver.resolve(op.returnType)
        val builder = FunSpec
            .builder(safeName)
            .returns(returnTypeName)
            .addCode(body.todoBody())

        // op.description?.takeIf { it.isNotBlank() }?.let { builder.addKdoc(it) }
        builder.addKdoc("\nGodot operator: `%L`", op.name)

        if (op.rightType != null) {
            val rightTypeName = typeResolver.resolve(op.rightType)
            builder.addParameter("other", rightTypeName)
            builder.addModifiers(KModifier.INFIX)
        }

        return builder.build()
    }

    // ── Method generation ─────────────────────────────────────────────────────

    context(_: Context)
    private fun generateMethod(method: BuiltinClass.BuiltinMethod): FunSpec {
        val returnTypeName = method.returnType?.let { typeResolver.resolve(it) } ?: UNIT

        val builder = FunSpec
            .builder(safeIdentifier(method.name))
            .returns(returnTypeName)
            .addCode(body.todoBody())

        /*
        val kdoc = buildKdoc(description = listOfNotNull(method.description))
        if (kdoc.isNotBlank()) builder.addKdoc(kdoc)
         */
        builder.addKdocForBitfield(method.returnType)

        method.arguments.forEach { arg ->
            builder.addParameter(buildParameter(arg))
        }

        return builder.build()
    }

    // ── Parameter building ────────────────────────────────────────────────────

    context(_: Context)
    private fun buildParameter(arg: MethodArg): ParameterSpec {
        val type = typeResolver.resolve(arg)
        val paramBuilder = ParameterSpec.builder(safeIdentifier(arg.name), type)
        arg.defaultValue?.let { _ ->
            // Pass the default value as TODO(); impl layer will fix it
            paramBuilder.defaultValue("%L", "TODO()")
        }
        return paramBuilder.build()
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

    // ── BodyGenerator ──────────────────────────────────────────────────

    /**
     * Responsible solely for generating function/property bodies.
     *
     * Currently produces stubs (`TODO()`). The impl layer will replace
     * these with actual native calls via cinterop.
     */
    class BodyGenerator {
        fun todoBody(): CodeBlock = CodeBlock.of("%M()", K_TODO)

        fun constGetter(value: String): FunSpec = FunSpec.getterBuilder()
            .addCode("return %L", value)
            .build()

        fun todoGetter(): FunSpec = FunSpec.getterBuilder()
            .addCode(todoBody())
            .build()
    }
}
