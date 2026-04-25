package io.github.kingg22.godot.codegen.extensionapi

/**
 * Maintains the Godot class hierarchy using Godot names (as they appear in extension_api.json).
 *
 * Built once during [Context] construction, never mutated after that.
 */
class InheritanceTree {

    private val derivedToBase = mutableMapOf<String, String>()

    fun insert(derived: String, base: String) {
        check(derivedToBase.put(derived, base) == null) {
            "Duplicate inheritance insert for '$derived'"
        }
    }

    /** Direct parent class, or null if [derived] is a root (Object). */
    fun directBase(derived: String): String? = derivedToBase[derived]

    /**
     * All base classes from nearest to furthest (Object), **excluding** [derived] itself.
     *
     * Example: Node3D → [Node, Object]
     */
    fun collectAllBases(derived: String): List<String> {
        val result = mutableListOf<String>()
        var current = derived
        while (true) {
            val base = derivedToBase[current] ?: break
            result += base
            current = base
        }
        return result
    }

    /**
     * Returns true if [derived] IS [baseName] or transitively inherits from it.
     *
     * Reflexive: `inherits("Node", "Node") == true`
     */
    fun inherits(derived: String, baseName: String): Boolean {
        if (derived == baseName) return true
        var current = derived
        while (true) {
            val base = derivedToBase[current] ?: return false
            if (base == baseName) return true
            current = base
        }
    }
}
