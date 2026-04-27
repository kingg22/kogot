package io.github.kingg22.godot.codegen.rules

/**
 * Exclude rule - excludes items that match the predicate.
 */
data class ExcludeRule(private val predicate: (ApiItem) -> Boolean) : CodegenRule {
    constructor(vararg names: String) : this({ it.name in names.toSet() })
    constructor(names: Set<String>) : this({ it.name in names })

    override fun matches(item: ApiItem): Boolean = !predicate(item)
}
