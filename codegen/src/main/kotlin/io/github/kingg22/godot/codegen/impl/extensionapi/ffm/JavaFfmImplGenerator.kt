package io.github.kingg22.godot.codegen.impl.extensionapi.ffm

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi

/** Generates Java FFM API implementation bodies (Java FFM bindings, MemorySegment, Arena). */
class JavaFfmImplGenerator(override val typeResolver: TypeResolver) : CodeImplGenerator.ImplGenerator {

    // TODO: generate Java FFM for classes, builtins, utility functions
    context(context: Context)
    override fun generate(api: ExtensionApi): Sequence<FileSpec> = sequenceOf()
}
