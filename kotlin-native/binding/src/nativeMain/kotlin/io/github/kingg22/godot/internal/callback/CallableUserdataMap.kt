package io.github.kingg22.godot.internal.callback

import io.github.kingg22.godot.internal.binding.InternalBinding
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

/**
 * Global map storing KotlinCallable instances with unique IDs for callable userdata.
 *
 * This map serves as the bridge between Godot's custom callable system (which uses
 * opaque userdata pointers) and Kotlin lambdas. Each lambda is assigned a unique
 * ID that is passed as the userdata to Godot, allowing the trampolines to
 * retrieve the original lambda.
 *
 * **deprecated** This is no longer needed, prefers store the [KotlinCallable] in userdata directly.
 */
@InternalBinding
@OptIn(ExperimentalAtomicApi::class)
public object CallableUserdataMap {
    private val map = mutableMapOf<Long, KotlinCallable>()
    private var nextId = AtomicLong(0)

    public fun add(callable: KotlinCallable): Long {
        val id = nextId.incrementAndFetch()
        map[id] = callable
        return id
    }

    public fun remove(id: Long) {
        map.remove(id)
    }

    public fun get(id: Long): KotlinCallable? = map[id]
}
