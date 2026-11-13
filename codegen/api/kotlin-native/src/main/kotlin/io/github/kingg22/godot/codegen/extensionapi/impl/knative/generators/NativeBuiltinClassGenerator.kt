package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.interceptors.PackedListConfig
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.impl.BuiltinClassImplGen
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.impl.buildLayoutConstants
import io.github.kingg22.godot.codegen.extensionapi.resolver.addKdocIfPresent
import io.github.kingg22.godot.codegen.extensionapi.resolver.experimentalApiAnnotation
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinClass
import io.github.kingg22.godot.codegen.types.K_AUTOCLOSEABLE
import io.github.kingg22.godot.codegen.utils.debug
import io.github.kingg22.godot.codegen.utils.filterValuesNotNull
import io.github.kingg22.godot.codegen.utils.logger
import io.github.kingg22.godot.codegen.utils.warning

/**
 * Generates Kotlin Native class declarations for Godot builtin classes
 * (Vector2, Color, Array, Dictionary, etc).
 *
 * Responsibilities of this class:
 * - Class structure: name, supertype, modifiers, kdoc
 * - Members: fields, constants, method signatures, operator signatures, constructors
 * - Companion object with static methods and constants
 *
 * Types that are NOT generated as classes (primitives/nil):
 * [NativeBuiltinClassGenerator.SKIPPED_TYPES] — these are handled via typealiases or extension functions elsewhere.
 */
class NativeBuiltinClassGenerator(
    private val typeResolver: TypeResolver,
    private val body: BuiltinClassImplGen,
    private val defaultValueGenerator: DefaultValueGenerator,
    private val methodGen: NativeMethodGenerator,
    private val overloadGen: TypeOverloadGenerator,
    private val enumGenerator: NativeEnumGenerator,
    private val genericInterceptor: GenericBuiltinInterceptor,
    private val typeAliasGenerator: TypeAliasGenerator,
) {
    private val logger = logger()

    companion object {
        /**
         * Godot builtin "types" that map to Kotlin primitives — no class is generated for these.
         * Operators/constructors for them are handled via extension functions or typealiases.
         */
        val SKIPPED_TYPES = setOf("int", "float", "bool", "nil", "double")

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
        val genericConfig = genericInterceptor.getGenericConfig(builtinClass.raw)
        return createFile(spec.name!!, context.packageForOrDefault(godotName)) {
            typeAliasGenerator.generateTypeAliasSpec(builtinClass.raw)?.let { addTypeAlias(it) }
            addType(spec)
            addFunctions(buildCollectionFactories(builtinClass, spec.name!!, genericConfig))
            addProperties(body.buildTopLevelFptrProperties(builtinClass, utilMembers))
        }
    }

    /**
     * Public `inline` factories for the constructors of the generic collections (`GodotArray<T>()`,
     * `GodotArray<T>(from)`, `Dictionary<K, V>()`...). The constructors themselves are `internal`: they need an
     * `ElementCodec`, and the factories pick it from the reified types so it always matches.
     *
     * The typed constructor (type, class name and script as arguments) has no factory: the factories type the
     * collection from `T` instead.
     */
    context(context: Context)
    private fun buildCollectionFactories(
        builtinClass: ResolvedBuiltinClass,
        kotlinName: String,
        genericConfig: GenericBuiltinInterceptor.GenericConfig?,
    ): List<FunSpec> {
        if (genericConfig == null || genericConfig.typeVariableNames.isEmpty()) return emptyList()
        val typeNames = genericConfig.typeVariableNames
        val className = ClassName(context.packageForOrDefault(builtinClass.name), kotlinName)
        val returnType = className.parameterizedBy(typeNames.map { TypeVariableName(it) })
        // `ElementCodec.of<T>()` for each type variable, with the class resolved through the package registry.
        val elementCodec = context.classNameForOrDefault("ElementCodec")
        val codecs = typeNames.map { CodeBlock.of("%T.of<%L>()", elementCodec, it) }
        val applyGodotType = context.memberNameForOrDefault("applyGodotType", isExtension = true)

        return builtinClass.constructors
            .filter { it.raw != null && it.arguments.size <= 1 }
            .map { ctor ->
                val parameters = ctor.arguments.map { methodGen.buildParameter(it) }
                FunSpec
                    .builder(kotlinName)
                    .addKdocIfPresent(ctor.raw!!)
                    .addModifiers(KModifier.INLINE)
                    // Prefixed names: see `contractedTypeVariable`.
                    .addTypeVariables(
                        typeNames.map { type ->
                            GenericBuiltinInterceptor.contractedTypeVariable(type).copy(reified = true)
                        },
                    )
                    .addParameters(parameters)
                    .returns(returnType)
                    .addCode(
                        buildCodeBlock {
                            val arguments = (parameters.map { CodeBlock.of("%N", it) } + codecs).joinToCode()
                            // A new empty collection is typed in the engine; one built from other data keeps its content.
                            if (parameters.isEmpty()) {
                                addStatement("return %T(%L).%M()", className, arguments, applyGodotType)
                            } else {
                                addStatement("return %T(%L)", className, arguments)
                            }
                        },
                    )
                    .build()
            }
    }

    /** Generates the [TypeSpec] for [builtinClass], or null if it belongs to [NativeBuiltinClassGenerator.SKIPPED_TYPES]. */
    context(context: Context)
    fun generate(builtinClass: ResolvedBuiltinClass): TypeSpec? {
        val raw = builtinClass.raw
        if (builtinClass.name.lowercase() in SKIPPED_TYPES) return null
        val requiresGenerics = genericInterceptor.requiresGenerics(raw)

        val kotlinName = builtinClass.name.renameGodotClass(requiresGenerics)

        // ── GENERIC INTERCEPTION ──────────────────────────────────────────────
        val genericConfig = genericInterceptor.getGenericConfig(raw)
        val codecParameters = genericConfig?.codecParameters.orEmpty()

        val classBuilder = TypeSpec
            .classBuilder(kotlinName)
            .experimentalApiAnnotation(builtinClass.name)
            .addKdocIfPresent(raw)
            .let { body.configureStorageBackedBuiltin(builtinClass, it, genericConfig?.superclass, codecParameters) }
        genericConfig?.let { classBuilder.addTypeVariables(it.typeVariables) }

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
        val isString = builtinClass.name == "String"
        val haveStringConstructor = isString || builtinClass.name == "StringName" || builtinClass.name == "NodePath"

        // ── Constructors ─────────────────────────────────────────────────────
        // Godot constructors become secondary constructors (or companion factory funs if static-style).
        // The primary constructor only wraps the storage (see `configureStorageBackedBuiltin`).
        // Index 0 is always the no-arg constructor.
        val constructorsSpecs = builtinClass.constructors.filter { it.raw != null }.flatMap { ctor ->
            val ctorBuilder = FunSpec
                .constructorBuilder()
                .addKdocIfPresent(ctor.raw!!)
                .let { body.configureStorageBackedSecondaryCtor(builtinClass, it, codecParameters) }
                .addParameters(ctor.arguments.map { arg -> methodGen.buildParameter(arg) })
                .addParameters(codecParameters)
                .addCode(body.constructorBodyFor(builtinClass, ctor))
                .build()

            buildList {
                add(ctorBuilder)
                if (!haveStringConstructor) {
                    overloadGen.buildOverloadsForConstructor(
                        ctor.raw!!,
                        ctorBuilder,
                        TypeOverloadGenerator.GodotStringMapping,
                    )?.let(::add)
                }
            }
        }
        classBuilder.addFunctions(constructorsSpecs)

        // ── Special constructors for String types ─────────────────────────────
        if (haveStringConstructor) {
            classBuilder.addFunction(
                FunSpec
                    .constructorBuilder()
                    .addParameter("value", STRING) // kotlin.String
                    .addKdoc("Creates a %L from a Kotlin String.", kotlinName)
                    .addCode(body.stringConstructorBodyFor(builtinClass))
                    .let { body.configureStorageBackedSecondaryCtor(builtinClass, it) }
                    .build(),
            )
        }

        if (builtinClass.name == "Callable") classBuilder.addFunction(body.callableCustomConstructorFor())

        // ── Operators ────────────────────────────────────────────────────────
        classBuilder.addFunctions(generateOperators(builtinClass, genericConfig))

        // A List is equal to any List with the same elements, and its hash agrees: the engine's own `==` and
        // `hash()` only know Godot values, so for the packed arrays `list == listOf(...)` comes from AbstractList.
        if (genericConfig is PackedListConfig) {
            classBuilder.funSpecs.removeAll { fn -> fn.name == "equals" || fn.name == "hashCode" }
        }

        // ── Instance methods ──────────────────────────────────────────────────
        val (staticMethods, instanceMethods) = raw.methods.partition { it.isStatic }

        val methodSpecs = instanceMethods.flatMap { method ->
            val specs = buildMethodWithGenericTransform(method, builtinClass.name, genericConfig)

            if (raw.operators.any { compareMethodOperator(method, it) }) {
                val existingOperator = raw.operators.first { compareMethodOperator(method, it) }
                logger.debug {
                    "Skipping operator overload for ${builtinClass.name}.${method.name}(${existingOperator.rightType}): ${existingOperator.returnType} because it's already defined as operator"
                }
                emptyList()
            } else {
                specs
            }
        }
        classBuilder.addFunctions(methodSpecs)

        if (isString) classBuilder.addFunctions(body.buildToKStringConverters())

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
        val staticMethodSpecs = staticMethods.flatMap { method ->
            val original = methodGen.buildMethod(
                method,
                builtinClass.name,
                codeBody = body.buildMethodBody(method, builtinClass.name),
            )
            overloadGen.buildOverloadsForMethod(
                method,
                original,
                TypeOverloadGenerator.GodotStringMapping,
            ).ifEmpty { listOf(original) }
        }
        companionBuilder.addFunctions(staticMethodSpecs)

        if (isString) companionBuilder.addFunctions(body.buildStaticStringConverters())

        val companionHasContent = companionBuilder.funSpecs.isNotEmpty() || companionBuilder.propertySpecs.isNotEmpty()

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
    context(ctx: Context)
    private fun buildMethodWithGenericTransform(
        method: BuiltinClass.BuiltinMethod,
        className: String,
        genericConfig: GenericBuiltinInterceptor.GenericConfig?,
    ): List<FunSpec> {
        // Si no hay config genérica, usar generador normal
        val original: FunSpec
        if (genericConfig == null) {
            original = methodGen.buildMethod(method, className, codeBody = body.buildMethodBody(method, className)) {
                if (method.name == "get" || method.name == "set") addModifiers(KModifier.OPERATOR)
            }
        } else {
            // Resolver tipo de retorno original
            val originalReturnType = method.returnType?.let { typeResolver.resolve(it) }

            // Transformar con config genérica
            val transformedReturnType = genericConfig.transformReturnType(method, originalReturnType)

            // Construir método con tipo transformado
            original = methodGen.buildMethod(method, className, codeBody = body.buildMethodBody(method, className)) {
                if (method.name == "get" || method.name == "set") addModifiers(KModifier.OPERATOR)

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

        // The engine methods that implement the abstract members of the collection view: `override`, no String overloads.
        val viewName = genericConfig?.viewMethods?.get(method.name)
        if (viewName != null) {
            val signature = genericConfig.viewSignature(method.name)
            if (signature == null) return listOf(original.asViewMember(viewName))
            // Several overloads (String): all are kept, the one whose types match the abstract member overrides it.
            val candidates = overloadGen
                .buildOverloadsForMethod(method, original, TypeOverloadGenerator.GodotStringMapping)
                .ifEmpty { listOf(original) }
            val member = candidates.first {
                it.parameters.map { p -> p.type } == signature.parameters &&
                    (signature.returns == null || it.returnType == signature.returns)
            }
            return candidates.map { if (it === member) it.asViewMember(viewName) else it }
        }

        // String overloads if needed
        return overloadGen.buildOverloadsForMethod(method, original, TypeOverloadGenerator.GodotStringMapping).ifEmpty {
            listOf(original)
        }
    }

    /**
     * This engine method as the implementation of an abstract member of the collection view: renamed to [name],
     * `override` and with no `operator` and no default values, which an override cannot declare again.
     */
    private fun FunSpec.asViewMember(name: String): FunSpec = toBuilder(name)
        .apply {
            modifiers.remove(KModifier.OPERATOR)
            modifiers += KModifier.OVERRIDE
            modifiers.remove(KModifier.INLINE) // an override is final, not inlined
            parameters.replaceAll { it.toBuilder().defaultValue(null as CodeBlock?).build() }
        }.build()

    // ── Operator generation ───────────────────────────────────────────────────

    context(ctx: Context)
    private fun generateOperators(
        resolvedClass: ResolvedBuiltinClass,
        genericConfig: GenericBuiltinInterceptor.GenericConfig?,
    ): List<FunSpec> {
        val builtinClass = resolvedClass.raw
        var compareToGenerated = false
        var equalsGenerated = false

        // Group operators by symbol to handle overloads (e.g., * with int and with Vector2)
        return builtinClass.operators.mapNotNull { op ->
            val symbol = op.name
            val kotlinOpName = OPERATOR_MAP[symbol]

            val operator = when {
                // compareTo covers <, <=, >, >= — generate once, first occurrence wins
                symbol in COMPARE_OPERATORS -> {
                    if (compareToGenerated) return@mapNotNull null
                    compareToGenerated = true
                    buildCompareToOperator(resolvedClass)
                }

                // != is derived from equals in Kotlin — skip
                symbol == "!=" -> return@mapNotNull null

                // Recognized operator with a Kotlin keyword
                kotlinOpName != null -> {
                    if (kotlinOpName == "equals") {
                        if (equalsGenerated) {
                            return@mapNotNull null
                        } else {
                            equalsGenerated = true
                        }
                    }

                    val opFun = buildKotlinOperator(
                        name = kotlinOpName,
                        rightType = op.rightType,
                        returnType = op.returnType,
                        genericConfig = genericConfig,
                        operator = op,
                        cls = resolvedClass,
                    ) {
                        if (kotlinOpName == "equals") {
                            kdoc.clear()
                            val equalsOps = resolvedClass.raw.operators.filter { it.name == "==" }
                            equalsOps.forEachIndexed { index, operator ->
                                if (index != 0) addKdoc("\n\n")
                                addKdoc("**In case of**: [%L]\n\n", operator.rightType!!)
                                addKdocIfPresent(operator)
                            }
                        }
                    }

                    if (kotlinOpName == "equals") {
                        return@mapNotNull listOf(
                            opFun,
                            FunSpec
                                .builder("hashCode")
                                .addModifiers(KModifier.OVERRIDE)
                                .returns(INT)
                                .addCode(body.buildHashCodeBody(resolvedClass))
                                .apply {
                                    val hasMethod = resolvedClass.raw.methods.any { it.name == "hash" }
                                    val targetClass = if (hasMethod) {
                                        typeResolver.resolve(resolvedClass.shortName)
                                    } else {
                                        ctx.classNameForOrDefault("Variant")
                                    }
                                    addKdoc("Refers to [%T.hash] method", targetClass)
                                }
                                .build(),
                            FunSpec
                                .builder("toString")
                                .addModifiers(KModifier.OVERRIDE)
                                .returns(STRING)
                                .addCode(body.buildToStringBody())
                                .addKdoc(
                                    "Returns the Godot string representation via [%T.stringify]",
                                    ctx.classNameForOrDefault("Variant"),
                                )
                                .build(),
                        )
                    } else {
                        opFun
                    }
                }

                // Unknown operator — delegate to named method (infix if binary)
                else -> {
                    logger.warning {
                        "Unknown operator found ${builtinClass.name}.${op.name}(${op.rightType.orEmpty()}): ${op.returnType}"
                    }
                    buildFallbackOperatorMethod(op, builtinClass.name)
                }
            }
            return@mapNotNull listOf(operator)
        }.flatten()
    }

    /** Builds a standard Kotlin operator fun (plus, minus, times, equals, not, etc). */
    context(_: Context)
    private fun buildKotlinOperator(
        name: String,
        rightType: String?,
        returnType: String,
        genericConfig: GenericBuiltinInterceptor.GenericConfig?,
        operator: BuiltinClass.Operator,
        cls: ResolvedBuiltinClass,
        block: FunSpec.Builder.() -> Unit = {},
    ): FunSpec {
        val originalReturnType = typeResolver.resolve(returnType)
        val returnTypeName = genericConfig?.transformOperatorReturnType(operator, originalReturnType)
            ?: originalReturnType

        val builder = FunSpec
            .builder(name)
            .addKdocIfPresent(operator)

        if (name == "equals") {
            // Must override Any.equals — parameter is Any? not the specific type
            builder.addModifiers(KModifier.OVERRIDE)
            builder.returns(BOOLEAN)
            builder.addParameter("other", ANY.copy(nullable = true))
            builder.addCode(body.buildEqualsOperatorBody(cls))
        } else {
            builder.addModifiers(KModifier.OPERATOR)
            builder.returns(returnTypeName)
            if (rightType != null) {
                val rightTypeName = typeResolver.resolve(rightType)
                builder.addParameter("other", rightTypeName)
            }
            builder.addCode(body.buildOperatorBody(operator))
        }

        return builder.apply(block).build()
    }

    /**
     * Builds `override fun compareTo(other: T): Int` for ordering operators.
     * Kotlin derives <, <=, >, >= from a single compareTo.
     */
    context(_: Context)
    private fun buildCompareToOperator(resolvedClass: ResolvedBuiltinClass): FunSpec {
        val selfType = typeResolver.resolve(resolvedClass.name)
        return FunSpec
            .builder("compareTo")
            .addModifiers(KModifier.OPERATOR)
            .addParameter("other", selfType)
            .returns(INT)
            .addKdoc("Supports `<`, `<=`, `>`, `>=` operators via Kotlin compareTo convention.")
            .addCode(body.buildCompareToBody(resolvedClass))
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
        val safeName = safeIdentifier(op.name)
        val returnTypeName = typeResolver.resolve(op.returnType)
        val builder = FunSpec
            .builder(safeName)
            .returns(returnTypeName)
            .addCode(body.buildOperatorBody(op))
            .experimentalApiAnnotation(className, op.name)
            .addKdocIfPresent(op)
            .addKdoc("\nGodot operator: `%L`", op.name)

        if (op.rightType != null) {
            val rightTypeName = typeResolver.resolve(op.rightType!!)
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
