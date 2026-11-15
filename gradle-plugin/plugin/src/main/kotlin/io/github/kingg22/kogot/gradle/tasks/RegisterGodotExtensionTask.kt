package io.github.kingg22.kogot.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.File

/**
 * Lists the extension's `.gdextension` in the Godot project's `.godot/extension_list.cfg` (one `res://` path per line),
 * so Godot loads it at startup from the very first editor run.
 *
 * Without that file (a fresh clone, a deleted `.godot`) the editor only discovers the extension during its first
 * filesystem scan and loads it mid-session. Godot 4.7.2 then crashes at editor exit, before deinitializing extensions,
 * as soon as such a hot-loaded extension registers any class: reproduced with a 60-line C extension, not specific to
 * kogot (issue #218). Loaded at startup, it exits cleanly.
 *
 * Idempotent: an entry already present (or other extensions' entries) is left as it is. Untracked because it edits a
 * file Godot also writes.
 */
@UntrackedTask(because = "Edits .godot/extension_list.cfg, a file the Godot editor also maintains")
abstract class RegisterGodotExtensionTask : DefaultTask() {
    init {
        description = "Lists the GDExtension in .godot/extension_list.cfg so Godot loads it at startup"
        group = "kogot"
    }

    @get:Internal
    abstract val godotProjectDir: DirectoryProperty

    /** The `res://` path of the `.gdextension` file, e.g. `res://my_game.gdextension`. */
    @get:Input
    abstract val gdextensionResPath: Property<String>

    @TaskAction
    fun register() {
        val list = File(godotProjectDir.get().asFile, ".godot/extension_list.cfg")
        val entry = gdextensionResPath.get()
        val existing = if (list.isFile) list.readLines().map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
        if (entry in existing) return
        list.parentFile.mkdirs()
        list.writeText((existing + entry).joinToString(separator = "\n", postfix = "\n"))
    }
}
