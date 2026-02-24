package io.github.kingg22.godot.codegen.impl

import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.gextensioninterface.GDExtensionInterface
import java.nio.file.Path

class KotlinPoetGenerator(packageName: String) {
    private val interfaceGenerator = GDExtensionInterfaceGenerator(packageName)
    private val extensionApiGenerator = ExtensionApiGenerator(packageName)

    fun generate(api: GDExtensionInterface, outputDir: Path): List<Path> = interfaceGenerator.generate(api, outputDir)

    fun generate(api: ExtensionApi, outputDir: Path): List<Path> = extensionApiGenerator.generate(api, outputDir)
}
