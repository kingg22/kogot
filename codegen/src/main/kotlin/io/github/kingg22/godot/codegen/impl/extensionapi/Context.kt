package io.github.kingg22.godot.codegen.impl.extensionapi

import io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver.EnumConstantResolver
import io.github.kingg22.godot.codegen.impl.runtime.prefixOf
import io.github.kingg22.godot.codegen.models.extensionapi.*
import io.github.kingg22.godot.codegen.models.extensionapi.domain.GodotVersion
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedApiModel
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinConstructor
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinLayout
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedEngineClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedEnum
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedNativeStructure
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedRuntimeInterface
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import io.github.kingg22.godot.codegen.models.internal.BuildConfiguration
import io.github.kingg22.godot.codegen.models.internal.CodegenOptions

/**
 * Inmutable, built once from [ExtensionApi] and optionally [GDExtensionInterface].
 *
 * Exposes:
 * - global indexes and lookups
 * - the resolved API model used by generators
 * - package registry delegated centrally through [PackageRegistry]
 */
class Context(
    val extensionApi: ExtensionApi,
    val extensionInterface: GDExtensionInterface?,
    val model: ResolvedApiModel,
    private val inheritanceTree: InheritanceTree,
    private val enumConstantResolver: EnumConstantResolver,
    private val experimentalTypesRegistry: ExperimentalTypesRegistry,
    val godotVersion: GodotVersion,
    packageRegistry: PackageRegistry,
    val options: CodegenOptions,
) : PackageRegistry by packageRegistry {
    init {
        println("INFO: Context created to generate for Godot: $godotVersion (${model.buildConfiguration.jsonName})")
    }

    val precision: String get() = extensionApi.header.precision
    val isDoublePrecision: Boolean get() = precision == "double"

    val builtinTypes: Set<String> get() = model.builtinTypes
    val singletons: Set<String> get() = model.singletonNames
    val globalEnumsTypes: Set<String> get() = model.globalEnumTypes
    val nestedEnumsTypes: Set<Pair<String, String>> get() = model.nestedEnumOwners
    val nativeStructureTypes: Set<String> get() = model.nativeStructuresByName.keys
    val classesAndApiType: Set<Pair<String, String>>
        get() = model.classApiTypes.entries.mapTo(linkedSetOf()) { it.toPair() }

    fun isBuiltin(godotName: String): Boolean = godotName in builtinTypes
    fun isSingleton(godotName: String): Boolean = godotName in singletons
    fun isSingleton(engineClass: EngineClass): Boolean =
        model.engineClassesByName[engineClass.name]?.isSingleton == true

    fun isGodotType(godotName: String) = isBuiltin(godotName) || isSingleton(godotName) ||
        godotName in model.engineClassesByName || godotName in globalEnumsTypes

    fun isSpecializedClass(godotName: String): Boolean = godotName.endsWith('i') && isGodotType(godotName.dropLast(1))

    fun directBase(godotName: String): String? = inheritanceTree.directBase(godotName)
    fun allBases(godotName: String): List<String> = inheritanceTree.collectAllBases(godotName)
    fun inherits(godotName: String, baseName: String): Boolean = inheritanceTree.inherits(godotName, baseName)

    fun resolveEnumConstant(parentClass: String?, enumName: String, value: Long): String? =
        enumConstantResolver.resolveConstant(parentClass, enumName, value)

    /**
     * Like [resolveEnumConstant] but logs a WARN when multiple aliases share the same value.
     * Use this in call-sites where the choice of alias is surprising (e.g., indexed property indices).
     */
    fun resolveEnumConstantUnambiguous(
        parentClass: String?,
        enumName: String,
        value: Long,
        context: String = "",
    ): String? = enumConstantResolver.resolveConstantUnambiguous(parentClass, enumName, value, context)

    fun getConstantEnumNamesFor(parentClass: String?, enumName: String) =
        enumConstantResolver.getAllConstantsNames(parentClass, enumName)

    fun isExperimentalType(className: String, memberName: String? = null): Boolean =
        experimentalTypesRegistry.isExperimental(className, memberName)

    fun getReasonOfExperimental(className: String, memberName: String? = null): String? =
        experimentalTypesRegistry.getReason(className, memberName)

    fun findBuiltinClass(name: String): BuiltinClass? = model.builtinsByName[name]?.raw
    fun findResolvedBuiltinClass(name: String): ResolvedBuiltinClass? = model.builtinsByName[name]
    fun findEngineClass(name: String): EngineClass? = model.engineClassesByName[name]?.raw
    fun findResolvedEngineClass(name: String): ResolvedEngineClass? = model.engineClassesByName[name]

    /**
     * Returns the raw meta string for a member of a builtin class as recorded in
     * `builtin_class_member_offsets` for the active build configuration.
     *
     * Examples (float_64):
     * - resolveBuiltinMemberMeta("Vector3", "x") = "float"   → kotlin.Float
     * - resolveBuiltinMemberMeta("Vector2i", "x") = "int32"  → kotlin.Int
     * - resolveBuiltinMemberMeta("Color", "r")    = "float"   → kotlin.Float
     *
     * Returns null for opaque builtins (String, StringName, etc.) that have no member offsets.
     */
    fun resolveBuiltinMemberMeta(className: String, member: String): String? =
        findResolvedBuiltinClass(className)?.layout?.memberMeta?.get(member)

    fun findConstructor(className: String, argCount: Int): BuiltinClass.Constructor? =
        findResolvedBuiltinClass(className)
            ?.constructors
            ?.firstOrNull { it.raw != null && it.arguments.size == argCount }?.raw

    fun resolveConstructor(className: String, rawArgs: List<String>): ResolvedBuiltinConstructor? {
        val constructors = findResolvedBuiltinClass(className)?.constructors.orEmpty()
            .filter { it.arguments.size == rawArgs.size }
        if (constructors.isEmpty()) return null
        return constructors.maxByOrNull { constructor ->
            constructor.arguments.zip(rawArgs).sumOf { (arg, rawArg) ->
                scoreConstructorArgument(rawArg.trim(), arg.type)
            }
        }
    }

    companion object {
        fun buildFromApi(
            api: ExtensionApi,
            extensionInterface: GDExtensionInterface,
            rootPackage: String,
            packageRegistryFactory: PackageRegistryFactory,
            options: CodegenOptions = CodegenOptions(),
        ): Context {
            val godotVersion = GodotVersion(api.header)
            val buildConfiguration = options.resolveBuildConfiguration(api.header.precision)
            val inheritanceTree = buildInheritanceTree(api)
            val enumResolver = EnumConstantResolver.build(api)
            val model = ResolvedApiModel(
                buildConfiguration = buildConfiguration,
                builtins = resolveBuiltins(api, buildConfiguration),
                engineClasses = resolveEngineClasses(api),
                globalEnums = resolveGlobalEnums(api),
                nativeStructures = api.nativeStructures.map(::ResolvedNativeStructure),
                runtimeInterfaces = extensionInterface.interfaces.map { iface ->
                    ResolvedRuntimeInterface(iface, prefixOf(iface))
                },
            )
            val packageRegistry = packageRegistryFactory(rootPackage, model)
            val experimentalTypesRegistry = if (godotVersion.compareTo(4, 6, 1) == 0) {
                ExperimentalTypesRegistry.v4_6_1
            } else {
                error("Missing experimental types registry for Godot version $godotVersion")
            }
            return Context(
                extensionApi = api,
                extensionInterface = extensionInterface,
                model = model,
                inheritanceTree = inheritanceTree,
                enumConstantResolver = enumResolver,
                experimentalTypesRegistry = experimentalTypesRegistry,
                godotVersion = godotVersion,
                packageRegistry = packageRegistry,
                options = options,
            )
        }

        private fun buildInheritanceTree(api: ExtensionApi): InheritanceTree = InheritanceTree().also { tree ->
            api.classes.forEach { cls ->
                cls.inherits?.takeIf(String::isNotBlank)?.let { base -> tree.insert(derived = cls.name, base = base) }
            }
        }

        private fun resolveBuiltins(
            api: ExtensionApi,
            buildConfiguration: BuildConfiguration,
        ): List<ResolvedBuiltinClass> {
            val sizeMap = api.builtinClassSizes
                .firstOrNull { it.buildConfiguration == buildConfiguration.jsonName }
                ?.sizes
                ?.associate { it.name to it.size }
                .orEmpty()

            // Build a name -> members map upfront so resolveBuiltinAlign can recurse into it
            val membersMap = api.builtinClassMemberOffsets
                .firstOrNull { it.buildConfiguration == buildConfiguration.jsonName }
                ?.classes
                ?.associate { it.name to it.members }
                .orEmpty()

            val offsetsMap = api.builtinClassMemberOffsets
                .firstOrNull { it.buildConfiguration == buildConfiguration.jsonName }
                ?.classes
                ?.associate { cls ->
                    cls.name to ResolvedBuiltinLayout(
                        className = cls.name,
                        buildConfiguration = buildConfiguration,
                        size = sizeMap[cls.name]
                            ?: error("Missing size for builtin ${cls.name} in ${buildConfiguration.jsonName}"),
                        align = resolveBuiltinAlign(cls.name, membersMap, buildConfiguration),
                        memberOffsets = cls.members.associate { it.member to it.offset },
                        memberMeta = cls.members.associate { it.member to it.meta },
                    )
                }
                .orEmpty()

            return api.builtinClasses.map { builtin ->
                ResolvedBuiltinClass(
                    raw = builtin,
                    layout = offsetsMap[builtin.name] ?: sizeMap[builtin.name]?.let { size ->
                        ResolvedBuiltinLayout(
                            className = builtin.name,
                            buildConfiguration = buildConfiguration,
                            size = size,
                            align = resolveBuiltinAlignOpaque(builtin.name, size, buildConfiguration),
                            memberOffsets = emptyMap(),
                            memberMeta = emptyMap(),
                        )
                    },
                    constructors = builtin.constructors.map { ctor ->
                        ResolvedBuiltinConstructor(
                            raw = ctor,
                            ownerName = builtin.name,
                            arguments = ctor.arguments,
                            runtimeFunctionName = builtinRuntimeFunctionName(builtin.name, ctor.index),
                        )
                    } + syntheticBuiltinConstructorsFor(builtin.name),
                    enums = builtin.enums.map { enum ->
                        ResolvedEnum(
                            name = "${builtin.name}.${enum.name}",
                            shortName = enum.shortName,
                            ownerName = builtin.name,
                            raw = enum,
                        )
                    },
                )
            }
        }

        private fun resolveEngineClasses(api: ExtensionApi): List<ResolvedEngineClass> {
            val singletonNames = api.singletons.mapTo(hashSetOf()) { it.name }
            return api.classes.map { cls ->
                val isSingleton = cls.name in singletonNames
                ResolvedEngineClass(
                    raw = cls,
                    isSingleton = isSingleton,
                    isSingletonExtensible = isSingleton && api.classes.any { it.inherits == cls.name },
                    enums = cls.enums.map { enum ->
                        ResolvedEnum(
                            name = "${cls.name}.${enum.name}",
                            shortName = enum.shortName,
                            ownerName = cls.name,
                            raw = enum,
                        )
                    },
                )
            }
        }

        private fun resolveGlobalEnums(api: ExtensionApi): List<ResolvedEnum> = api.globalEnums.map { enum ->
            ResolvedEnum(
                name = enum.name,
                shortName = enum.shortName,
                ownerName = enum.ownerName,
                raw = enum,
            )
        }

        private fun builtinRuntimeFunctionName(className: String, constructorIndex: Int): String? = when (className) {
            "String" -> when (constructorIndex) {
                0 -> "initializeStringEmpty"
                1 -> "initializeStringCopy"
                2 -> "initializeStringFromStringName"
                3 -> "initializeStringFromNodePath"
                else -> null
            }

            "StringName" -> when (constructorIndex) {
                0 -> "initializeStringNameEmpty"
                1 -> "initializeStringNameCopy"
                2 -> "initializeStringNameFromString"
                else -> null
            }

            "NodePath" -> when (constructorIndex) {
                0 -> "initializeNodePathEmpty"
                1 -> "initializeNodePathCopy"
                2 -> "initializeNodePathFromString"
                else -> null
            }

            "Variant" -> when (constructorIndex) {
                0 -> "initializeVariantNil"
                1 -> "initializeVariantCopy"
                else -> null
            }

            else -> null
        }

        private fun syntheticBuiltinConstructorsFor(className: String): List<ResolvedBuiltinConstructor> =
            when (className) {
                "String" -> listOf(
                    ResolvedBuiltinConstructor(
                        raw = null,
                        ownerName = className,
                        arguments = listOf(MethodArg("value", "kotlin.String")),
                        runtimeFunctionName = "initializeStringFromUtf8",
                        usesKotlinStringBridge = true,
                    ),
                )

                "StringName" -> listOf(
                    ResolvedBuiltinConstructor(
                        raw = null,
                        ownerName = className,
                        arguments = listOf(MethodArg("value", "kotlin.String")),
                        runtimeFunctionName = "initializeStringNameFromUtf8",
                        usesKotlinStringBridge = true,
                    ),
                )

                "NodePath" -> listOf(
                    ResolvedBuiltinConstructor(
                        raw = null,
                        ownerName = className,
                        arguments = listOf(MethodArg("value", "kotlin.String")),
                        runtimeFunctionName = "initializeNodePathFromString",
                        usesKotlinStringBridge = true,
                    ),
                )

                else -> emptyList()
            }

        private fun scoreConstructorArgument(rawArg: String, expectedType: String): Int {
            val cleanExpectedType = expectedType.removePrefix("enum::").removePrefix("bitfield::")
            return when {
                rawArg.startsWith("^") && rawArg.endsWith('"') && cleanExpectedType == "NodePath" -> 100

                rawArg.startsWith("&") && rawArg.endsWith('"') && cleanExpectedType == "StringName" -> 100

                rawArg.startsWith('"') && rawArg.endsWith('"') && cleanExpectedType in setOf(
                    "String",
                    "kotlin.String",
                ) ->
                    100

                rawArg == "true" || rawArg == "false" -> if (cleanExpectedType == "bool") 100 else 0

                rawArg.matches(Regex("-?\\d+")) -> when (cleanExpectedType) {
                    "int", "int32_t", "int64_t", "uint32_t", "uint64_t" -> 80
                    "float", "double", "real_t" -> 60
                    else -> 0
                }

                rawArg.matches(Regex("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?")) -> when (cleanExpectedType) {
                    "float", "double", "real_t" -> 80
                    else -> 0
                }

                rawArg.contains('(') && rawArg.endsWith(')') -> {
                    val rawType = rawArg.substringBefore('(')
                    if (rawType == cleanExpectedType) 100 else 0
                }

                rawArg == "[]" && cleanExpectedType.contains("Array") -> 70

                rawArg == "{}" && cleanExpectedType == "Dictionary" -> 70

                cleanExpectedType == "Variant" -> 1

                else -> 0
            }
        }

        /**
         * Derives the alignment for a builtin class that HAS member offsets in the JSON.
         *
         * The alignment of a struct equals the maximum alignment of its leaf primitive members.
         * Recursion terminates at meta types that map directly to a primitive size:
         *   - "float"  -> buildConfiguration.realAlign  (4 for single, 8 for double)
         *   - "double" -> 8 always
         *   - "int32"  -> 4 always
         *   - "int64"  -> 8 always
         *
         * For members whose meta is another builtin (e.g. Rect2's members have meta "Vector2"),
         * we recurse into that builtin's members.
         *
         * Guarded against cycles with a visited set (Godot builtins have no cycles, but defensive).
         */
        private fun resolveBuiltinAlign(
            className: String,
            membersMap: Map<String, List<BuiltinClassMemberOffsets.Members>>,
            buildConfiguration: BuildConfiguration,
            visited: Set<String> = emptySet(),
        ): Int {
            if (className in visited) error("Cycle detected resolving align for '$className'")
            val members = membersMap[className]
                ?: return resolveBuiltinAlignOpaque(className, -1, buildConfiguration)

            return members.maxOf { member ->
                resolveMetaAlign(member.meta, membersMap, buildConfiguration, visited + className)
            }
        }

        /**
         * Resolves the alignment contributed by a single member meta type.
         */
        private fun resolveMetaAlign(
            meta: String,
            membersMap: Map<String, List<BuiltinClassMemberOffsets.Members>>,
            buildConfiguration: BuildConfiguration,
            visited: Set<String>,
        ): Int = when (meta) {
            "float" -> buildConfiguration.realAlign
            "double" -> 8
            "int32" -> 4
            "int64" -> 8
            else -> resolveBuiltinAlign(meta, membersMap, buildConfiguration, visited)
        }

        /**
         * Alignment for builtins that have NO member offsets in the JSON (opaque types).
         *
         * These are types whose internals are hidden (String, StringName, NodePath, Array,
         * PackedXxxArray, Dictionary, Callable, Signal, Object, RID...).
         *
         * Rules:
         * - RID: always 8 (it's a uint64 internally regardless of build config)
         * - size <= 4: pointer_align capped to 4 (float_32/double_32 have 4-byte pointers)
         * - everything else: pointerAlign from BuildConfiguration
         */
        private fun resolveBuiltinAlignOpaque(
            className: String,
            size: Int,
            buildConfiguration: BuildConfiguration,
        ): Int = when {
            className == "RID" -> 8
            size in 1..4 -> minOf(buildConfiguration.pointerAlign, 4)
            else -> buildConfiguration.pointerAlign
        }
    }
}
