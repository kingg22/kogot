package io.github.kingg22.godot.internal.callback

import io.github.kingg22.godot.internal.binding.InternalBinding

@InternalBinding
@PublishedApi
internal value class Callable1(private val lambda: Function1<Any?, *>) : KotlinCallable {
    override fun call(vararg args: Any?): Any? = lambda(args[0])
    override fun arity(): Long = 1
    override fun toString(): String = "Callable1(lambda=$lambda)"
}
