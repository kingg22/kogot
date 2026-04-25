package io.github.kingg22.godot.codegen.impl

import io.github.kingg22.godot.codegen.extensionapi.Backend
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.PackageRegistryFactory
import io.github.kingg22.godot.codegen.extensionapi.impl.jffm.JavaFfmBackend
import io.github.kingg22.godot.codegen.extensionapi.impl.jffm.JavaFfmPackageRegistry
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.KotlinNativeBackend
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.NativePackageRegistry
import io.github.kingg22.godot.codegen.models.config.CodegenOptions
import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface

@ConsistentCopyVisibility
data class KotlinPoetGenerator private constructor(
    private val packageName: String,
    private val backend: Backend,
    private val packageRegistryFactory: PackageRegistryFactory,
) {
    constructor(packageName: String, backend: GeneratorBackend) : this(
        packageName,
        when (backend) {
            GeneratorBackend.JAVA_FFM -> JavaFfmBackend()
            GeneratorBackend.KOTLIN_NATIVE -> KotlinNativeBackend()
        },
        when (backend) {
            GeneratorBackend.JAVA_FFM -> JavaFfmPackageRegistry.factory
            GeneratorBackend.KOTLIN_NATIVE -> NativePackageRegistry.factory
        },
    )

    fun generate(api: ExtensionApi, extensionInterface: GDExtensionInterface?, options: CodegenOptions) = context(
        Context.buildFromApi(api, packageName, packageRegistryFactory, options),
        extensionInterface,
    ) {
        backend.generateAll(api)
    }
}
