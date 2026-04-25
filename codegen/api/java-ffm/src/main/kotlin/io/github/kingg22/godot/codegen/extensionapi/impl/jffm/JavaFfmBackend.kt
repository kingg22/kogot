package io.github.kingg22.godot.codegen.extensionapi.impl.jffm

import io.github.kingg22.godot.codegen.extensionapi.Backend
import io.github.kingg22.godot.codegen.extensionapi.CachedTypeResolver
import io.github.kingg22.godot.codegen.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver

class JavaFfmBackend(
    override val typeResolver: TypeResolver = CachedTypeResolver(JavaFfmTypeResolver()),
    override val codeImplGenerator: CodeImplGenerator = JavaFfmImplGenerator(typeResolver),
) : Backend {
    override val name: String get() = "java-ffm"
}
