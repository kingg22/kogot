package io.github.kingg22.godot.api.internal.callable

import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.internal.anyToVariant
import io.github.kingg22.godot.api.builtin.internal.getValue
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.pushError
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.CallableBinding
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.VariantBinding
import io.github.kingg22.godot.internal.binding.getInstance
import io.github.kingg22.godot.internal.ffi.GDExtensionCallErrorType
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
@InternalBinding
public object CallableFactory {
    /** See [Callable] class and factory function */
    public fun create(lambda: Function<*>, callable: Callable) {
        check(callable.isNull()) { "Callable must be null to create a custom callable" }
        val callableCustomInfo2 = createImpl(lambda)
        // Call the low-level Godot function to create custom callable and fill the callable object
        memScoped {
            CallableBinding.instance.customCreate2Raw(
                callable.rawPtr,
                callableCustomInfo2.ptr,
            )
        }
    }

    /** See [Callable] class and factory function. Create the empty Callable */
    public fun create(lambda: Function<*>): Callable = Callable().apply { create(lambda, this) }

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

            fun fillCallError(error: GDExtensionCallErrorType) {
                val callError = rError.pointed
                callError.error = error
                callError.expected = callable.arity().toInt()
                callError.argument = argumentCount.toInt()
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
            val resultVariant = runCatching { anyToVariant(result) }.getOrElse { exception ->
                GD.pushError(
                    "[kogot]: Error converting callable result ($result) to Variant, fallback to null: $exception",
                )
                fillCallError(GDEXTENSION_CALL_ERROR_INVALID_METHOD)
                return@staticCFunction
            }

            VariantBinding.instance.newCopyRaw(rReturn, resultVariant.rawPtr)
        },
    )

    public fun storeKotlinCallable(lambda: Function<*>): COpaquePointer =
        StableRef.create(wrapLambda(lambda)).asCPointer()

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
