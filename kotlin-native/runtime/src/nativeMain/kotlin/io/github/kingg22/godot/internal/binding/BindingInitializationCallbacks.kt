package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.internal.ffi.GDExtensionInitializationLevel
import kotlinx.cinterop.COpaquePointer
import org.jetbrains.annotations.ApiStatus

/**
 * Low-level callbacks for initialization and deinitialization of the extension.
 *
 * **This class is not intended to be used directly**.
 *
 * The processor or plugin is going to generate this class for your project overriding some methods.
 */
@InternalBinding
@ApiStatus.OverrideOnly
public open class BindingInitializationCallbacks {
    public open fun onInitCore() {}
    public open fun onInitServers() {}
    public open fun onInitScene() {}
    public open fun onInitEditor() {}

    public open fun onDeInitCore() {}
    public open fun onDeInitServers() {}
    public open fun onDeInitScene() {}
    public open fun onDeInitEditor() {}

    /**
     * Catch-all deinitialization hook, invoked for every level *after* the matching per-level
     * `onDeInit*` hook. Kept for overrides that need the raw [level]; prefer the per-level hooks.
     */
    public open fun onDeInit(level: GDExtensionInitializationLevel) {}

    public fun initialize(userdata: COpaquePointer?, level: GDExtensionInitializationLevel) {
        when (level) {
            GDEXTENSION_INITIALIZATION_CORE -> onInitCore()
            GDEXTENSION_INITIALIZATION_SERVERS -> onInitServers()
            GDEXTENSION_INITIALIZATION_SCENE -> onInitScene()
            GDEXTENSION_INITIALIZATION_EDITOR -> onInitEditor()
            else -> println("Unexpected $level, userdata: $userdata")
            // False positive for when exhaustive but in nativeMain with more than 1 platform https://youtrack.jetbrains.com/issue/KT-77521
        }
    }

    public fun deinitialize(userdata: COpaquePointer?, level: GDExtensionInitializationLevel) {
        val _ = userdata
        when (level) {
            GDEXTENSION_INITIALIZATION_CORE -> onDeInitCore()
            GDEXTENSION_INITIALIZATION_SERVERS -> onDeInitServers()
            GDEXTENSION_INITIALIZATION_SCENE -> onDeInitScene()
            GDEXTENSION_INITIALIZATION_EDITOR -> onDeInitEditor()
            else -> {}
            // False positive for when exhaustive but in nativeMain with more than 1 platform https://youtrack.jetbrains.com/issue/KT-77521
        }
        onDeInit(level)
    }
}
