@file:OptIn(ExperimentalGodotKotlin::class) // safety, or throws an acceptable exception

package io.github.kingg22.godot.internal.callback

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.MustBeVariant
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.asVariant
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.CallableBinding
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.VariantBinding
import io.github.kingg22.godot.internal.binding.getInstance
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomCall
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomInfo2
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
     * Creates a Godot Callable from a Kotlin lambda with 0 arguments.
     *
     * @param T The return type of the lambda must be a [Variant type compatible][MustBeVariant].
     * Allows [Unit] as return type, is converted to [Variant.Type.NIL] internally.
     * @param lambda The Kotlin lambda to wrap
     * @return A Callable that wraps the Kotlin lambda
     */
    public inline fun <@MustBeVariant reified T> create(noinline lambda: () -> T): Callable {
        // Create a KotlinCallable wrapper
        val kotlinCallable = Callable0(lambda)

        // Register it in the map and get a unique ID
        val id = CallableUserdataMap.add(kotlinCallable)

        // Create a StableRef to hold the ID as userdata
        val userdata = StableRef.create(id).asCPointer()

        // Create the GDExtensionCallableCustomInfo2 struct using cValue
        val callableCustomInfo2 = createCallableCustomInfo2(
            userdata,
            staticCFunction { userdata, _, argumentCount, rReturn, rError ->
                val callable = CallableUserdataMap.get(userdata.getInstance()) ?: return@staticCFunction

                if (rReturn == null || rError == null) return@staticCFunction

                if (argumentCount != 0L) {
                    println("[kogot]: Error: callable called with $argumentCount arguments, expected 0")
                    val callError = rError[0]
                    callError.error = GDEXTENSION_CALL_ERROR_TOO_MANY_ARGUMENTS
                    callError.expected = 0
                    callError.argument = argumentCount.toInt()
                    return@staticCFunction
                }

                // TODO: properly convert arguments to Kotlin types using Variant
                val result = runCatching { callable.call(emptyArray()) }.getOrElse {
                    println("[kogot]: Error calling callable 0 args, fallback to null: $it")
                    null
                }

                val resultVariant = when (result) {
                    null, is Unit -> Variant()

                    is Variant -> result

                    else -> runCatching { result.asVariant() }.getOrElse {
                        println("[kogot]: Error converting callable result ($result) to Variant, fallback to null: $it")
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

    @PublishedApi
    internal fun createCallableCustomInfo2(
        userdata: COpaquePointer,
        call: GDExtensionCallableCustomCall,
    ): CValue<GDExtensionCallableCustomInfo2> = cValue {
        callable_userdata = userdata
        token = BindingProcAddressHolder.library
        object_id = 0uL // custom, not bind to object
        call_func = call
        is_valid_func = CallableTrampolines.isValidFunc
        free_func = CallableTrampolines.freeFunc
        hash_func = CallableTrampolines.hashFunc
        equal_func = CallableTrampolines.equalFunc
        less_than_func = CallableTrampolines.lessThanFunc
        to_string_func = CallableTrampolines.toStringFunc
        get_argument_count_func = CallableTrampolines.getArgumentCountFunc
    }
}
