package io.github.kingg22.godot.codegen.extensionapi.impl.jffm

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface

/** Generates Java FFM API implementation bodies (Java FFM bindings, MemorySegment, Arena). */
class JavaFfmImplGenerator(override val typeResolver: TypeResolver) : CodeImplGenerator {
    context(ctx: Context, gdeInterface: GDExtensionInterface?)
    override fun generate(api: ExtensionApi): Sequence<FileSpec> = sequenceOf()
}
