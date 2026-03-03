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
class Context(
    private val builtinTypes: Set<String>,
    private val singletons: Set<String>,
    private val classes: Set<String>,
    private val globalEnumsTypes: Set<String>,
    private val nestedEnumsTypes: Set<Pair<String, String>>,
    private val inheritanceTree: InheritanceTree,
    val godotVersion: GodotVersion,
    packageRegistry: PackageRegistry,
    val precision: String,
) : PackageRegistry by packageRegistry {
    init {
        println("INFO: Context created to generate for Godot: $godotVersion")
    }

    constructor(incompleteContext: IncompleteContext, packageRegistry: PackageRegistry, precision: String) : this(
        builtinTypes = incompleteContext.builtinTypes,
        singletons = incompleteContext.singletons,
        classes = incompleteContext.classesAndApiType.map { it.first }.toSet(),
        globalEnumsTypes = incompleteContext.globalEnumsTypes,
        nestedEnumsTypes = incompleteContext.nestedEnumsTypes,
        inheritanceTree = incompleteContext.inheritanceTree,
        godotVersion = incompleteContext.godotVersion,
        packageRegistry = packageRegistry,
        precision = precision,
    )

    // ── Type classification ───────────────────────────────────────────────────

    fun isBuiltin(godotName: String): Boolean = godotName in builtinTypes
    fun isSingleton(godotName: String): Boolean = godotName in singletons
    fun isSingleton(godotClass: GodotClass): Boolean = godotClass.name in singletons

    fun isGodotType(godotName: String): Boolean =
        isBuiltin(godotName) || isSingleton(godotName) || godotName in classes || godotName in globalEnumsTypes ||
            godotName in nestedEnumsTypes.map { it.first }

    fun isNestedEnum(godotName: String): Boolean = nestedEnumsTypes.any {
        "${it.first}${it.second}" == godotName || it.second == godotName
    }

    fun getNestedEnumName(godotName: String): String {
        check(isNestedEnum(godotName)) { "Not a nested enum: $godotName" }
        return nestedEnumsTypes.first { "${it.first}${it.second}" == godotName || it.second == godotName }.second
    }

    fun getNestedEnumParent(godotName: String): String {
        check(isNestedEnum(godotName)) { "Not a nested enum: $godotName" }
        return nestedEnumsTypes.first { "${it.first}${it.second}" == godotName || it.second == godotName }.first
    }

    fun getNestedEnumPair(godotName: String): Pair<String, String> {
        check(isNestedEnum(godotName)) { "Not a nested enum: $godotName" }
        return nestedEnumsTypes.first { "${it.first}${it.second}" == godotName || it.second == godotName }
    }

    /** @return `true` if [godotName] is a specialized class (e.g. `Vector2i` is specialized of `Vector2`) and the base exists */
    fun isSpecializedClass(godotName: String): Boolean = godotName.endsWith('i') && isGodotType(godotName.dropLast(1))

    val isDoublePrecision: Boolean get() = precision == "double"

    // ── Hierarchy ─────────────────────────────────────────────────────────────

    /** Direct parent class, or null if [godotName] is a root (Object). */
    fun directBase(godotName: String): String? = inheritanceTree.directBase(godotName)

    /**
     * All base classes from nearest to furthest (Object), **excluding** [godotName] itself.
     *
     * Example: Node3D → [Node, Object]
     */
    fun allBases(godotName: String): List<String> = inheritanceTree.collectAllBases(godotName)

    /**
     * Returns true if [godotName] IS [baseName] or transitively inherits from it.
     *
     * Reflexive: `inherits("Node", "Node") == true`
     */
    fun inherits(godotName: String, baseName: String): Boolean = inheritanceTree.inherits(godotName, baseName)

    class IncompleteContext(
        val builtinTypes: Set<String>,
        val nativeStructureTypes: Set<String>,
        val singletons: Set<String>,
        /** List of Class name and API type */
        val classesAndApiType: Set<Pair<String, String>>,
        val inheritanceTree: InheritanceTree,
        val godotVersion: GodotVersion,
        val globalEnumsTypes: Set<String>,
        /** List of Parent class and nested enum name */
        val nestedEnumsTypes: Set<Pair<String, String>>,
    )

    companion object {
        fun buildFromApi(
            api: ExtensionApi,
            rootPackage: String,
            packageRegistryFactory: PackageRegistryFactory,
        ): Context {
            val incompleteContext = buildFromApi(api)
            val packageRegistry = packageRegistryFactory(rootPackage, incompleteContext)
            return Context(incompleteContext, packageRegistry, api.header.precision)
        }

        private fun buildFromApi(api: ExtensionApi): IncompleteContext {
            // add Variant as builtin because is absent
            val builtinTypes = mutableSetOf("Variant")
            val nativeStructureTypes = mutableSetOf<String>()
            val singletons = mutableSetOf<String>()
            val globalEnumsTypes = mutableSetOf<String>()
            val nestedEnumsTypes = mutableSetOf<Pair<String, String>>()
            val godotClasses = mutableSetOf<Pair<String, String>>()
            val tree = InheritanceTree()

            api.singletons.forEach { singleton ->
                singletons += singleton.name
            }

            api.builtinClasses.forEach { builtin ->
                builtinTypes += builtin.name
            }

            api.nativeStructures.forEach { ns ->
                nativeStructureTypes += ns.name
            }

            api.globalEnums.forEach { enum ->
                globalEnumsTypes += enum.name
            }

            api.classes.forEach { cls ->
                godotClasses += cls.name to cls.apiType
                cls.enums.forEach { nestedEnum ->
                    nestedEnumsTypes += cls.name to nestedEnum.name
                }
                cls.inherits?.takeIf { it.isNotBlank() }?.let { base ->
                    tree.insert(derived = cls.name, base = base)
                }
            }

            check(builtinTypes.none { it in singletons }) {
                "Found a builtin type that is also a singleton: ${builtinTypes.intersect(singletons)}"
            }

            return IncompleteContext(
                builtinTypes = builtinTypes,
                nativeStructureTypes = nativeStructureTypes,
                singletons = singletons,
                classesAndApiType = godotClasses,
                inheritanceTree = tree,
                godotVersion = GodotVersion(api.header),
                globalEnumsTypes = globalEnumsTypes,
                nestedEnumsTypes = nestedEnumsTypes,
            )
        }
    }
}
