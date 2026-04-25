package io.github.kingg22.godot.codegen.models.extensionapi.domain

import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg

data class ResolvedBuiltinConstructor(
    val raw: BuiltinClass.Constructor?,
    val ownerName: String,
    val arguments: List<MethodArg>,
    val runtimeFunctionName: String? = null,
    val usesKotlinStringBridge: Boolean = false,
) {
    val index: Int get() = raw?.index ?: -1
    val argumentTypes: List<String> get() = arguments.map { it.type }
}
