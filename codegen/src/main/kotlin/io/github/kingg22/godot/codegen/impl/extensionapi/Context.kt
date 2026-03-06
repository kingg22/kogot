package io.github.kingg22.godot.codegen.impl.extensionapi

import io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver.EnumConstantResolver
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.EngineClass
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
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
    val extensionApi: ExtensionApi,
    private val builtinTypes: Set<String>,
    private val singletons: Set<String>,
    private val classes: Set<String>,
    private val globalEnumsTypes: Set<String>,
    val nativeStructureTypes: Set<String>,
    private val inheritanceTree: InheritanceTree,
    private val enumConstantResolver: EnumConstantResolver,
    private val experimentalTypesRegistry: ExperimentalTypesRegistry,
    val godotVersion: GodotVersion,
    packageRegistry: PackageRegistry,
    val precision: String,
) : PackageRegistry by packageRegistry {
    init {
        println("INFO: Context created to generate for Godot: $godotVersion")
    }

    constructor(
        extensionApi: ExtensionApi,
        incompleteContext: IncompleteContext,
        packageRegistry: PackageRegistry,
        precision: String,
        experimentalTypesRegistry: ExperimentalTypesRegistry,
    ) : this(
        extensionApi = extensionApi,
        builtinTypes = incompleteContext.builtinTypes,
        singletons = incompleteContext.singletons,
        classes = incompleteContext.classesAndApiType.map { it.first }.toSet(),
        globalEnumsTypes = incompleteContext.globalEnumsTypes,
        nativeStructureTypes = incompleteContext.nativeStructureTypes,
        inheritanceTree = incompleteContext.inheritanceTree,
        enumConstantResolver = incompleteContext.enumConstantResolver,
        experimentalTypesRegistry = experimentalTypesRegistry,
        godotVersion = incompleteContext.godotVersion,
        packageRegistry = packageRegistry,
        precision = precision,
    )

    // ── Type classification ───────────────────────────────────────────────────

    fun isBuiltin(godotName: String): Boolean = godotName in builtinTypes
    fun isSingleton(godotName: String): Boolean = godotName in singletons
    fun isSingleton(engineClass: EngineClass): Boolean = engineClass.name in singletons

    fun isGodotType(godotName: String) = isBuiltin(godotName) || isSingleton(godotName) ||
        godotName in classes || godotName in globalEnumsTypes

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

    // Enums Constants
    fun resolveEnumConstant(parentClass: String?, enumName: String, value: Long): String? =
        enumConstantResolver.resolveConstant(parentClass, enumName, value)

    fun getConstantEnumsWithValueFor(parentClass: String?, enumName: String) =
        enumConstantResolver.getAllConstantsWithValue(parentClass, enumName)

    fun getConstantEnumNamesFor(parentClass: String?, enumName: String) =
        enumConstantResolver.getAllConstantsNames(parentClass, enumName)

    // experimental types
    fun isExperimentalType(className: String, memberName: String? = null): Boolean =
        experimentalTypesRegistry.isExperimental(className, memberName)

    fun getReasonOfExperimental(className: String, memberName: String? = null): String? =
        experimentalTypesRegistry.getReason(className, memberName)

    // ── API Lookups ───────────────────────────────────────────────────────

    fun findBuiltinClass(name: String): BuiltinClass? = extensionApi.builtinClasses.find { it.name == name }

    fun findEngineClass(name: String): EngineClass? = extensionApi.classes.find { it.name == name }

    fun findConstructor(className: String, argCount: Int): BuiltinClass.Constructor? = findBuiltinClass(className)
        ?.constructors
        ?.find { it.arguments.size == argCount }

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
        val enumConstantResolver: EnumConstantResolver,
    )

    companion object {
        fun buildFromApi(
            api: ExtensionApi,
            rootPackage: String,
            packageRegistryFactory: PackageRegistryFactory,
        ): Context {
            val incompleteContext = buildFromApi(api)
            val packageRegistry = packageRegistryFactory(rootPackage, incompleteContext)
            val experimentalTypesRegistry = if (incompleteContext.godotVersion.compareTo(4, 6, 1) == 0) {
                ExperimentalTypesRegistry.v4_6_1
            } else {
                error("Missing experimental types registry for Godot version ${incompleteContext.godotVersion}")
            }
            return Context(api, incompleteContext, packageRegistry, api.header.precision, experimentalTypesRegistry)
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
                builtin.enums.forEach { nestedEnum ->
                    nestedEnumsTypes += builtin.name to nestedEnum.name
                }
            }

            api.nativeStructures.forEach { ns ->
                nativeStructureTypes += ns.name
            }

            api.globalEnums.forEach { enum ->
                val enumName = enum.name
                if (enumName.contains(".")) {
                    nestedEnumsTypes += enumName.substringBefore('.') to enumName.substringAfter('.')
                } else {
                    globalEnumsTypes += enumName
                }
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

            val enumResolver = EnumConstantResolver.build(api)

            return IncompleteContext(
                builtinTypes = builtinTypes,
                nativeStructureTypes = nativeStructureTypes,
                singletons = singletons,
                classesAndApiType = godotClasses,
                inheritanceTree = tree,
                godotVersion = GodotVersion(api.header),
                globalEnumsTypes = globalEnumsTypes,
                nestedEnumsTypes = nestedEnumsTypes,
                enumConstantResolver = enumResolver,
            )
        }
    }
}
