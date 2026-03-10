package io.github.kingg22.godot.codegen.models.extensionapi.domain

import io.github.kingg22.godot.codegen.models.extensionapi.EngineClass

data class ResolvedEngineClass(
    val raw: EngineClass,
    val isSingleton: Boolean,
    val isSingletonExtensible: Boolean,
    val enums: List<ResolvedEnum>,
) {
    val name: String get() = raw.name
    val shortName: String get() = raw.name.substringAfterLast('.')
    val apiType: String get() = raw.apiType
    val isRefcounted: Boolean get() = raw.isRefcounted
    val isInstantiable: Boolean get() = raw.isInstantiable
}
