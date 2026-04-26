package io.github.kingg22.godot.codegen.rules

import io.github.kingg22.godot.codegen.models.config.ApiFilters

/**
 * Engine for applying rules to filter API items.
 *
 * The engine combines:
 * 1. Kind-based filtering (include/exclude enums, builtin classes, etc.)
 * 2. Type-name filtering (exclude specific types)
 */
class RuleEngine(
    private val filters: ApiFilters = ApiFilters.ALL,
    private val additionalRules: List<CodegenRule> = emptyList(),
) {
    /**
     * Creates a composite rule from the configured filters.
     */
    private fun buildCompositeRule(): CodegenRule {
        var rule: CodegenRule = IncludeKindRule(
            includeEnums = filters.includeEnums,
            includeBuiltinClasses = filters.includeBuiltinClasses,
            includeEngineClasses = filters.includeEngineClasses,
            includeUtilityFunctions = filters.includeUtilityFunctions,
            includeNativeStructures = filters.includeNativeStructures,
        )

        if (filters.excludedTypes.isNotEmpty()) {
            rule = rule and ExcludeTypesRule(filters.excludedTypes)
        }

        return if (additionalRules.isEmpty()) {
            rule
        } else {
            rule and additionalRules.reduce { acc, r -> acc and r }
        }
    }

    /**
     * Checks if an API item should be included in generation.
     */
    fun shouldInclude(item: ApiItem): Boolean = buildCompositeRule().matches(item)

    /**
     * Filters a sequence of API items, keeping only those that match.
     */
    fun <T : ApiItem> filter(sequence: Sequence<T>): Sequence<T> = sequence.filter { shouldInclude(it) }

    /**
     * Filters a list of API items, keeping only those that match.
     */
    fun <T : ApiItem> filter(list: List<T>): List<T> = list.filter { shouldInclude(it) }

    companion object {
        /**
         * Creates a RuleEngine from ApiFilters configuration.
         */
        fun fromApiFilters(filters: ApiFilters): RuleEngine = RuleEngine(filters)
    }
}
