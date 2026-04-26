package io.github.kingg22.godot.codegen.models.config

/**
 * Filters for controlling API generation output.
 *
 * Use [ApiFilters.ALL] to generate everything.
 * Use [ApiFilters.none] combined with specific includes to generate only specific types.
 * Use [ApiFilters.Builder] for more fine-grained control.
 */
data class ApiFilters(
    val includeEnums: Boolean = true,
    val includeBuiltinClasses: Boolean = true,
    val includeEngineClasses: Boolean = true,
    val includeUtilityFunctions: Boolean = true,
    val includeNativeStructures: Boolean = true,
    val excludedTypes: Set<String> = emptySet(),
) {
    /**
     * Generates everything (default behavior).
     */
    companion object {
        @JvmField
        val ALL = ApiFilters()
    }

    /**
     * Creates ApiFilters with only enums included.
     */
    fun onlyEnums() = copy(
        includeEnums = true,
        includeBuiltinClasses = false,
        includeEngineClasses = false,
        includeUtilityFunctions = false,
        includeNativeStructures = false,
    )

    /**
     * Creates ApiFilters with only builtin classes included.
     */
    fun onlyBuiltinClasses() = copy(
        includeEnums = false,
        includeBuiltinClasses = true,
        includeEngineClasses = false,
        includeUtilityFunctions = false,
        includeNativeStructures = false,
    )

    /**
     * Creates ApiFilters with only engine classes included.
     */
    fun onlyEngineClasses() = copy(
        includeEnums = false,
        includeBuiltinClasses = false,
        includeEngineClasses = true,
        includeUtilityFunctions = false,
        includeNativeStructures = false,
    )

    /**
     * Creates ApiFilters excluding specific types.
     */
    fun excludingTypes(types: Set<String>) = copy(excludedTypes = types)

    /**
     * Builder for creating custom ApiFilters.
     */
    class Builder {
        private var includeEnums: Boolean = true
        private var includeBuiltinClasses: Boolean = true
        private var includeEngineClasses: Boolean = true
        private var includeUtilityFunctions: Boolean = true
        private var includeNativeStructures: Boolean = true
        private var excludedTypes: Set<String> = emptySet()

        fun includeEnums(include: Boolean = true) = apply { this.includeEnums = include }
        fun includeBuiltinClasses(include: Boolean = true) = apply { this.includeBuiltinClasses = include }
        fun includeEngineClasses(include: Boolean = true) = apply { this.includeEngineClasses = include }
        fun includeUtilityFunctions(include: Boolean = true) = apply { this.includeUtilityFunctions = include }
        fun includeNativeStructures(include: Boolean = true) = apply { this.includeNativeStructures = include }
        fun excludeTypes(types: Set<String>) = apply { this.excludedTypes = types }

        fun build() = ApiFilters(
            includeEnums = includeEnums,
            includeBuiltinClasses = includeBuiltinClasses,
            includeEngineClasses = includeEngineClasses,
            includeUtilityFunctions = includeUtilityFunctions,
            includeNativeStructures = includeNativeStructures,
            excludedTypes = excludedTypes,
        )
    }
}
