package io.github.kingg22.godot.codegen.impl.runtime

import com.squareup.kotlinpoet.ClassName
import io.github.kingg22.godot.codegen.impl.extensionapi.PackageRegistry
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface

class RuntimePackageRegistry(packageStr: String, interfaceModel: GDExtensionInterface) : PackageRegistry {
    override val rootPackage: String = if (packageStr.endsWith(".internal.binding")) {
        packageStr
    } else if (packageStr.endsWith(".api")) {
        "${packageStr.removeSuffix(".api")}.internal.binding"
    } else {
        "$packageStr.internal.binding"
    }

    private val typeToPackage: Map<String, String> = buildMap {
        put("BindingProcAddressHolder", rootPackage)
        put("BindingInterop", rootPackage)
        put("InternalBinding", rootPackage)
        put("BindingStatus", rootPackage)
        put("BindingBooleanResult", rootPackage)
        interfaceModel.interfaces
            .map { it.name.substringBefore('_') }
            .distinct()
            .forEach { prefix ->
                put(implementationNameForPrefix(prefix), rootPackage)
            }
    }

    override fun packageFor(godotName: String): String? = typeToPackage[godotName]

    fun bindingClassName(prefix: String): ClassName = classNameForOrDefault(implementationNameForPrefix(prefix))

    fun statusClassName(): ClassName = classNameForOrDefault("BindingStatus")

    fun booleanResultClassName(): ClassName = classNameForOrDefault("BindingBooleanResult")

    fun callErrorInfoClassName(): ClassName = classNameForOrDefault("BindingCallErrorInfo")
}
