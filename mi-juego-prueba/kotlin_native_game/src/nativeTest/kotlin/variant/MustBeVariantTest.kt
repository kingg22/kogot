@file:OptIn(ExperimentalGodotKotlin::class)

package io.github.kingg22.godot.test.variant

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.MustBeVariant
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.builtin.getValue
import io.github.kingg22.godot.api.builtin.getValueOrNull
import io.github.kingg22.godot.api.builtin.toVariant
import io.github.kingg22.godot.api.builtin.toVariantOrNull
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every generated API with a `@MustBeVariant` contract, used the ways a game uses it: the kogot compiler plugin
 * validates each use against the real generated declarations when this file is compiled, and the tests check what
 * crosses into and out of the `Variant`. Ported from `MustBeVariantUsage`. All the uses here are valid.
 */
class MustBeVariantTest {
    // The two `as Any` samples go through the runtime slow path on purpose (`anyToVariant*`): the open type is expected.
    @Suppress("MUST_BE_VARIANT_UNKNOWN_TYPE")
    private fun sendingSamples(): List<Variant?> = listOf(
        // Sending: Kotlin value -> Variant.
        Variant.from(42L),
        Variant.from("hello kogot"),
        ((3.5) as Any).toVariant(), // force use anyToVariant
        (Vector2(1.0, 2.0) as Any).toVariantOrNull(), // force use anyToVariantOrNull
        // Most types have a typed `toVariant()` overload (`Int?`, `String?`...); Short only has the generic one.
        7.toShort().toVariant(),
        true.toVariantOrNull(),
    )

    /** Collections: every value entering the list is checked, and its elements can be sent back as they are. */
    private fun variantList(extra: Long): List<Variant> {
        val values: MutableList<@MustBeVariant Any?> = mutableListOf(1L, "two", Vector2(3.0, 4.0))
        values.add(extra)
        values += "five"
        return values.map { it.toVariant() }
    }

    @Test
    fun sendingAKotlinValueGivesTheVariantOfItsType() {
        val samples = sendingSamples()
        try {
            val types = listOf(
                Variant.Type.INT,
                Variant.Type.STRING,
                Variant.Type.FLOAT,
                Variant.Type.VECTOR2,
                Variant.Type.INT,
                Variant.Type.BOOL,
            )
            assertEquals(types, samples.map { it?.getType() }, "types of the samples")
            assertEquals("42", samples[0]?.toString())
            assertEquals("hello kogot", samples[1]?.toString())
        } finally {
            samples.forEach { it?.close() }
        }
    }

    @Test
    fun receivingReadsTheVariantExplicitlyAndInferred() {
        Variant.from(7L).use { assertEquals(7L, it.getValue<Long>()) }
        Variant.from("kogot").use {
            val name: String = it.getValue()
            assertEquals("kogot", name)
        }
        Variant.from(1.5f).use {
            val speed: Float? = it.getValueOrNull()
            assertEquals(1.5f, speed)
        }
    }

    @Test
    fun receivingThroughByVariantDelegates() {
        Variant.from(42L).use { variant ->
            val health: Int by variant
            assertEquals(42, health)
        }
        Variant.from(2.5).use { variant ->
            val scale: Double by variant
            assertEquals(2.5, scale)
        }
    }

    @Test
    fun valuesEnteringAMustBeVariantListCanBeSentBackAsTheyAre() {
        val variants = variantList(extra = 4L)
        try {
            val types = listOf(
                Variant.Type.INT,
                Variant.Type.STRING,
                Variant.Type.VECTOR2,
                Variant.Type.INT,
                Variant.Type.STRING,
            )
            assertEquals(types, variants.map { it.getType() }, "types of the list")
            assertEquals(4L, variants[3].getValue<Long>())
            assertEquals("five", variants[4].toString())
        } finally {
            variants.forEach(Variant::close)
        }
    }
}
