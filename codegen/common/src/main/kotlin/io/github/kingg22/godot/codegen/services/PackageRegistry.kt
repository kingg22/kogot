package io.github.kingg22.godot.codegen.services

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import io.github.kingg22.godot.codegen.impl.renameGodotClass

interface PackageRegistry {
    val rootPackage: String

    /**
     * Returns the package for [godotName], or null if not registered
     * (caller should treat it as a primitive / external type).
     */
    fun packageFor(godotName: String): String?

    /**
     * Returns the [com.squareup.kotlinpoet.ClassName] for [godotName] using its registered package
     * and the [kotlinName] produced by [renameGodotClass].
     *
     * Throws if the type is not registered — forces all generated types
     * to be registered before code generation starts.
     */
    fun classNameFor(godotName: String, vararg kotlinName: String = arrayOf(godotName.renameGodotClass())): ClassName {
        val pkg = packageFor(godotName) ?: error("Type '$godotName' is not registered in PackageRegistry")
        return ClassName(pkg, *kotlinName)
    }

    /**
     * @see classNameFor
     * @return null if not registered
     */
    fun classNameForOrNull(
        godotName: String,
        vararg kotlinName: String = arrayOf(godotName.renameGodotClass()),
    ): ClassName? = packageFor(godotName)?.let { ClassName(it, *kotlinName) }

    /**
     * @see classNameFor
     * @return default package if not registered
     */
    fun classNameForOrDefault(godotName: String, vararg kotlinName: String, typedClass: Boolean = false): ClassName {
        val kotlinNames = if (typedClass) {
            if (kotlinName.isEmpty()) {
                arrayOf(godotName.renameGodotClass(getTypedClass = true))
            } else {
                kotlinName.map { it.renameGodotClass(getTypedClass = true) }.toTypedArray()
            }
        } else {
            kotlinName.ifEmpty { arrayOf(godotName.renameGodotClass()) }
        }
        return classNameForOrNull(godotName, *kotlinNames) ?: ClassName(rootPackage, *kotlinNames)
    }

    fun memberNameForOrDefault(
        godotName: String,
        kotlinName: String? = null,
        typedClass: Boolean = false,
        isExtension: Boolean = false,
    ): MemberName {
        val kotlinNames: String = kotlinName?.renameGodotClass(typedClass) ?: godotName.renameGodotClass(typedClass)
        return packageFor(godotName)?.let { MemberName(it, kotlinNames, isExtension) }
            ?: MemberName(rootPackage, kotlinNames, isExtension)
    }

    /**
     * @see packageFor
     * @return default package if not registered
     */
    fun packageForOrDefault(godotName: String): String = packageFor(godotName) ?: rootPackage

    fun packageForUtilObject(): String = TODO("This package registry does not support utility functions")

    fun classNameOfExperimentalAnnotation(): ClassName =
        TODO("This package registry does not support experimental annotation")
}
