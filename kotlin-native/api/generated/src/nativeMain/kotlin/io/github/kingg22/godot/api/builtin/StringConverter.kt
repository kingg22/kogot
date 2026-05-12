package io.github.kingg22.godot.api.builtin

/** Syntactic sugar for [GodotString] constructor */
public fun String.toGodotString(): GodotString = GodotString(this)

/** Syntactic sugar for [NodePath] constructor */
public fun String.toNodePath(): NodePath = NodePath(this)

/** Syntactic sugar for [StringName] constructor */
public fun String.toStringName(): StringName = StringName(this)

/** Converts [String] to [Variant] using [GodotString] as [Variant.Type.STRING] */
public fun String?.toVariant(): Variant = toVariantString()

/** Converts [String] to [Variant] using [GodotString] as [Variant.Type.STRING] */
public fun String?.toVariantString(): Variant = this?.toGodotString().use { it.toVariant() }

/** Converts [String] to [Variant] using [NodePath] as [Variant.Type.NODE_PATH] */
public fun String?.toVariantNodePath(): Variant = this?.toNodePath().use { it.toVariant() }

/** Converts [String] to [Variant] using [StringName] as [Variant.Type.STRING_NAME] */
public fun String?.toVariantStringName(): Variant = this?.toStringName().use { it.toVariant() }
