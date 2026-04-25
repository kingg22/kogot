package io.github.kingg22.godot.codegen.extensionapi.impl.knative

import io.github.kingg22.godot.codegen.extensionapi.Backend
import io.github.kingg22.godot.codegen.extensionapi.CachedTypeResolver
import io.github.kingg22.godot.codegen.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver

class KotlinNativeBackend(
    override val typeResolver: TypeResolver = CachedTypeResolver(KotlinNativeTypeResolver()),
    override val codeImplGenerator: CodeImplGenerator = KotlinNativeImplGenerator(typeResolver),
) : Backend {
    override val name: String get() = "kotlin-native"
}
