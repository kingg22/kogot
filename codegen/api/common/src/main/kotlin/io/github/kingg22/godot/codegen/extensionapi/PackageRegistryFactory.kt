package io.github.kingg22.godot.codegen.extensionapi

import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedApiModel
import io.github.kingg22.godot.codegen.services.PackageRegistry

typealias PackageRegistryFactory = (rootPackage: String, model: ResolvedApiModel) -> PackageRegistry
