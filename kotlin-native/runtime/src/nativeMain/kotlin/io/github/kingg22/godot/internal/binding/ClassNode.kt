package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtr

/**
 * One node of the engine class tree, generated once per Godot class (issue #206).
 *
 * The tree is *distributed*: every node only knows its own constructor and its **direct** children, so
 * there is no single map holding every class. [WrapperResolver] walks it from the root down to the most
 * derived class the binding knows.
 *
 * This is also the only place that is allowed to call the wrapper's `(COpaquePointer)` constructor, so
 * removing that constructor later only changes the generated nodes, not the casting machinery.
 */
@InternalBinding
public interface ClassNode<out W : Any> {
    /** The Godot name of the class this node builds, e.g. `Node2D`. */
    public val godotName: String

    /** Builds the Kotlin wrapper of this exact class around [ptr]. Never reuses an instance; identity is the caller's job. */
    public fun create(ptr: GDExtensionObjectPtr): W

    /** The direct child class named [godotName], or `null` when this binding has no such subclass. */
    public fun child(godotName: String): ClassNode<W>?
}
