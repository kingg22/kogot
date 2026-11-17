@file:OptIn(ExperimentalForeignApi::class)

package io.github.kingg22.godot.test.registrationorder

import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.singleton.ClassDB
import io.github.kingg22.godot.test.assertKnownBug
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertNull

/*
 * Registration order of @Godot hierarchies. The generated `GeneratedBindings` registers classes in the order KSP
 * returns them (file and declaration order), not parent-first, and Godot refuses a class whose parent is not
 * registered yet (#254). Both hierarchies below are declared in this one file on purpose:
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
class RegistrationOrderTest {
    @Test
    fun controlAChildDeclaredAfterItsParentIsRegistered() {
        assertRegistered("OrderParentFirst", "Node")
        assertRegistered("OrderChildLast", "OrderParentFirst")
    }

    @Test
    fun theParentOfAChildDeclaredFirstIsRegistered() {
        assertRegistered("OrderParentLast", "Node")
    }

    /** #254 is open: Godot refuses a child registered before its parent. Passes while it does; see [assertKnownBug]. */
    @Test
    fun aChildDeclaredBeforeItsParentIsNotRegistered_254() {
        assertKnownBug("#254") {
            assertRegistered("OrderChildFirst", "OrderParentLast")
        }
    }

    private fun assertRegistered(className: String, parent: String) {
        val problem = registrationProblem(className, parent)
        assertNull(problem, problem ?: "Unknown")
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
