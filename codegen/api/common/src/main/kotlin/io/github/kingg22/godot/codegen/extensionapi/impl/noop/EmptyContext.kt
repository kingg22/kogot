package io.github.kingg22.godot.codegen.extensionapi.impl.noop

import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.ExperimentalTypesRegistry
import io.github.kingg22.godot.codegen.extensionapi.resolver.EnumConstantResolver
import io.github.kingg22.godot.codegen.models.config.BuildConfiguration
import io.github.kingg22.godot.codegen.models.config.CodegenOptions
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensionapi.Header
import io.github.kingg22.godot.codegen.models.extensionapi.domain.GodotVersion
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedApiModel
import io.github.kingg22.godot.codegen.services.PackageRegistry

/** Special context for test */
@Suppress("ktlint:standard:function-naming", "FunctionName")
fun EmptyContext(packageRegistry: PackageRegistry = NoOpPackageRegistry("")): Context {
    val header = Header(0, 0, 0, "test", "test", "Test version", "single")
    return Context(
        extensionApi = ExtensionApi(header = header),
        enumConstantResolver = EnumConstantResolver.empty(),
        experimentalTypesRegistry = ExperimentalTypesRegistry.empty,
        godotVersion = GodotVersion(header),
        packageRegistry = packageRegistry,
        model = ResolvedApiModel(BuildConfiguration.defaultFor("single", 64)),
        options = CodegenOptions(),
    )
}
