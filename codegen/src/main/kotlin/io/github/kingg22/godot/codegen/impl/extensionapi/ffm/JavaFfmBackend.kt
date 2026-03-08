package io.github.kingg22.godot.codegen.impl.extensionapi.ffm

import io.github.kingg22.godot.codegen.impl.extensionapi.Backend
import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver

class JavaFfmBackend(
    override val typeResolver: TypeResolver = JavaFfmTypeResolver(),
    override val codeImplGenerator: CodeImplGenerator = JavaFfmImplGenerator(typeResolver),
) : Backend {
    override val name: String get() = "java-ffm"
}
