package io.github.kingg22.godot.codegen.callable.knative

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.services.PackageRegistry
import io.github.kingg22.godot.codegen.types.K_SUPPRESS

private const val MAX_CALLABLE_ARITY = 23
private val K_OPT_IN = ClassName("kotlin", "OptIn")
private val K_PUBLISHED_API = ClassName("kotlin", "PublishedApi")
private val ANY_NULLABLE = ANY.copy(nullable = true)

/**
 * Generates the typed `Callable { ... }` factories (one overload per arity, 0..[MAX_CALLABLE_ARITY]).
 *
 * Each overload is `inline` with `reified` parameter types, so the call site knows the lambda's declared types
 * and converts every Godot argument to them (e.g. a Godot `INT`, always a [Long], into an [Int] parameter).
 * Type parameters carry `@MustBeVariant`, so the kogot compiler plugin validates them at the call site.
 */
class KotlinNativeCallableGenerator(private val packageRegistry: PackageRegistry) {
    private val callable = packageRegistry.classNameForOrDefault("Callable")
    private val variant = packageRegistry.classNameForOrDefault("Variant")
    private val mustBeVariant = packageRegistry.classNameForOrDefault("MustBeVariant")
    private val getValue = MemberName(packageRegistry.packageForOrDefault("getValue"), "getValue")
    private val callableFactory = packageRegistry.classNameForOrDefault("CallableFactory")
    private val internalBinding = packageRegistry.classNameForOrDefault("InternalBinding")

    /** `(Array<Variant>) -> Any?`: the arguments every factory hands to [callableOf]. */
    private val invokerType = LambdaTypeName.get(
        parameters = arrayOf(ARRAY.parameterizedBy(variant)),
        returnType = ANY_NULLABLE,
    )

    fun generate(): Sequence<FileSpec> = sequence {
        yield(
            createFile("CallableFactories", packageRegistry.packageForOrDefault("Callable")) {
                addFunction(callableOf())
                addFunction(toCallableArgument())
                addFunction(copyForCallable())
                (0..MAX_CALLABLE_ARITY).forEach { arity -> addFunction(factory(arity)) }
            },
        )
    }

    /** Non-inline bridge: owns the argument Variants (closed after the call) and hides the FFI types. */
    private fun callableOf() = FunSpec
        .builder("callableOf")
        .addModifiers(KModifier.INTERNAL)
        .addAnnotation(K_PUBLISHED_API)
        .addAnnotation(AnnotationSpec.builder(K_OPT_IN).addMember("%T::class", internalBinding).build())
        .addParameter("arity", INT)
        .addParameter("invoker", invokerType)
        .returns(callable)
        .beginControlFlow("return %T.create(arity) { pointers ->", callableFactory)
        .addStatement("val arguments = Array(pointers.size) { i -> pointers[i]?.let(::%T) ?: %T() }", variant, variant)
        .beginControlFlow("try")
        .addStatement("invoker(arguments)")
        .nextControlFlow("finally")
        .addStatement("arguments.forEach(%T::close)", variant)
        .endControlFlow()
        .endControlFlow()
        .build()

    /** Converts one argument to the lambda's declared type; `Variant` parameters get their own copy. */
    private fun toCallableArgument(): FunSpec {
        val t = TypeVariableName("T").copy(reified = true)
        return FunSpec
            .builder("toCallableArgument")
            .addModifiers(KModifier.INTERNAL, KModifier.INLINE)
            .addAnnotation(K_PUBLISHED_API)
            .addAnnotation(AnnotationSpec.builder(K_SUPPRESS).addMember("%S", "UNCHECKED_CAST").build())
            .addTypeVariable(t)
            .receiver(variant)
            .returns(t)
            .addStatement(
                "return when {\n⇥%T::class == %T::class -> copyForCallable() as T\nisNil() && null is T -> null as T\nelse -> %M<T>()\n⇤}",
                t,
                variant,
                getValue,
            )
            .build()
    }

    private fun copyForCallable() = FunSpec
        .builder("copyForCallable")
        .addModifiers(KModifier.INTERNAL)
        .addAnnotation(K_PUBLISHED_API)
        .addKdoc("The argument Variants are closed after the call, so a `Variant` parameter must own a copy.")
        .receiver(variant)
        .returns(variant)
        .addStatement("return %T(rawPtr)", variant)
        .build()

    private fun factory(arity: Int): FunSpec {
        // KotlinPoet only emits TypeVariableName annotations at use sites, but the compiler plugin reads them from
        // the declaration (`<@MustBeVariant reified P1>`). Prefixing the name emits the annotation in both places;
        // no import is needed because MustBeVariant lives in the generated file's own package.
        require(mustBeVariant.packageName == callable.packageName) { "MustBeVariant must share the factories' package" }
        val annotated = "@${mustBeVariant.simpleName}"
        val parameterTypes = (1..arity).map { i -> TypeVariableName("$annotated P$i").copy(reified = true) }
        val returnType = TypeVariableName("$annotated R")
        val lambdaType = LambdaTypeName.get(parameters = parameterTypes.toTypedArray(), returnType = returnType)

        return FunSpec
            .builder("Callable")
            .addKdoc(
                """
                Creates a Godot [%T] from a Kotlin lambda with %L argument(s).

                Every argument Godot passes is converted to the lambda's declared parameter type: Godot `INT` and
                `FLOAT` values may be received as [Int]/[Short]/[Byte] and [Float], and `STRING` as [String].
                Parameter and return types must be Variant compatible (see [%T]); the kogot compiler plugin checks it.
                """.trimIndent(),
                callable,
                arity,
                mustBeVariant,
            )
            .addTypeVariables(parameterTypes + returnType)
            .apply { if (arity > 0) addModifiers(KModifier.INLINE) }
            .addParameter(
                ParameterSpec
                    .builder("lambda", lambdaType)
                    .apply { if (arity > 0) addModifiers(KModifier.NOINLINE) }
                    .build(),
            )
            .returns(callable)
            .addStatement(
                "return callableOf(%L) { %Llambda(%L) }",
                arity,
                if (arity > 0) "args -> " else "",
                (0 until arity).joinToString(", ") { "args[$it].toCallableArgument()" },
            )
            .build()
    }
}
