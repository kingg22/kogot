package io.github.kingg22.godot.codegen.callable.knative

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.services.PackageRegistry

/** Same limit as the `Callable` factories: every typed handler becomes one of them. */
private const val MAX_SIGNAL_ARITY = 23

/**
 * Generates the typed `emit` and `connect` extensions of `TypedSignal`, one overload per arity (0..[MAX_SIGNAL_ARITY]).
 *
 * The receiver `TypedSignal<(P1, ..., Pn) -> Unit>` selects the overload, so the call site needs no type arguments.
 * Type parameters are `reified` (arguments are converted with `toVariant`, handlers through the typed `Callable`
 * factories) and carry `@MustBeVariant`, so the kogot compiler plugin validates them at the call site.
 */
class KotlinNativeSignalGenerator(private val packageRegistry: PackageRegistry) {
    private val typedSignal = packageRegistry.classNameForOrDefault("TypedSignal")
    private val signalConnection = packageRegistry.classNameForOrDefault("SignalConnection")
    private val callable = packageRegistry.classNameForOrDefault("Callable")
    private val mustBeVariant = packageRegistry.classNameForOrDefault("MustBeVariant")
    private val godotError = packageRegistry.classNameForOrDefault("GodotError")
    private val connectFlags = packageRegistry.classNameForOrDefault("GodotObject").nestedClass("ConnectFlags")
    private val toVariant = MemberName(packageRegistry.packageForOrDefault("toVariant"), "toVariant")

    fun generate(): Sequence<FileSpec> = sequence {
        yield(
            createFile("TypedSignals", typedSignal.packageName) {
                // Type variables carry the annotation in their name (see typeVariables), so it needs an explicit import.
                addImport(mustBeVariant.packageName, mustBeVariant.simpleName)
                (0..MAX_SIGNAL_ARITY).forEach { arity ->
                    addFunction(emit(arity))
                    addFunction(connect(arity))
                }
            },
        )
    }

    /**
     * Declaration-site type variables. KotlinPoet only emits TypeVariableName annotations at use sites, but the
     * compiler plugin reads them from the declaration (`<@MustBeVariant reified P1>`); prefixing the name emits it.
     */
    private fun typeVariables(arity: Int) =
        (1..arity).map { i -> TypeVariableName("@${mustBeVariant.simpleName} P$i").copy(reified = true) }

    /** Use-site types (`P1`, ...), without the annotation. */
    private fun parameterTypes(arity: Int) = (1..arity).map { i -> TypeVariableName("P$i") }

    /** `TypedSignal<(P1, ..., Pn) -> Unit>` */
    private fun receiver(arity: Int) = typedSignal.parameterizedBy(handlerType(arity))

    private fun handlerType(arity: Int) =
        LambdaTypeName.get(parameters = parameterTypes(arity).toTypedArray(), returnType = UNIT)

    private fun FunSpec.Builder.typed(arity: Int) = apply {
        receiver(receiver(arity))
        if (arity > 0) {
            addModifiers(KModifier.INLINE)
            addTypeVariables(typeVariables(arity))
        }
    }

    private fun emit(arity: Int): FunSpec {
        val names = (1..arity).map { "p$it" }
        val builder = FunSpec
            .builder("emit")
            .apply {
                if (arity == 0) {
                    addKdoc("Emits this signal, which has no arguments.\n")
                } else {
                    addKdoc(
                        "Emits this signal with %L argument(s), converting each one to a [Variant][%T].\n",
                        arity,
                        packageRegistry.classNameForOrDefault("Variant"),
                    )
                }
            }
            .addKdoc("\n@return the [%T] Godot reports for the emission.", godotError)
            .typed(arity)
            .returns(godotError)
        names.zip(parameterTypes(arity)).forEach { (name, type) -> builder.addParameter(name, type) }
        return if (arity == 0) {
            builder.addStatement("return emitArguments(emptyArray(), emptyArray())").build()
        } else {
            builder
                .addStatement(
                    "return emitArguments(arrayOf<Any?>(%L), arrayOf(%L))",
                    names.joinToString(", "),
                    names.map { CodeBlock.of("%N.%M()", it, toVariant) }.joinToCode(", "),
                )
                .build()
        }
    }

    private fun connect(arity: Int): FunSpec = FunSpec
        .builder("connect")
        .addKdoc(
            """
            Connects [handler] to this signal. Close the returned [%T] to disconnect it.

            Each argument Godot passes is converted to the handler's parameter type.
            `APPEND_SOURCE_OBJECT` is rejected: it would pass an argument the handler does not declare.

            @param flags connection flags, e.g. `DEFERRED` or `ONE_SHOT`.
            @throws IllegalStateException when Godot rejects the connection.
            """.trimIndent(),
            signalConnection,
        )
        .typed(arity)
        .addParameter("flags", connectFlags, KModifier.VARARG)
        .addParameter(
            ParameterSpec
                .builder("handler", handlerType(arity))
                .apply { if (arity > 0) addModifiers(KModifier.NOINLINE) }
                .build(),
        )
        .returns(signalConnection)
        .addStatement("return connectCallable(%T(handler), flags)", callable)
        .build()
}
