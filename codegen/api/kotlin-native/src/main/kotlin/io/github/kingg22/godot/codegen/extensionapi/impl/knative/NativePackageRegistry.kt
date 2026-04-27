package io.github.kingg22.godot.codegen.extensionapi.impl.knative

import com.squareup.kotlinpoet.ClassName
import io.github.kingg22.godot.codegen.extensionapi.InheritanceTree
import io.github.kingg22.godot.codegen.extensionapi.PackageRegistryFactory
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.NativeBuiltinClassGenerator
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.services.PackageRegistry
import io.github.kingg22.godot.codegen.utils.currentClassLogger
import io.github.kingg22.godot.codegen.utils.info

/**
 * Maps every Godot type name to the Kotlin package where it will be generated.
 *
 * Built once alongside [io.github.kingg22.godot.codegen.extensionapi.Context].
 *
 * Consulted by [io.github.kingg22.godot.codegen.extensionapi.TypeResolver] implementations and all code generators
 * to produce correct [ClassName] references.
 *
 * The resolver handles types NOT registered here (primitives, void, etc.) directly without a package lookup.
 */
class NativePackageRegistry private constructor(private val typeToPackage: Map<String, String>, rootPackage: String) :
    PackageRegistry {
    override val rootPackage = if (rootPackage.endsWith(".api")) rootPackage else "$rootPackage.api"

    init {
        currentClassLogger().info { "INFO: Native Package Registry created with ${typeToPackage.size} entries" }
    }

    override fun packageFor(godotName: String): String? = typeToPackage[godotName]

    override fun packageForUtilObject(): String = "$rootPackage.utils"

    override fun classNameOfExperimentalAnnotation(): ClassName = ClassName(rootPackage, "ExperimentalGodotApi")

    companion object {
        val factory: PackageRegistryFactory = { rootPackage, model ->
            val inheritanceTree = InheritanceTree().also { tree ->
                model.engineClasses.forEach { resolved ->
                    resolved.raw.inherits?.takeIf { it.isNotBlank() }?.let { base ->
                        tree.insert(derived = resolved.name, base = base)
                    }
                }
            }

            val map = mutableMapOf<String, String>()

            fun isSingleton(name: String) = name in model.singletonNames
            fun renameQualified(name: String): String {
                if (!name.contains('.')) return name.renameGodotClass()
                return name.split('.').joinToString(".") { it.renameGodotClass() }
            }

            fun register(name: String, pkg: String) {
                map[name] = pkg
                val renamed = renameQualified(name)
                if (renamed != name) map[renamed] = pkg
            }

            // FFI RAW special case
            register("GDExtensionInitializationFunction", "$rootPackage.internal.ffi")

            // Builtins
            // Variant is injected manually in Context but not in builtinClasses
            register("Variant", "$rootPackage.api.builtin")

            model.builtinTypes.forEach { cls ->
                if (cls in NativeBuiltinClassGenerator.SKIPPED_TYPES) {
                    val kotlinName = when (cls.lowercase()) {
                        "int" -> "kotlin.Long"
                        "float" -> "kotlin.Double"
                        "bool" -> "kotlin.Boolean"
                        "nil" -> "null"
                        else -> error("Unexpected builtin type: $cls")
                    }
                    register(cls, kotlinName)
                } else {
                    register(cls, "$rootPackage.api.builtin")

                    // NUEVO: Registrar typealiases para clases genéricas
                    when (cls) {
                        "Array" -> register("VariantArray", "$rootPackage.api.builtin")
                        "Dictionary" -> register("VariantDictionary", "$rootPackage.api.builtin")
                    }
                }
            }

            // Engine classes
            model.classApiTypes.forEach { (name, apiType) ->
                val pkg = when {
                    isSingleton(name) -> "$rootPackage.api.singleton"

                    // los 79 confiables
                    apiType == "editor" -> "$rootPackage.api.editor"

                    else -> {
                        val sub = resolveCoreSubpackage(name, inheritanceTree)
                        val subPack = sub?.let { ".$sub" }.orEmpty()
                        "$rootPackage.api.core$subPack"
                    }
                }
                register(name, pkg)
            }

            model.nestedEnumOwners.forEach { (cls, enum) ->
                val pkg = map[cls]
                    ?: error("Class $cls for nested enum $enum not found in PackageRegistry build: $map")
                register("$cls.$enum", pkg)
            }

            // Global enums
            model.globalEnumTypes.forEach { enum ->
                register(enum, "$rootPackage.api.global")
            }

            // Native structures — manual impl, but types must be resolvable
            model.nativeStructuresByName.keys.forEach { ns ->
                register(ns, "$rootPackage.api.native")
            }

            // Utility functions → no types per se, but category packages
            // are registered separately if needed

            NativePackageRegistry(map, rootPackage)
        }

        /**
         * Determines the core sub-package for a non-singleton, non-editor class.
         *
         * Strategy: walk up to the last ancestor before Object (depth-1 root).
         * That root name → sub-package name.
         */
        private fun resolveCoreSubpackage(godotName: String, inheritanceTree: InheritanceTree): String? {
            val bases = inheritanceTree.collectAllBases(godotName)
            // bases = [DirectParent, …, RootBeforeObject, Object]
            // We want the root before "Object"
            val rootAncestor = bases.dropLastWhile { it == "Object" }.lastOrNull()
                ?: return null

            return when (rootAncestor) {
                "Node" -> "node"
                "Resource" -> "resource"
                "RefCounted" -> "refcounted"
                else -> null
            }
        }
    }
}
