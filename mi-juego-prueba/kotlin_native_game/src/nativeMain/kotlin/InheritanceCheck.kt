@file:OptIn(ExperimentalForeignApi::class)

import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.RefCounted
import io.github.kingg22.godot.api.singleton.ClassDB
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.ObjectBinding
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Live inheritance matrix (`inheritance_check.tscn`): every engine class the generated API lets a user extend has a
 * `@Godot` subclass `KK_<Parent>` in `InheritanceMatrix.kt`, registered by the KSP processor like any user class. For
 * each one this checks that Godot registered it with the right parent, and that `ClassDB.instantiate` builds it
 * through the KSP factory: the object is the Kotlin `KK_<Parent>` instance and is still a `<Parent>` for the engine.
 *
 * The known failures are separate checks, red on purpose until fixed:
 * - the classes the generator renames in Kotlin (`Object` → `GodotObject`, `Range` → `GodotRange`, ...) are registered
 *   with the Kotlin name as parent, which Godot does not know (#239);
 * - editor classes do not exist yet when kogot registers its classes (#241).
 *
 * What Kotlin refuses at compile time (abstract, final and singleton parents) is not part of the matrix; see
 * `docs/inheritance-matrix.md`.
 */
@Godot
class InheritanceCheck(nativePtr: COpaquePointer) : Node(nativePtr) {
    private val checks = Checks("InheritanceCheck")

    override fun _ready() {
        val knownBroken = (INHERITANCE_MATRIX_RENAMED + INHERITANCE_MATRIX_EDITOR).toSet()
        val supported = INHERITANCE_MATRIX_PARENTS.filterNot { it in knownBroken }

        with(checks) {
            check("${supported.size} @Godot subclasses are registered with their engine parent") {
                val wrong = supported.mapNotNull { parent -> registrationProblem(parent) }
                expect(wrong.isEmpty()) { "${wrong.size} wrong: ${wrong.take(MAX_LISTED)}" }
            }
            val instantiated = supported.filterNot { it in NOT_INSTANTIATED }
            check("${instantiated.size} @Godot subclasses are instantiated through the KSP factory") {
                val wrong = instantiated.mapNotNull { parent -> instantiationProblem(parent) }
                expect(wrong.isEmpty()) { "${wrong.size} wrong: ${wrong.take(MAX_LISTED)}" }
            }
            for (parent in INHERITANCE_MATRIX_RENAMED) {
                check("a @Godot subclass of $parent (renamed in Kotlin) is registered (#239)") {
                    registrationProblem(parent)?.let { throw AssertionError(it) }
                }
            }
            for (parent in INHERITANCE_MATRIX_EDITOR) {
                check("a @Godot subclass of the editor class $parent is registered (#241)") {
                    registrationProblem(parent)?.let { throw AssertionError(it) }
                }
            }
        }
        getTree().quit(checks.finish())
    }

    private fun registrationProblem(parent: String): String? {
        val className = "KK_$parent"
        val exists = className.toStringName().use { ClassDB.instance.classExists(it) }
        if (!exists) return "$className not registered"
        val actual = className.toStringName().use { name ->
            ClassDB.instance.getParentClass(name).use { it.toString() }
        }
        return if (actual == parent) null else "$className has parent $actual"
    }

    private fun instantiationProblem(parent: String): String? {
        val className = "KK_$parent"
        val instance = className.toStringName().use { name ->
            ClassDB.instance.instantiate(name).use { it.toObjectOrNull() }
        } ?: return "$className: instantiate gave null"
        try {
            val kotlinClass = instance::class.simpleName
            if (kotlinClass != className) return "$className: the object is a $kotlinClass, not the KSP instance"
            val isParent = parent.toStringName().use { instance.isClass(it) }
            if (!isParent) return "$className: the engine does not see it as a $parent"
            return null
        } finally {
            // A RefCounted goes away with its last reference; any other object must be freed by its owner, us.
            @OptIn(InternalBinding::class)
            if (instance !is RefCounted) ObjectBinding.destroyRaw(instance.rawPtr)
        }
    }

    private companion object {
        const val MAX_LISTED = 20

        /**
         * Registered but not instantiated: constructing one of these installs it as the engine's physics or text
         * server singleton, and freeing it leaves that singleton dangling. The next physics node then crashes the
         * engine (seen with `PinJoint2D` after `PhysicsServer2DExtension`, with or without kogot).
         */
        val NOT_INSTANTIATED = setOf("PhysicsServer2DExtension", "PhysicsServer3DExtension", "TextServerExtension")
    }
}
