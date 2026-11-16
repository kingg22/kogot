package io.github.kingg22.godot.api.internal.callable

import io.github.kingg22.godot.api.GodotNative
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.CallableBinding
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.VariantBinding
import io.github.kingg22.godot.internal.binding.getInstance
import io.github.kingg22.godot.internal.binding.guardCallback
import io.github.kingg22.godot.internal.ffi.GDExtensionCallError
import io.github.kingg22.godot.internal.ffi.GDExtensionCallErrorType
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomCall
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomInfo2
import io.github.kingg22.godot.internal.ffi.GDExtensionConstVariantPtr
import io.github.kingg22.godot.internal.ffi.GDExtensionConstVariantPtrVar
import io.github.kingg22.godot.internal.ffi.GDExtensionInt
import io.github.kingg22.godot.internal.ffi.GDExtensionVariantPtr
import kotlinx.cinterop.*

/**
 * This is a low-level binding for custom callables. You must use `CallabeFactory` to create custom callables or use the `Callable` class.
 *
 * The initialization of `CallableFactory` link properties of this class. Must not be invoked before that happens,
 * otherwise the binding throws errors with null values.
 *
 * Arguments reach the [KotlinCallable] as raw pointers; the typed `Callable { ... }` factories convert them.
 *
 * Currently, all callables are `static` for Godot, missing linking to the object. This can lead to incorrect freeing.
 */
@InternalBinding
public object CallableBinding {
    public var onError: (message: String) -> Unit = ::println
    public var onToVariant: (Any?) -> GodotNative? = { null }

    // Create the GDExtensionCallableCustomInfo2 struct using cValue
    public fun createCustomInfo2(
        callable: KotlinCallable,
        objectId: ULong = 0uL,
    ): CValue<GDExtensionCallableCustomInfo2> = createCustomInfo2(
        // Store the KotlinCallable in a StableRef on userdata
        StableRef.create(callable).asCPointer(),
        createCustomCall(),
        objectId,
    )

    /**
     * The `call_func` of every custom callable. Runs on whatever thread calls the `Callable` (a `WorkerThreadPool`
     * task, a `Thread`, a signal emitted from a thread), so any failure, including one in the error reporting or the
     * result conversion, is reported and answered as `INVALID_METHOD` instead of aborting the process (issue #113).
     */
    public fun createCustomCall(): GDExtensionCallableCustomCall = staticCFunction {
            userdata: COpaquePointer?,
            arguments: CArrayPointer<GDExtensionConstVariantPtrVar>?,
            argumentCount: GDExtensionInt,
            rReturn: GDExtensionVariantPtr?,
            rError: CPointer<GDExtensionCallError>?,
        ->
        val completed = guardCallback("Callable.call", false) {
            dispatch(userdata, arguments, argumentCount, rReturn, rError)
            true
        }
        if (!completed) rError?.pointed?.error = GDExtensionCallErrorType.GDEXTENSION_CALL_ERROR_INVALID_METHOD
    }

    private fun dispatch(
        userdata: COpaquePointer?,
        arguments: CArrayPointer<GDExtensionConstVariantPtrVar>?,
        argumentCount: GDExtensionInt,
        rReturn: GDExtensionVariantPtr?,
        rError: CPointer<GDExtensionCallError>?,
    ) {
        val callable = userdata?.getInstance<KotlinCallable>()

        if (callable == null || rReturn == null || rError == null) {
            onError(
                "Invalid arguments received from Godot to Callable custom call: $arguments, $argumentCount, $rReturn, $rError, $userdata == $callable",
            )
            return
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
            onError("[kogot]: Error: callable called with $argumentCount arguments, expected ${callable.arity()}")
            fillCallError(
                if (argumentCount < callable.arity()) {
                    GDEXTENSION_CALL_ERROR_TOO_FEW_ARGUMENTS
                } else {
                    GDEXTENSION_CALL_ERROR_TOO_MANY_ARGUMENTS
                },
            )
            return
        }

        if (argumentCount != 0L && arguments == null) {
            onError("[kogot]: Error: callable called with null array of arguments, expected $argumentCount")
            fillCallError(GDEXTENSION_CALL_ERROR_INVALID_ARGUMENT)
            return
        }

        val arguments = Array(argumentCount.toInt()) { i -> arguments?.get(i) }

        val result: Any? = runCatching { callable.call(arguments) }.getOrElse { exception ->
            onError(
                "[kogot]: Error calling callable ${callable.arity()} args, fallback to null: $exception",
            )
            fillCallError(GDEXTENSION_CALL_ERROR_INVALID_METHOD)
            return
        }

        // TODO: properly convert return types to Kotlin types
        // (aka if the user request T, the argument must preserve the type T, not unbox or box unrequested)
        val resultVariant = onToVariant(result) ?: run {
            onError("[kogot]: Error converting callable result ($result) to Variant, fallback to null")
            fillCallError(GDEXTENSION_CALL_ERROR_INVALID_METHOD)
            return
        }

        fillCallError(GDEXTENSION_CALL_OK, 0, 0)
        try {
            VariantBinding.newCopyRaw(rReturn, resultVariant.rawPtr)
        } finally {
            // The engine got its own copy. The Variant built from the result is ours to free, unless the lambda
            // returned that Variant itself (#219: every call leaked one, the one of `Unit` included).
            if (resultVariant !== result) (resultVariant as? AutoCloseable)?.close()
        }
    }

    /**
     * Calls the low-level Godot function to create the custom callable into [callablePtr]. If that fails, the
     * [StableRef] of the userdata, which nothing would ever free otherwise (`free_func` only runs for a created
     * callable), is disposed before rethrowing (#219).
     */
    public fun createCustom2(callableCustomInfo2: CValue<GDExtensionCallableCustomInfo2>, callablePtr: COpaquePointer) {
        try {
            memScoped {
                CallableBinding.customCreate2Raw(
                    callablePtr,
                    callableCustomInfo2.ptr,
                )
            }
        } catch (t: Throwable) {
            callableCustomInfo2.useContents { callable_userdata }?.asStableRef<Any>()?.dispose()
            throw t
        }
    }

    /**
     * Creates a [GDExtensionCallableCustomInfo2] struct.
     * Delegates to [CallableTrampolines]
     * @param userdata Expects a [KotlinCallable] as [COpaquePointer]
     * @param call The custom function to call this callable
     * @param objectId 0 means _custom_, not bind to object
     */
    public fun createCustomInfo2(
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
