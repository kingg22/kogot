package io.github.kingg22.godot.codegen.impl.extensionapi

import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.inputStream

/**
 * Locates the `godot-version/` directory relative to the project root.
 *
 * Gradle sets `user.dir` to the subproject directory (e.g. `.../kogot/codegen`),
 * so we walk up until we find the marker directory.
 */
private fun godotVersionRoot(godotVersion: String): Path {
    val repoRoot = System.getProperty("kogot.repo.root")
        ?: error(
            "System property 'kogot.repo.root' not set. " +
                "Add systemProperty(\"kogot.repo.root\", rootProject.projectDir.absolutePath) " +
                "to the test task in codegen/build.gradle.kts",
        )
    return Path(repoRoot, "godot-version", godotVersion)
}

@OptIn(ExperimentalSerializationApi::class)
private val json = Json { prettyPrint = true }

@OptIn(ExperimentalSerializationApi::class)
fun loadExtensionApi(godotVersion: String = "v4_6_1"): ExtensionApi {
    val path = godotVersionRoot(godotVersion).resolve("extension_api.json")
    return path.inputStream().use { json.decodeFromStream(it) }
}

@OptIn(ExperimentalSerializationApi::class)
fun loadExtensionInterface(godotVersion: String = "v4_6_1"): GDExtensionInterface {
    val path = godotVersionRoot(godotVersion).resolve("gdextension_interface.json")
    return path.inputStream().use { json.decodeFromStream(it) }
}
