package io.github.kingg22.godot.api.builtin

/**
 * How the elements of a [GodotArray] or [Dictionary] cross between a [Variant] and the Kotlin type [T].
 *
 * A codec is fixed when the collection is built and never changes, so every wrapper derived from it reads and writes
 * the same way. It reuses [getValue] and [toVariant], so it is as strict as they are: an element that is not a [T]
 * throws instead of being dropped, and `NIL` is only valid for a nullable [T].
 *
 * It is internal on purpose: a codec that does not match the element type would let a wrong element through, so the
 * collections are only built by the factory functions, which pick the codec from the reified type.
 */
@PublishedApi
internal class ElementCodec<@MustBeVariant T> @PublishedApi internal constructor(
    private val reader: (Variant) -> T,
    private val writer: (T) -> Variant,
) {
    /**
     * Reads [variant] as a [T], taking ownership of it: it is closed unless [T] is [Variant] itself, which returns it.
     *
     * @throws ClassCastException if the element is not a [T].
     */
    fun read(variant: Variant): T = reader(variant)

    /** Writes [value] as a new [Variant] that the caller owns and must close. */
    fun write(value: T): Variant = writer(value)

    @PublishedApi
    internal companion object {
        /** The codec of the untyped containers: elements stay [Variant]s. */
        val VARIANT: ElementCodec<Variant> = ElementCodec({ it }, { Variant(it.rawPtr) })

        /**
         * The codec for [T]. Collections nested in [T] are read as `GodotArray<Variant>` or
         * `Dictionary<Variant, Variant>`: the engine keeps no inner element type, so it is not checked here.
         */
        @PublishedApi
        @Suppress("UNCHECKED_CAST")
        internal inline fun <@MustBeVariant reified T> of(): ElementCodec<T> {
            if (T::class == Variant::class) return VARIANT as ElementCodec<T>

            return ElementCodec(
                reader = { variant -> variant.use { it.getValue<T>() } },
                writer = { it.toVariant() },
            )
        }
    }
}
