package io.github.kingg22.godot.codegen.rules

/**
 * Base interface for API generation rules.
 *
 * A rule determines whether a specific API item should be included
 * in the generated output.
 */
sealed interface CodegenRule {
    /**
     * Checks if the given API item matches this rule.
     */
    fun matches(item: ApiItem): Boolean

    /**
     * Combines this rule with another rule using AND logic.
     */
    infix fun and(other: CodegenRule): CodegenRule = AndRule(this, other)

    /**
     * Combines this rule with another rule using OR logic.
     */
    infix fun or(other: CodegenRule): CodegenRule = OrRule(this, other)

    /**
     * Negates this rule.
     */
    fun not(): CodegenRule = NotRule(this)
}

/**
 * AND combination of two rules.
 */
private data class AndRule(private val left: CodegenRule, private val right: CodegenRule) : CodegenRule {
    override fun matches(item: ApiItem): Boolean = left.matches(item) && right.matches(item)
}

/**
 * OR combination of two rules.
 */
private data class OrRule(private val left: CodegenRule, private val right: CodegenRule) : CodegenRule {
    override fun matches(item: ApiItem): Boolean = left.matches(item) || right.matches(item)
}

/**
 * NOT of a rule.
 */
private data class NotRule(private val rule: CodegenRule) : CodegenRule {
    override fun matches(item: ApiItem): Boolean = !rule.matches(item)
}
