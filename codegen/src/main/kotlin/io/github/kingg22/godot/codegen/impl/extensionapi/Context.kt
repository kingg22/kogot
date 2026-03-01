package io.github.kingg22.godot.codegen.impl.extensionapi

import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensionapi.GodotClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.GodotVersion

/**
 * Inmutable, built once from [ExtensionApi].
 *
 * Provides fast lookups about the Godot type system:
 * class hierarchy, singletons, builtins, native structures.
 *
 * No generation state lives here — this is pure query over the parsed API.
 */
class Context private constructor(
    private val builtinTypes: Set<String>,
    private val nativeStructureTypes: Set<String>,
    private val singletons: Set<String>,

    /**
     * Classes that are "final" in Godot: singletons and abstract engine classes with no virtual interface.
     *
     * TODO investigate if this is still true in 4.0
     */
    private val finalClasses: Set<String>,
    private val inheritanceTree: InheritanceTree,
    val godotVersion: GodotVersion,
) {
    init {
        println("INFO: Context created to generate for Godot: $godotVersion")
    }

    // ── Type classification ───────────────────────────────────────────────────

    fun isBuiltin(godotName: String): Boolean = godotName in builtinTypes
    fun isNativeStructure(godotName: String): Boolean = godotName in nativeStructureTypes
    fun isSingleton(godotName: String): Boolean = godotName in singletons
    fun isSingleton(godotClass: GodotClass): Boolean = godotClass.name in singletons
    fun isFinal(godotName: String): Boolean = godotName in finalClasses

    // ── Hierarchy ─────────────────────────────────────────────────────────────

    fun directBase(godotName: String): String? = inheritanceTree.directBase(godotName)
    fun allBases(godotName: String): List<String> = inheritanceTree.collectAllBases(godotName)
    fun inherits(godotName: String, baseName: String): Boolean = inheritanceTree.inherits(godotName, baseName)

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun buildFromApi(api: ExtensionApi): Context {
            // add Variant as builtin because is absent
            val builtinTypes = mutableSetOf("Variant")
            val nativeStructureTypes = mutableSetOf<String>()
            val singletons = mutableSetOf<String>()
            val finalClasses = mutableSetOf<String>()
            val tree = InheritanceTree()

            api.singletons.forEach { singleton ->
                singletons += singleton.name
                finalClasses += singleton.name
            }

            api.builtinClasses.forEach { builtin ->
                builtinTypes += builtin.name
            }

            api.nativeStructures.forEach { ns ->
                nativeStructureTypes += ns.name
            }

            api.classes.forEach { cls ->
                cls.inherits?.takeIf { it.isNotBlank() }?.let { base ->
                    tree.insert(derived = cls.name, base = base)
                }
            }

            check(builtinTypes.none { it in singletons }) {
                "Found a builtin type that is also a singleton: ${builtinTypes.intersect(singletons)}"
            }

            return Context(
                builtinTypes = builtinTypes,
                nativeStructureTypes = nativeStructureTypes,
                singletons = singletons,
                finalClasses = finalClasses,
                inheritanceTree = tree,
                godotVersion = GodotVersion(api.header),
            )
        }
    }
}
