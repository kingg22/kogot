package io.github.kingg22.kogot.gradle.compiler

import io.github.kingg22.kogot.gradle.KogotConventions
import io.github.kingg22.kogot.gradle.KogotExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Unit tests for [KogotCompilerGradlePlugin]. Gradle properties are written to a real `gradle.properties`
 * (same technique as `KogotExtensionTest`), since that is what `providers.gradleProperty(...)` reads.
 */
class KogotCompilerGradlePluginTest {
    @TempDir
    lateinit var tempDir: File

    private fun applied(vararg properties: Pair<String, String>): Pair<KogotCompilerGradlePlugin, KogotCompilerExtension> {
        val projectDir = createTempDirectory(tempDir.toPath(), "project").toFile()
        if (properties.isNotEmpty()) {
            File(projectDir, "gradle.properties").writeText(
                properties.joinToString("\n") { (key, value) -> "$key=$value" },
            )
        }
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply(KogotCompilerGradlePlugin::class.java)
        val plugin = project.plugins.getPlugin(KogotCompilerGradlePlugin::class.java)
        return plugin to project.extensions.getByType(KogotCompilerExtension::class.java)
    }

    @Test
    fun `uses the compiler plugin id shared with the compiler plugin`() {
        val (plugin, _) = applied()
        assertEquals(KogotConventions.COMPILER_PLUGIN_ID, plugin.getCompilerPluginId())
    }

    @Test
    fun `resolves the compiler plugin artifact from kogot defaults`() {
        val (plugin, _) = applied()
        val artifact = plugin.getPluginArtifact()
        assertEquals(KogotConventions.DEFAULT_KOGOT_GROUP, artifact.groupId)
        assertEquals(KogotConventions.ARTIFACT_COMPILER_PLUGIN, artifact.artifactId)
        assertEquals(KogotConventions.DEFAULT_KOGOT_VERSION, artifact.version)
    }

    @Test
    fun `honors kogot group and version gradle properties`() {
        val (plugin, _) = applied(
            KogotConventions.PROP_KOGOT_GROUP to "com.example",
            KogotConventions.PROP_KOGOT_VERSION to "9.9.9",
        )
        val artifact = plugin.getPluginArtifact()
        assertEquals("com.example", artifact.groupId)
        assertEquals("9.9.9", artifact.version)
    }

    @Test
    fun `follows the kogot DSL group and version when the kogot extension exists`() {
        val projectDir = createTempDirectory(tempDir.toPath(), "project").toFile()
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        val kogot = project.extensions.create("kogot", KogotExtension::class.java, project)
        kogot.kogotGroup.set("org.custom")
        kogot.kogotVersion.set("1.2.3")
        project.pluginManager.apply(KogotCompilerGradlePlugin::class.java)

        val artifact = project.plugins.getPlugin(KogotCompilerGradlePlugin::class.java).getPluginArtifact()
        assertEquals("org.custom", artifact.groupId)
        assertEquals("1.2.3", artifact.version)
    }

    @Test
    fun `is enabled by default and can be disabled`() {
        val (_, extension) = applied()
        assertTrue(extension.enabled.get())

        val (_, disabled) = applied(KogotConventions.PROP_COMPILER_PLUGIN_ENABLED to "false")
        assertEquals(false, disabled.enabled.get())
    }
}
