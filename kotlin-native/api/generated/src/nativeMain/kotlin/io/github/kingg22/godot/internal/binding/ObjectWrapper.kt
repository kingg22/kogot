package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.GodotObjectClassNode
import io.github.kingg22.godot.api.singleton.ClassDB
import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtr

/**
 * Finds the most derived generated wrapper class for an engine object, walking the generated `ClassNode`
 * tree from `GodotObject` (issue #206). The two engine questions it needs are asked only the first time a
 * class is seen, because the resolver caches its answer per class name.
 */
private val wrapperResolver = WrapperResolver(
    root = GodotObjectClassNode,
    classNameOf = { ptr -> GodotObject(ptr).getClassAsGdStr().use { it.toKString() } },
    parentOf = { className ->
        StringName(className).use { name -> ClassDB.instance.getParentClass(name).use { it.toString() } }
    },
)

/**
 * The one canonical Kotlin wrapper of the object behind [rawPtr], of its most derived known class.
 *
 * This is what every conversion from a raw pointer to an object must use (`Variant.toObject()`, and through it
 * `getValue<T>()`, typed `Callable` parameters and collection elements): a `Variant` holding a `Sprite2D`
 * gives a `Sprite2D`, so a later `is`/`as` to `Node2D` is plain Kotlin and no factory is ever passed.
 *
 * It goes through [materialize], so the wrapper is created at most once per object (a `@Godot` class is
 * already in the slot and is returned as it is, its constructor never runs again) and the pointer is
 * treated as borrowed: a `RefCounted` wrapper takes its own reference.
 */
@InternalBinding
public fun wrapperOf(rawPtr: GDExtensionObjectPtr): GodotObject = materialize(rawPtr) { wrapperResolver.create(it) }
