package io.github.kingg22.godot.codegen.extensionapi.impl.jffm

import io.github.kingg22.godot.codegen.extensionapi.PackageRegistryFactory
import io.github.kingg22.godot.codegen.extensionapi.impl.noop.NoOpPackageRegistry
import io.github.kingg22.godot.codegen.services.PackageRegistry

class JavaFfmPackageRegistry(pkg: String) : PackageRegistry by NoOpPackageRegistry(pkg) {
    companion object {
        val factory: PackageRegistryFactory = { pkg, _ -> JavaFfmPackageRegistry(pkg) }
    }
}
