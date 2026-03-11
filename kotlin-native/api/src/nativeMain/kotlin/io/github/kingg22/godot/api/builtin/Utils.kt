package io.github.kingg22.godot.api.builtin

public fun String.asGodotString(): GodotString = GodotString(this)

public fun String.asNodePath(): NodePath = NodePath(this)

public fun String.asStringName(): StringName = StringName(this)

public fun String?.asVariantString(): Variant = this?.asGodotString().use { it.asVariant() }

public fun String?.asVariantStringName(): Variant = this?.asStringName().use { it.asVariant() }

public fun String?.asVariantNodePath(): Variant = this?.asNodePath().use { it.asVariant() }
