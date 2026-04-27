package io.github.kingg22.godot.codegen.rules

/**
 * Include rule - only includes items that match the predicate.
 */
data class IncludeRule(private val predicate: (ApiItem) -> Boolean) : CodegenRule {
    constructor(vararg names: String) : this({ it.name in names.toSet() })
    constructor(names: Set<String>) : this({ it.name in names })

    override fun matches(item: ApiItem): Boolean = predicate(item)
}
