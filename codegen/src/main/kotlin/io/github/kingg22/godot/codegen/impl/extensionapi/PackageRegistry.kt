package io.github.kingg22.godot.codegen.impl.extensionapi

import com.squareup.kotlinpoet.ClassName
import io.github.kingg22.godot.codegen.impl.renameGodotClass

typealias PackageRegistryFactory = (rootPackage: String, context: Context.IncompleteContext) -> PackageRegistry

interface PackageRegistry {
    val rootPackage: String

    /**
     * Returns the package for [godotName], or null if not registered
     * (caller should treat it as a primitive / external type).
     */
    fun packageFor(godotName: String): String?

    /**
     * Returns the [ClassName] for [godotName] using its registered package
     * and the [kotlinName] produced by [renameGodotClass].
     *
     * Throws if the type is not registered — forces all generated types
     * to be registered before code generation starts.
     */
    fun classNameFor(godotName: String, vararg kotlinName: String = arrayOf(godotName.renameGodotClass())): ClassName

    /**
     * @see classNameFor
     * @return null if not registered
     */
    fun classNameForOrNull(
        godotName: String,
        vararg kotlinName: String = arrayOf(godotName.renameGodotClass()),
    ): ClassName?

    /**
     * @see classNameFor
     * @return default package if not registered
     */
    fun classNameForOrDefault(
        godotName: String,
        vararg kotlinName: String = arrayOf(godotName.renameGodotClass()),
    ): ClassName

    /**
     * @see packageFor
     * @return default package if not registered
     */
    fun packageForOrDefault(godotName: String): String

    /**
     * @see packageFor
     * @return default string if not registered
     */
    fun packageForOrDefault(godotName: String, defaultName: String): String = packageFor(godotName) ?: defaultName

    fun packageForUtilityFun(): String

    fun packageForUtilObject(): String = packageForOrDefault("GD")

    fun classNameOfExperimentalAnnotation(): ClassName = classNameForOrDefault("ExperimentalGodotApi")
}
