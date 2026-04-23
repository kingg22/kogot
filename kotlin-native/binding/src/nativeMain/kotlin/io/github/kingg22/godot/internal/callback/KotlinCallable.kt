package io.github.kingg22.godot.internal.callback

/**
 * Interface for Kotlin lambdas stored as callable userdata.
 *
 * This interface represents a Kotlin function that can be invoked by Godot's
 * custom callable system. Each implementation holds a lambda and provides
 * the necessary metadata (arity, hash, equality) for the callable infrastructure.
 */
public interface KotlinCallable {
    /**
     * Invokes this callable with the given arguments.
     *
     * @param args The arguments as Kotlin Any? array
     * @return The result of the invocation, or null if no result
     */
    public fun call(args: Array<Any?>): Any?

    /**
     * Returns the number of arguments this callable expects.
     */
    public fun arity(): Int

    /**
     * Returns a hash value for this callable.
     */
    public override fun hashCode(): Int

    /**
     * Checks equality with another KotlinCallable.
     */
    public override fun equals(other: Any?): Boolean

    /**
     * Returns a string representation for debugging.
     */
    public override fun toString(): String

    /**
     * Returns whether this callable is still valid.
     * Default implementation returns true.
     */
    public fun isValid(): Boolean = true

    /**
     * Comparison for less-than ordering (used for sorting/btrees).
     * Default implementation returns false.
     */
    public fun lessThan(other: KotlinCallable): Boolean = false
}
