package io.github.kingg22.godot.codegen.impl.extensionapi.native

import io.github.kingg22.godot.codegen.impl.extensionapi.Backend
import io.github.kingg22.godot.codegen.impl.extensionapi.CachedTypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver

class KotlinNativeBackend(
    override val typeResolver: TypeResolver = CachedTypeResolver(KotlinNativeTypeResolver()),
    override val codeImplGenerator: CodeImplGenerator = KotlinNativeImplGenerator(typeResolver),
) : Backend {
    override val name: String get() = "kotlin-native"
}
