package io.github.kingg22.godot.codegen.extensionapi.impl.noop

import com.squareup.kotlinpoet.ClassName
import io.github.kingg22.godot.codegen.services.PackageRegistry

class NoOpPackageRegistry(override val rootPackage: String) : PackageRegistry {
    override fun packageFor(godotName: String): String = rootPackage
    override fun packageForUtilObject(): String = rootPackage
    override fun classNameOfExperimentalAnnotation(): ClassName = ClassName(rootPackage, "ExperimentalGodotApi")
}
