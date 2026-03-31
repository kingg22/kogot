package io.github.kingg22.godot.codegen.impl.extensionapi

import io.github.kingg22.godot.codegen.impl.extensionapi.knative.resolver.EnumConstantResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.stubs.StubsPackageRegistry
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensionapi.Header
import io.github.kingg22.godot.codegen.models.extensionapi.domain.GodotVersion
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedApiModel
import io.github.kingg22.godot.codegen.models.internal.BuildConfiguration
import io.github.kingg22.godot.codegen.models.internal.CodegenOptions

/** Special context for test */
@Suppress("ktlint:standard:function-naming", "TestFunctionName")
fun EmptyContext(packageRegistry: PackageRegistry = StubsPackageRegistry("")): Context {
    val header = Header(0, 0, 0, "test", "test", "Test version", "single")
    return Context(
        extensionApi = ExtensionApi(header = header),
        enumConstantResolver = EnumConstantResolver.empty(),
        experimentalTypesRegistry = ExperimentalTypesRegistry.empty,
        godotVersion = GodotVersion(header),
        packageRegistry = packageRegistry,
        extensionInterface = null,
        model = ResolvedApiModel(BuildConfiguration.defaultFor("single", 64)),
        options = CodegenOptions(),
    )
}
