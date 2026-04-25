package io.github.kingg22.godot.codegen.extensionapi

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface

/**
 * Generates the platform-specific implementation bodies for a specific [Backend].
 *
 * Each backend produces its own impl (cinterop calls, JNI bindings, FFM MemorySegment, …).
 */
interface CodeImplGenerator {
    val typeResolver: TypeResolver

    context(ctx: Context, gdeInterface: GDExtensionInterface?)
    fun generate(api: ExtensionApi): Sequence<FileSpec>
}
