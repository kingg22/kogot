package io.github.kingg22.godot.codegen.models.extensionapi.domain

import io.github.kingg22.godot.codegen.models.config.BuildConfiguration

data class ResolvedApiModel(
    val buildConfiguration: BuildConfiguration,
    val builtins: List<ResolvedBuiltinClass> = emptyList(),
    val engineClasses: List<ResolvedEngineClass> = emptyList(),
    val globalEnums: List<ResolvedEnum> = emptyList(),
    val nativeStructures: List<ResolvedNativeStructure> = emptyList(),
) {
    val builtinTypes: Set<String> = buildSet {
        add("Variant")
        builtins.forEach { add(it.name) }
    }
    val singletonNames: Set<String> = engineClasses.filter { it.isSingleton }.mapTo(linkedSetOf()) { it.name }
    val classApiTypes: Map<String, String> = engineClasses.associate { it.name to it.apiType }
    val globalEnumTypes: Set<String> =
        globalEnums.filterTo(linkedSetOf()) { it.ownerName == null }.map { it.name }.toSet()
    val nestedEnumOwners: Set<Pair<String, String>> = buildSet {
        builtins.forEach { builtin -> builtin.enums.forEach { add(builtin.name to it.shortName) } }
        engineClasses.forEach { engineClass -> engineClass.enums.forEach { add(engineClass.name to it.shortName) } }
        globalEnums.filter { it.ownerName != null }.forEach { add(it.ownerName!! to it.shortName) }
    }

    val builtinsByName: Map<String, ResolvedBuiltinClass> = builtins.associateBy { it.name }
    val engineClassesByName: Map<String, ResolvedEngineClass> = engineClasses.associateBy { it.name }
    val globalEnumsByName: Map<String, ResolvedEnum> = globalEnums.associateBy { it.name }
    val nativeStructuresByName: Map<String, ResolvedNativeStructure> = nativeStructures.associateBy { it.name }
}
