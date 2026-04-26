package io.github.kingg22.godot.codegen.rules

/**
 * Represents an item in the Godot API that can be filtered.
 */
sealed class ApiItem {
    abstract val name: String

    /** A global enum (not owned by any class) */
    data class GlobalEnum(override val name: String, val values: List<String>) : ApiItem()

    /** An enum nested inside a class (e.g., Node.PlayMode) */
    data class NestedEnum(override val name: String, val ownerName: String, val values: List<String>) : ApiItem()

    /** A builtin class like String, Array, Vector2 */
    data class BuiltinClass(override val name: String) : ApiItem()

    /** An engine class like Node, Resource, Sprite2D */
    data class EngineClass(override val name: String) : ApiItem()

    /** A utility function (global function) */
    data class UtilityFunction(override val name: String) : ApiItem()

    /** A native structure */
    data class NativeStructure(override val name: String) : ApiItem()
}
