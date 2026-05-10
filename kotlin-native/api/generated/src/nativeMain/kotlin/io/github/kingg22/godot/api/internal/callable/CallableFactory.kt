package io.github.kingg22.godot.api.internal.callable

import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.internal.anyToVariantOrNull
import io.github.kingg22.godot.api.builtin.internal.getValue
import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.pushError
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.ffi.GDExtensionConstVariantPtr

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
    init {
        CallableBinding.onError = GD::pushError
        CallableBinding.onVariantToKotlin = ::reinterpretedVariantToKotlin
        CallableBinding.onToVariant = ::anyToVariantOrNull
    }

    /** See [Callable] class and factory function */
    @UsedFromCodegenGeneratedCode
    public fun create(lambda: Function<*>, callable: Callable) {
        check(callable.isNull()) { "Callable must be null to create a custom callable" }
        val callableCustomInfo2 = CallableBinding.createCustomInfo2(lambda)
        CallableBinding.createCustom2(callableCustomInfo2, callable.rawPtr)
    }

    /** See [Callable] class and factory function. Create the empty Callable */
    public fun create(lambda: Function<*>): Callable = Callable(null).apply { create(lambda, this) }

    public fun reinterpretedVariantToKotlin(argument: GDExtensionConstVariantPtr?): Any? =
        if (argument == null) null else Variant(argument).getValue()
}
