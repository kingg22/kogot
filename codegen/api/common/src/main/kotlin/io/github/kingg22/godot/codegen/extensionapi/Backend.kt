package io.github.kingg22.godot.codegen.extensionapi

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface

/**
 * A **Backend** owns everything target-specific:
 * its [TypeResolver], its [CodeImplGenerator].
 *
 * The pipeline calls [generateAll] once per backend after the [Context] is built.
 *
 * New targets (Kotlin Native, Java FFM, Java JNI, …) implement this interface.
 */
interface Backend {
    val name: String

    val typeResolver: TypeResolver
    val codeImplGenerator: CodeImplGenerator

    /**
     * Generates all files for [api]
     *
     * The [context] is the shared, immutable API context built before this call.
     *
     * Returns the list of paths written.
     */
    context(ctx: Context, gdeInterface: GDExtensionInterface?)
    fun generateAll(api: ExtensionApi): Sequence<FileSpec> = codeImplGenerator.generate(api)
}
