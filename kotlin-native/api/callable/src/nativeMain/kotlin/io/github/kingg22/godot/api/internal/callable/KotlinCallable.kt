package io.github.kingg22.godot.api.internal.callable

import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.ffi.GDExtensionConstVariantPtr

/**
 * A Kotlin function stored as the userdata of a Godot custom callable.
 *
 * [invoker] receives the raw argument pointers exactly as Godot passed them; converting them to Kotlin
 * types is the job of the typed `Callable { ... }` factories, which know the lambda's parameter types.
 *
 * @param arity number of arguments Godot must pass, checked before every call.
 * @see CallableTrampolines
 */
@UsedFromCodegenGeneratedCode
@InternalBinding
public class KotlinCallable(
    private val arity: Long,
    private val invoker: (arguments: Array<GDExtensionConstVariantPtr?>) -> Any?,
) {
    /** Returns the number of arguments this callable expects. */
    public fun arity(): Long = arity

    /** Invokes the function. **Safety:** `arguments.size` must equal [arity]. */
    public fun call(arguments: Array<GDExtensionConstVariantPtr?>): Any? {
        check(arguments.size.toLong() == arity) { "Expected $arity arguments, got ${arguments.size}" }
        return invoker(arguments)
    }

    /** Returns whether this callable is still valid. */
    public fun isValid(): Boolean = true

    /** Comparison for less-than ordering (used for sorting/btrees). */
    public fun lessThan(other: KotlinCallable): Boolean = false

    override fun equals(other: Any?): Boolean = other is KotlinCallable && invoker == other.invoker

    override fun hashCode(): Int = invoker.hashCode()

    override fun toString(): String = "KotlinCallable(arity=$arity)"
}
