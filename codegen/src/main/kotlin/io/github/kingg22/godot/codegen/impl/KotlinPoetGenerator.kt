package io.github.kingg22.godot.codegen.impl

import io.github.kingg22.godot.codegen.impl.extensionapi.Backend
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.PackageRegistryFactory
import io.github.kingg22.godot.codegen.impl.extensionapi.ffm.JavaFfmBackend
import io.github.kingg22.godot.codegen.impl.extensionapi.ffm.JavaFfmPackageRegistry
import io.github.kingg22.godot.codegen.impl.extensionapi.native.KotlinNativeBackend
import io.github.kingg22.godot.codegen.impl.extensionapi.native.NativePackageRegistry
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import io.github.kingg22.godot.codegen.models.internal.CodegenOptions
import io.github.kingg22.godot.codegen.models.internal.GeneratorBackend

class KotlinPoetGenerator(
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

    fun generate(api: ExtensionApi, extensionInterface: GDExtensionInterface, options: CodegenOptions) = context(
        Context.buildFromApi(api, extensionInterface, packageName, packageRegistryFactory, options),
    ) {
        backend.generateAll(api)
    }
}
