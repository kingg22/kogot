@file:OptIn(ExperimentalGodotKotlin::class)

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.MustBeVariant
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.builtin.getValue
import io.github.kingg22.godot.api.builtin.getValueOrNull
import io.github.kingg22.godot.api.builtin.toVariant
import io.github.kingg22.godot.api.builtin.toVariantOrNull

/**
 * Exercises every generated API with a `@MustBeVariant` contract (Callable factories live in CallableUsage.kt), so
 * the kogot compiler plugin validates each against the real generated declarations. All uses here are valid.
 */
// The two `as Any` samples go through the runtime slow path on purpose (`anyToVariant*`), so the open type is expected.
@Suppress("MUST_BE_VARIANT_UNKNOWN_TYPE")
internal fun mustBeVariantSamples(): List<Variant?> = listOf(
    // Sending: Kotlin value -> Variant.
    Variant.from(42L),
    Variant.from("hello kogot"),
    ((3.5) as Any).toVariant(), // force use anyToVariant
    (Vector2(1.0, 2.0) as Any).toVariantOrNull(), // force use anyToVariantOrNull
    // Most types have a typed `toVariant()` overload (`Int?`, `String?`, `Vector2?`...); Short only has the generic one.
    7.toShort().toVariant(),
    true.toVariantOrNull(),
)

/** Receiving: Variant -> Kotlin value, explicit, inferred, and through `by variant`. */
internal class VariantReader(variant: Variant) {
    val health: Int by variant
    val name: String = variant.getValue()
    val speed: Float? = variant.getValueOrNull()

    fun explicit(variant: Variant): Long = variant.getValue<Long>()

    fun local(variant: Variant): Double {
        val scale: Double by variant
        return scale
    }
}

/** Collections: every value entering the list is checked, and its elements can be sent back as they are. */
internal fun variantList(extra: Long): List<Variant> {
    val values: MutableList<@MustBeVariant Any?> = mutableListOf(1L, "two", Vector2(3.0, 4.0))
    values.add(extra)
    values += "five"
    return values.map { it.toVariant() }
}
