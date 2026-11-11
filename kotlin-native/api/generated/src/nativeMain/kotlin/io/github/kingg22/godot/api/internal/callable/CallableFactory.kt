package io.github.kingg22.godot.api.internal.callable

import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.toVariant
import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.pushError
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.ffi.GDExtensionConstVariantPtr

/**
 * Creates Godot custom Callables backed by Kotlin functions.
 *
 * Users call the typed `Callable { a: Int, b: String -> ... }` factories instead, which convert every argument
 * to the lambda's declared parameter type.
 */
@UsedFromCodegenGeneratedCode
@InternalBinding
public object CallableFactory {
    init {
        CallableBinding.onError = GD::pushError
        CallableBinding.onToVariant = Any?::toVariant
    }

    /** Creates a custom [Callable] that receives Godot's raw argument pointers. */
    @UsedFromCodegenGeneratedCode
    public fun create(arity: Int, invoker: (arguments: Array<GDExtensionConstVariantPtr?>) -> Any?): Callable =
        Callable(null).also { create(it, arity, invoker) }

    /** Initializes the empty [callable] (e.g. from a `Callable` constructor) as a custom callable. */
    public fun create(
        callable: Callable,
        arity: Int,
        invoker: (arguments: Array<GDExtensionConstVariantPtr?>) -> Any?,
    ) {
        check(callable.isNull()) { "Callable must be null to create a custom callable" }
        val callableCustomInfo2 = CallableBinding.createCustomInfo2(KotlinCallable(arity.toLong(), invoker))
        CallableBinding.createCustom2(callableCustomInfo2, callable.rawPtr)
    }
}
