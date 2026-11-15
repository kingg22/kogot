@file:OptIn(ExperimentalForeignApi::class)

import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.singleton.ClassDB
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

/*
 * Registration order of @Godot hierarchies (`registration_order_check.tscn`). The generated `GeneratedBindings`
 * registers classes in the order KSP returns them (file and declaration order), not parent-first, and Godot refuses a
 * class whose parent is not registered yet (#254). Both hierarchies below are declared in this one file on purpose:
 *
 * - control: the parent comes first, so source order happens to be parent-first;
 * - the child comes first: Godot reports "Attempt to register an extension class 'OrderChildFirst' using non-existing
 *   parent class 'OrderParentLast'" at startup, and "Attempt to unregister unexisting extension class" at exit.
 *
 * Keep the declaration order of this file as it is: it is the reproduction.
 */

@Godot
open class OrderParentFirst(nativePtr: COpaquePointer) : Node(nativePtr)

@Godot
class OrderChildLast(nativePtr: COpaquePointer) : OrderParentFirst(nativePtr)

@Godot
class OrderChildFirst(nativePtr: COpaquePointer) : OrderParentLast(nativePtr)

@Godot
open class OrderParentLast(nativePtr: COpaquePointer) : Node(nativePtr)

/** Checks that both hierarchies are registered with the right parent; the child-first one is red until #254. */
@Godot
class RegistrationOrderCheck(nativePtr: COpaquePointer) : Node(nativePtr) {
    private val checks = Checks("RegistrationOrderCheck")

    override fun _ready() {
        with(checks) {
            check("control: a child declared after its parent is registered") {
                registrationProblem("OrderParentFirst", "Node")?.let { throw AssertionError(it) }
                registrationProblem("OrderChildLast", "OrderParentFirst")?.let { throw AssertionError(it) }
            }
            check("the parent of a child declared first is registered") {
                registrationProblem("OrderParentLast", "Node")?.let { throw AssertionError(it) }
            }
            check("a child declared before its parent is registered (#254)") {
                registrationProblem("OrderChildFirst", "OrderParentLast")?.let { throw AssertionError(it) }
            }
        }
        getTree().quit(checks.finish())
    }

    private fun registrationProblem(className: String, parent: String): String? {
        val exists = className.toStringName().use { ClassDB.instance.classExists(it) }
        if (!exists) return "$className not registered"
        val actual = className.toStringName().use { name ->
            ClassDB.instance.getParentClass(name).use { it.toString() }
        }
        return if (actual == parent) null else "$className has parent $actual, expected $parent"
    }
}
