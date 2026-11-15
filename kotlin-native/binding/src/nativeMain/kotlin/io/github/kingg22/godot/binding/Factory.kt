package io.github.kingg22.godot.binding

import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.singleton.ClassDB
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.resolveBinding

/**
 * Instantiate a Godot object of type [T].
 *
 * ```kotlin
 * @Godot class MyGodotClass(rawPtr: ObjectPtr) : Node(rawPtr)
 *
 * val myObject: MyGodotClass = instantiate()
 * ```
 *
 * **For API classes**: use the secondary constructor, the godot name should not be the same as the Kotlin class name.
 * This method will fail due to missing the kotlin instance.
 *
 * **For custom classes** (`@Godot`): the returned value is the canonical instance, the same one the engine calls into.
 * A `RefCounted` one is owned by Kotlin like any other wrapper: it stays alive while Kotlin uses it (or the engine holds
 * a reference to it), and is freed once neither does (issue #114). A `Node` is not reference counted: free it yourself
 * (`queueFree()`) or hand it to the scene tree, exactly as with an engine node.
 *
 * **Safety**: The class must not be anonymous and requires already registered in the ClassDB with Godot.
 *
 * **Note**: The following code is **invalid**, DON'T do that:
 *
 * ```kotlin
 * val myObject: MyGodotClass = MyGodotClass(Node().rawPtr) // This is invalid
 * val myNode: Node = instantiate() // This will fail
 * ```
 */
public inline fun <reified T : GodotObject> instantiate(): T {
    val className = requireNotNull(T::class.simpleName) {
        "Cannot instantiate ${T::class.qualifiedName} without a valid class name"
    }
    return className.toStringName().use { str -> ClassDB.instance.instantiate(str) }.use { variant ->
        @OptIn(InternalBinding::class)
        val nativePtr = variant.toObjectRawPtr()
        // create_instance_func (registerClass) already built and cached the Kotlin instance for nativePtr: resolve
        // it instead of building a second one (issue #114). Resolved before the Variant closes: for a RefCounted
        // that close drops the creation reference and the instance's own reference becomes the only one, so the
        // instance must already be held here (issue #213).
        @OptIn(InternalBinding::class)
        requireNotNull(resolveBinding<T>(nativePtr)) {
            "No Kotlin instance bound for a freshly-instantiated '$className' ($nativePtr) — " +
                "was it registered via createInstanceFunc?"
        }
    }
}
