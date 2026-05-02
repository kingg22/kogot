@file:OptIn(ExperimentalGodotKotlin::class) // safety, or throws an acceptable exception

package io.github.kingg22.godot.internal.callback

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.MustBeVariant
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.asVariant
import io.github.kingg22.godot.api.builtin.getValue
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.pushError
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.CallableBinding
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.VariantBinding
import io.github.kingg22.godot.internal.binding.getInstance
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomCall
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomInfo2
import io.github.kingg22.godot.internal.ffi.GDExtensionConstVariantPtr
import io.github.kingg22.godot.internal.ffi.GDExtensionConstVariantPtrVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.cValue
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
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
@InternalBinding
public object CallableFactory {
    /**
     * Creates a Godot Callable from a Kotlin lambda with arguments [variant type compatible][MustBeVariant].
     *
     * The return type of the lambda must be a [Variant type compatible][MustBeVariant].
     * Allows [Unit] as return type, is converted to [Variant.Type.NIL] internally.
     *
     * @param lambda The Kotlin lambda to wrap
     * @return A Callable that wraps the Kotlin lambda
     */
    public fun create(lambda: Function<@MustBeVariant Any?>): Callable {
        // Create the GDExtensionCallableCustomInfo2 struct using cValue
        val callableCustomInfo2 = createCallableCustomInfo2(
            // Create a KotlinCallable wrapper and store it in a StableRef on userdata
            storeKallable(wrapLambda(lambda)),
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

                val callError = rError[0]

                if (argumentCount != callable.arity()) {
                    GD.pushError(
                        "[kogot]: Error: callable called with $argumentCount arguments, expected ${callable.arity()}",
                    )
                    callError.error = if (argumentCount < callable.arity()) {
                        GDEXTENSION_CALL_ERROR_TOO_FEW_ARGUMENTS
                    } else {
                        GDEXTENSION_CALL_ERROR_TOO_MANY_ARGUMENTS
                    }
                    callError.expected = callable.arity().toInt()
                    callError.argument = argumentCount.toInt()
                    return@staticCFunction
                }

                if (argumentCount != 0L && arguments == null) {
                    GD.pushError(
                        "[kogot]: Error: callable called with null array of arguments, expected $argumentCount",
                    )
                    callError.error = GDEXTENSION_CALL_ERROR_INVALID_ARGUMENT
                    callError.expected = callable.arity().toInt()
                    callError.argument = argumentCount.toInt()
                    return@staticCFunction
                }

                val arguments = (0 until argumentCount).map { i ->
                    // FIXME requires apropiate conversion to Kotlin types. Requires supports downcast, Variant, KString
                    reinterpretedVariantToKotlin(arguments?.get(i))
                }.toTypedArray()

                // TODO: properly convert arguments to Kotlin types using Variant
                // (aka if the user request T, the argument must preserve the type T, not unbox or box unrequested)
                val result = runCatching { callable.call(*arguments) }.getOrElse { exception ->
                    GD.pushError(
                        "[kogot]: Error calling callable ${callable.arity()} args, fallback to null: $exception",
                    )
                    null
                }

                val resultVariant = when (result) {
                    null, is Unit -> Variant()

                    is Variant -> result

                    else -> runCatching { result.asVariant() }.getOrElse {
                        GD.pushError(
                            "[kogot]: Error converting callable result ($result) to Variant, fallback to null: $it",
                        )
                        Variant()
                    }
                }

                VariantBinding.instance.newCopyRaw(rReturn, resultVariant.rawPtr)
            },
        )

        // Create the empty Callable
        val callable = Callable()

        // Call the low-level Godot function to create custom callable and fill the callable object
        memScoped {
            CallableBinding.instance.customCreate2Raw(
                callable.rawPtr,
                callableCustomInfo2.ptr,
            )
        }

        return callable
    }

    public fun storeKallable(callable: KotlinCallable): COpaquePointer = StableRef.create(callable).asCPointer()

    @Suppress("UNCHECKED_CAST")
    public fun wrapLambda(lambda: Function<*>): KotlinCallable = when (lambda) {
        is Function0<*> -> Callable0(lambda)

        is Function1<*, *> -> {
            val lambda = lambda as Function1<Any?, *>
            Callable1(lambda)
        }

        else -> error("Unsupported lambda type: ${lambda::class}")
    }

    public fun reinterpretedVariantToKotlin(argument: GDExtensionConstVariantPtr?): Any? =
        if (argument == null) null else Variant(argument).getValue()

    /**
     * Creates a [GDExtensionCallableCustomInfo2] struct.
     * Delegates to [CallableTrampolines]
     * @param userdata Expects a [KotlinCallable] as [COpaquePointer]
     * @param call The custom function to call this callable
     * @param objectId 0 means _custom_, not bind to object
     */
    public fun createCallableCustomInfo2(
        userdata: COpaquePointer,
        call: GDExtensionCallableCustomCall,
        objectId: ULong = 0uL,
    ): CValue<GDExtensionCallableCustomInfo2> = cValue {
        object_id = objectId
        callable_userdata = userdata
        call_func = call
        token = BindingProcAddressHolder.library
        is_valid_func = CallableTrampolines.isValidFunc
        free_func = CallableTrampolines.freeFunc
        hash_func = CallableTrampolines.hashFunc
        equal_func = CallableTrampolines.equalFunc
        less_than_func = CallableTrampolines.lessThanFunc
        to_string_func = CallableTrampolines.toStringFunc
        get_argument_count_func = CallableTrampolines.getArgumentCountFunc
    }
}
