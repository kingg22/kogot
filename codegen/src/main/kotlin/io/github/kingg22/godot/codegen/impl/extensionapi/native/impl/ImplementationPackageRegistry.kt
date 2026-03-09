package io.github.kingg22.godot.codegen.impl.extensionapi.native.impl

import io.github.kingg22.godot.codegen.impl.extensionapi.PackageRegistry
import io.github.kingg22.godot.codegen.impl.runtime.RuntimePackageRegistry
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface

class ImplementationPackageRegistry(packageStr: String, interfaceModel: GDExtensionInterface) : PackageRegistry {
    private val runtimeBindingPackageRegistry = RuntimePackageRegistry(packageStr, interfaceModel)
    override val rootPackage: String = runtimeBindingPackageRegistry.rootPackage

    private val bindingPackage = runtimeBindingPackageRegistry.rootPackage
    private val builtinInternalBind = if (packageStr.endsWith(".api")) {
        "$packageStr.builtin.internal"
    } else {
        "$packageStr.api.builtin.internal"
    }

    private val typeToPackage: Map<String, String> = buildMap {
        put("allocConstTypePtrArray", bindingPackage)
        put("allocateBuiltinStorage", bindingPackage)
        put("allocGdBool", bindingPackage)
        put("readGdBool", bindingPackage)
        put("allocateBuiltinStorage", builtinInternalBind)
        put("freeBuiltinStorage", builtinInternalBind)
    }

    override fun packageFor(godotName: String): String? = typeToPackage[godotName]
        ?: runtimeBindingPackageRegistry.packageFor(godotName)
}
