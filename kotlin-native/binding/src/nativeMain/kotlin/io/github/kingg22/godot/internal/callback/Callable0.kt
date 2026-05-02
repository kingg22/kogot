package io.github.kingg22.godot.internal.callback

import io.github.kingg22.godot.internal.binding.InternalBinding

@InternalBinding
@PublishedApi
internal value class Callable0(private val lambda: Function0<*>) : KotlinCallable {
    override fun call(vararg args: Any?): Any? = lambda()
    override fun arity(): Long = 0
    override fun toString(): String = "Callable0(lambda=$lambda)"
}
