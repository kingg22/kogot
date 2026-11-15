package io.github.kingg22.godot.internal.script

import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.builtin.GodotString
import io.github.kingg22.godot.api.builtin.PackedStringArray
import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.toGodotString
import io.github.kingg22.godot.api.builtin.toVariant
import io.github.kingg22.godot.api.core.refcounted.ResourceFormatLoader
import io.github.kingg22.godot.api.singleton.ProjectSettings
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.createInstanceFromKotlin
import io.github.kingg22.godot.internal.script.KotlinScriptRegistry.KOTLIN_SCRIPT_EXTENSION
import kotlinx.cinterop.COpaquePointer

/**
 * Resolves a `.kt` script resource path to a [KotlinScript] wrapping the already-compiled `@Godot`
 * class recorded for that path in [KotlinScriptRegistry] (issue #42).
 *
 * [KotlinScriptRegistry] is keyed by KSP's `filePath`: an absolute filesystem path, since that's all
 * the compile-time processor knows. Godot instead calls `_load`/`_getResourceTypeAsGdStr` with a
 * `res://`-relative virtual path. [ProjectSettings.globalizePath] converts the latter to the former
 * (it is a no-op for paths that are already absolute), so every registry lookup below normalizes
 * through it first — otherwise every `.kt` resource would report as not found.
 */
@InternalBinding
public class KotlinResourceFormatLoader(nativePtr: COpaquePointer) : ResourceFormatLoader(nativePtr) {
    override fun _getRecognizedExtensions(): PackedStringArray {
        val array = PackedStringArray()
        val _ = array.pushBack(KOTLIN_SCRIPT_EXTENSION.toGodotString())
        return array
    }

    // Matches ResourceFormatLoaderGDScript::handles_type (modules/gdscript/gdscript_resource_format.cpp):
    // accepts both the generic "Script" hint and the concrete registered ClassDB type name.
    override fun _handlesType(type: StringName): Boolean = type.toString() in HANDLED_TYPES

    // Must be the concrete registered ClassDB type ("KotlinScript"), not the abstract "Script" base —
    // GDScript's own loader returns "GDScript" here, never "Script", for the same reason: Godot's editor
    // tooling (e.g. EditorData::get_script_icon, editor/editor_data.cpp) uses this to decide what to
    // instantiate in some of its own internal fallback/caching paths, and "Script" is abstract (pure
    // virtual methods) — instantiating it directly produces an object with no usable vtable. Confirmed
    // via a self-built Godot dev binary + lldb: returning "Script" here crashed with EXC_BAD_ACCESS
    // inside Godot's own get_class_icon_path() call, entirely outside this extension's code.
    override fun _getResourceTypeAsGdStr(path: GodotString): GodotString =
        if (KotlinScriptRegistry.contains(globalizedPath(path.toKString()))) KOTLIN_SCRIPT_CLASS_NAME.toGodotString() else GodotString()

    override fun _load(path: GodotString, originalPath: GodotString, useSubThreads: Boolean, cacheMode: Int): Variant {
        val normalizedPath = globalizedPath(path.toKString())
        val entry = KotlinScriptRegistry[normalizedPath] ?: return GodotError.FILE_NOT_FOUND.value.toVariant()

        // The canonical instance (the one every virtual call `_can_instantiate`, `_is_valid`, ... dispatches to)
        // carries `entry`'s values; returning a second `KotlinScript(ptr)` wrapper, as an earlier version did, split
        // the identity and left the creation reference behind, so every load grew the refcount by one and the
        // script leaked at exit (issue #213). The script owns its own reference; the returned Variant adds the
        // engine's. Can run on a loader worker thread: everything here is thread-safe (see docs/threading-model.md).
        val script = createInstanceFromKotlin("ScriptExtension", "KotlinScript") { ptr ->
            KotlinScript(ptr, normalizedPath, entry.className, entry.baseClassName)
        } ?: return GodotError.CANT_CREATE.value.toVariant()

        return script.toVariant()
    }

    private fun globalizedPath(path: String): String = ProjectSettings.instance.globalizePath(path)
}

private const val KOTLIN_SCRIPT_CLASS_NAME = "KotlinScript"
private val HANDLED_TYPES = setOf("Script", KOTLIN_SCRIPT_CLASS_NAME)
