package io.github.kingg22.godot.codegen.rules

/**
 * Include rule for specific API item types.
 */
data class IncludeKindRule(
    val includeEnums: Boolean = true,
    val includeBuiltinClasses: Boolean = true,
    val includeEngineClasses: Boolean = true,
    val includeUtilityFunctions: Boolean = true,
    val includeNativeStructures: Boolean = true,
) : CodegenRule {
    override fun matches(item: ApiItem): Boolean = when (item) {
        is ApiItem.GlobalEnum, is ApiItem.NestedEnum -> includeEnums
        is ApiItem.BuiltinClass -> includeBuiltinClasses
        is ApiItem.EngineClass -> includeEngineClasses
        is ApiItem.UtilityFunction -> includeUtilityFunctions
        is ApiItem.NativeStructure -> includeNativeStructures
    }
}
