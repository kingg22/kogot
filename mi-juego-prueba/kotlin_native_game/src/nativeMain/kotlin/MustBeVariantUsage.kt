@file:OptIn(ExperimentalGodotKotlin::class)

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.from

/** Exercises the kogot compiler plugin's `@MustBeVariant` call-site validation. */
internal fun mustBeVariantSamples(): List<Variant> = listOf(
    Variant.from(42L),
    Variant.from("hello kogot"),
)
