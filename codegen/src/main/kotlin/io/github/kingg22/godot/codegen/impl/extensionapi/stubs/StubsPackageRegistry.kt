package io.github.kingg22.godot.codegen.impl.extensionapi.stubs

import com.squareup.kotlinpoet.ClassName
import io.github.kingg22.godot.codegen.impl.extensionapi.PackageRegistry
import io.github.kingg22.godot.codegen.impl.extensionapi.PackageRegistryFactory

class StubsPackageRegistry(override val rootPackage: String) : PackageRegistry {
    override fun packageFor(godotName: String): String = rootPackage

    override fun classNameFor(godotName: String, vararg kotlinName: String): ClassName =
        ClassName(rootPackage, *kotlinName)

    override fun classNameForOrNull(godotName: String, vararg kotlinName: String): ClassName =
        classNameFor(godotName, *kotlinName)

    override fun classNameForOrDefault(godotName: String, vararg kotlinName: String): ClassName =
        classNameFor(godotName, *kotlinName)

    override fun packageForOrDefault(godotName: String): String = rootPackage

    override fun packageForUtilityFun(): String = rootPackage

    companion object {
        val factory: PackageRegistryFactory = { rootPackage, _ -> StubsPackageRegistry(rootPackage) }
    }
}
