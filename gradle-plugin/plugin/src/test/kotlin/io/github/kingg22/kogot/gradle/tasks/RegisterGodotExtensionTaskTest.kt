package io.github.kingg22.kogot.gradle.tasks

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * [RegisterGodotExtensionTask] makes Godot load the extension at startup on the first editor run, which avoids the
 * Godot 4.7.2 crash of a hot-loaded extension that registers classes (#218).
 */
class RegisterGodotExtensionTaskTest {
    @TempDir
    lateinit var projectDir: File

    private lateinit var task: RegisterGodotExtensionTask
    private lateinit var godotDir: File
    private val list get() = File(godotDir, ".godot/extension_list.cfg")

    @BeforeEach
    fun setUp() {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        godotDir = File(projectDir, "godot-project").apply { mkdirs() }
        task = project.tasks.register("registerTest", RegisterGodotExtensionTask::class.java).get()
        task.godotProjectDir.set(godotDir)
        task.gdextensionResPath.set("res://game.gdextension")
    }

    @Test
    fun `a fresh project gets the file with the extension`() {
        task.register()

        assertEquals("res://game.gdextension\n", list.readText())
    }

    @Test
    fun `running again does not duplicate the entry`() {
        task.register()
        task.register()

        assertEquals("res://game.gdextension\n", list.readText())
    }

    @Test
    fun `entries of other extensions are kept`() {
        list.parentFile.mkdirs()
        list.writeText("res://addons/other/other.gdextension\n")

        task.register()

        assertEquals("res://addons/other/other.gdextension\nres://game.gdextension\n", list.readText())
    }
}
