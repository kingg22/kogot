package io.github.kingg22.godot.codegen.impl.extensionapi.native

import com.squareup.kotlinpoet.ClassName
import io.github.kingg22.godot.codegen.impl.extensionapi.InheritanceTree
import io.github.kingg22.godot.codegen.impl.extensionapi.PackageRegistry
import io.github.kingg22.godot.codegen.impl.extensionapi.PackageRegistryFactory
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeBuiltinClassGenerator
import io.github.kingg22.godot.codegen.impl.renameGodotClass

/**
 * Maps every Godot type name to the Kotlin package where it will be generated.
 *
 * Built once alongside [io.github.kingg22.godot.codegen.impl.extensionapi.Context].
 *
 * Consulted by [io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver] implementations and all code generators
 * to produce correct [ClassName] references.
 *
 * The resolver handles types NOT registered here (primitives, void, etc.) directly without a package lookup.
 */
class NativePackageRegistry internal constructor(private val typeToPackage: Map<String, String>, rootPackage: String) :
    PackageRegistry {
    override val rootPackage = if (rootPackage.endsWith(".api")) rootPackage else "$rootPackage.api"

    init {
        println("INFO: Native PackageRegistry created with ${typeToPackage.size} entries")
    }

    override fun packageFor(godotName: String): String? = typeToPackage[godotName]

    override fun classNameFor(godotName: String, vararg kotlinName: String): ClassName {
        val pkg = packageFor(godotName) ?: error("Type '$godotName' is not registered in PackageRegistry")
        return ClassName(pkg, *kotlinName)
    }

    override fun packageForUtilityFun(): String = "$rootPackage.utils"

    override fun packageForUtilObject(): String = packageForUtilityFun()

    override fun classNameOfExperimentalAnnotation(): ClassName = ClassName(rootPackage, "ExperimentalGodotApi")

    companion object {
        val factory: PackageRegistryFactory = { rootPackage, context ->
            fun isSingleton(name: String) = name in context.singletons

            val map = mutableMapOf<String, String>()
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

            context.builtinTypes.forEach { cls ->
                if (cls in NativeBuiltinClassGenerator.SKIPPED_TYPES) {
                    val kotlinName = when (cls.lowercase()) {
                        "int" -> "kotlin.Int"
                        "float" -> "kotlin.Float"
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
            context.classesAndApiType.forEach { (name, apiType) ->
                val pkg = when {
                    isSingleton(name) -> "$rootPackage.api.singleton"

                    // los 79 confiables
                    apiType == "editor" -> "$rootPackage.api.editor"

                    else -> {
                        val sub = resolveCoreSubpackage(name, context.inheritanceTree)
                        val subPack = sub?.let { ".$sub" }.orEmpty()
                        "$rootPackage.api.core$subPack"
                    }
                }
                register(name, pkg)
            }

            context.nestedEnumsTypes.forEach { (cls, enum) ->
                val pkg = map[cls]
                    ?: error("Class $cls for nested enum $enum not found in PackageRegistry build: $map")
                register("$cls.$enum", pkg)
            }

            // Global enums
            context.globalEnumsTypes.forEach { enum ->
                register(enum, "$rootPackage.api.global")
            }

            // Native structures — manual impl, but types must be resolvable
            context.nativeStructureTypes.forEach { ns ->
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
