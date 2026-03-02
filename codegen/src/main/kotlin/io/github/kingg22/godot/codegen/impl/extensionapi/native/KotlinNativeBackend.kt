package io.github.kingg22.godot.codegen.impl.extensionapi.native

import io.github.kingg22.godot.codegen.impl.extensionapi.Backend
import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver

class KotlinNativeBackend(
    packageName: String,
    override val typeResolver: TypeResolver = KotlinNativeTypeResolver(),
    override val codeImplGenerator: CodeImplGenerator = KotlinNativeImplGenerator(typeResolver, packageName),
) : Backend {
    override val name: String get() = "kotlin-native"
}
