package io.github.kingg22.godot.api.internal.callable

import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.internal.anyToVariantOrNull
import io.github.kingg22.godot.api.builtin.internal.getValue
import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode
import io.github.kingg22.godot.api.internal.callable.CallableBinding.createCallableCustomInfo2
import io.github.kingg22.godot.api.internal.callable.CallableBinding.createCustom
import io.github.kingg22.godot.api.internal.callable.CallableBinding.storeKotlinCallable
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.pushError
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.VariantBinding
import io.github.kingg22.godot.internal.binding.getInstance
import io.github.kingg22.godot.internal.ffi.GDExtensionCallErrorType
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomInfo2
import io.github.kingg22.godot.internal.ffi.GDExtensionConstVariantPtr
import io.github.kingg22.godot.internal.ffi.GDExtensionConstVariantPtrVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import kotlinx.cinterop.staticCFunction

/**
 * Factory for creating Godot custom Callables from Kotlin lambdas.
 *
 * Usage:
 * ```
 * val callable = CallableFactory.create { println("Hello!") }
 * callable.callDeferred(node, StringName("my_method"))
 * ```
 */
@UsedFromCodegenGeneratedCode
@InternalBinding
public object CallableFactory {
    /** See [Callable] class and factory function */
    @UsedFromCodegenGeneratedCode
    public fun create(lambda: Function<*>, callable: Callable) {
        check(callable.isNull()) { "Callable must be null to create a custom callable" }
        val callableCustomInfo2 = createImpl(lambda)
        createCustom(callableCustomInfo2, callable.rawPtr)
    }

    /** See [Callable] class and factory function. Create the empty Callable */
    public fun create(lambda: Function<*>): Callable = Callable(null).apply { create(lambda, this) }

    // Create the GDExtensionCallableCustomInfo2 struct using cValue
    public fun createImpl(lambda: Function<*>): CValue<GDExtensionCallableCustomInfo2> = createCallableCustomInfo2(
        // Create a KotlinCallable wrapper and store it in a StableRef on userdata
        storeKotlinCallable(lambda),
        staticCFunction {
                userdata,
                arguments: CArrayPointer<GDExtensionConstVariantPtrVar>?,
                argumentCount,
                rReturn,
                rError,
            ->
            val callable = userdata?.getInstance<KotlinCallable>()

            if (callable == null || rReturn == null || rError == null) {
                GD.pushError(
                    "Invalid arguments received from Godot to Callable custom call: $arguments, $argumentCount, $rReturn, $rError, $userdata == $callable",
                )
                return@staticCFunction
            }

            fun fillCallError(
                error: GDExtensionCallErrorType,
                expected: Int = callable.arity().toInt(),
                argument: Int = argumentCount.toInt(),
            ) {
                val callError = rError.pointed
                callError.error = error
                callError.expected = expected
                callError.argument = argument
            }

            if (argumentCount != callable.arity()) {
                GD.pushError(
                    "[kogot]: Error: callable called with $argumentCount arguments, expected ${callable.arity()}",
                )
                fillCallError(
                    if (argumentCount < callable.arity()) {
                        GDEXTENSION_CALL_ERROR_TOO_FEW_ARGUMENTS
                    } else {
                        GDEXTENSION_CALL_ERROR_TOO_MANY_ARGUMENTS
                    },
                )
                return@staticCFunction
            }

            if (argumentCount != 0L && arguments == null) {
                GD.pushError(
                    "[kogot]: Error: callable called with null array of arguments, expected $argumentCount",
                )
                fillCallError(GDEXTENSION_CALL_ERROR_INVALID_ARGUMENT)
                return@staticCFunction
            }

            // FIXME validate arguments order is preserved
            val arguments = Array(argumentCount.toInt()) { i ->
                // FIXME requires apropiate conversion to Kotlin types. Requires supports downcast, Variant, KString
                reinterpretedVariantToKotlin(arguments?.get(i))
            }

            val result: Any? = runCatching { callable.invoke(arguments) }.getOrElse { exception ->
                GD.pushError(
                    "[kogot]: Error calling callable ${callable.arity()} args, fallback to null: $exception",
                )
                fillCallError(GDEXTENSION_CALL_ERROR_INVALID_METHOD)
                return@staticCFunction
            }

            // TODO: properly convert return types to Kotlin types
            // (aka if the user request T, the argument must preserve the type T, not unbox or box unrequested)
            val resultVariant = anyToVariantOrNull(result) ?: run {
                GD.pushError("[kogot]: Error converting callable result ($result) to Variant, fallback to null")
                fillCallError(GDEXTENSION_CALL_ERROR_INVALID_METHOD)
                return@staticCFunction
            }

            fillCallError(GDEXTENSION_CALL_OK, 0, 0)
            VariantBinding.instance.newCopyRaw(rReturn, resultVariant.rawPtr)
        },
    )

    public fun reinterpretedVariantToKotlin(argument: GDExtensionConstVariantPtr?): Any? =
        if (argument == null) null else Variant(argument).getValue()
}
