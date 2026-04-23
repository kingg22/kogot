package io.github.kingg22.godot.internal.callback

/**
 * Wrapper class for a callable with no arguments.
 */
@PublishedApi
internal value class Callable0<R>(private val lambda: () -> R) : KotlinCallable {
    override fun call(args: Array<Any?>): Any? = lambda()
    override fun arity(): Int = 0
    override fun toString(): String = "Callable0(lambda=$lambda)"
}
