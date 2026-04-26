package io.github.kingg22.godot.codegen.rules

/**
 * Exclude rule for specific type names.
 */
data class ExcludeTypesRule(private val excludedTypes: Set<String>) : CodegenRule {
    constructor(vararg names: String) : this(names.toSet())

    override fun matches(item: ApiItem): Boolean = item.name !in excludedTypes
}
