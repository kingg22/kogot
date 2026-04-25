package io.github.kingg22.godot.codegen.extensionapi.resolver

import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.models.extensionapi.EngineClass

/**
 * Initialization order for Godot (see [Godot main](https://github.com/godotengine/godot/blob/master/main/main.cpp)).
 * - Main::setup()
 * - register_core_types()
 * - register_early_core_singletons()
 * - initialize_extensions(GDExtension::INITIALIZATION_LEVEL_CORE)
 * - Main::setup2()
 * - register_server_types()
 * - initialize_extensions(GDExtension::INITIALIZATION_LEVEL_SERVERS)
 * - register_core_singletons() ...possibly a bug. Should this be before LEVEL_SERVERS?
 * - register_scene_types()
 * - register_scene_singletons()
 * - initialize_extensions(GDExtension::INITIALIZATION_LEVEL_SCENE)
 * - IF EDITOR
 * - register_editor_types()
 * - initialize_extensions(GDExtension::INITIALIZATION_LEVEL_EDITOR)
 * - register_server_singletons() ...another weird one.
 * - Autoloads, etc.
 *
 * ## Singleton availability by initialization level
 * - **Core level**: Basic singletons like `Engine`, `OS`, `ProjectSettings`, `Time` are available.
 * - **Servers level**: Server singletons like `RenderingServer` are NOT yet available due to GDExtension timing issues.
 * - **Scene level**: All singletons including `RenderingServer` are available.
 * - **Editor level**: Editor-specific functionality is available.
 *
 * GDExtension singletons are generally not available during *any* level initialization, except a few core singletons
 * (see above). This is different from how modules work, where servers are available at _Servers_ level.
 *
 * See also:
 * - [Singletons not accessible in Scene (godot-cpp)](https://github.com/godotengine/godot-cpp/issues/1180)
 * - [`global_get_singleton` not returning singletons](https://github.com/godotengine/godot/issues/64975)
 * - [PR to make singletons available](https://github.com/godotengine/godot/pull/98862)
 */
enum class ClassCodegenLevel {
    Core,
    Servers,
    Scene,
    Editor,
    ;

    companion object {
        context(context: Context)
        fun classifyCodegenLevel(className: String): ClassCodegenLevel? = when (className) {
            // See register_core_types() in https://github.com/godotengine/godot/blob/master/core/register_core_types.cpp,
            // which is called before Core level is initialized. Only a small list is promoted to Core; carefully evaluate if more are added.
            "Object", "RefCounted", "Resource", "MainLoop", "GDExtension" -> Core

            // See register_early_core_singletons() in https://github.com/godotengine/godot/blob/master/core/register_core_types.cpp,
            // which is called before Core level is initialized.
            // ClassDB is available, however its *singleton* will be registered at Core level only from Godot 4.7 on, see
            // https://github.com/godot-rust/gdext/pull/1474. Its function pointers can already be fetched in Core before; there's just no instance.
            "ProjectSettings", "Engine", "OS", "Time", "ClassDB" -> Core

            // See initialize_openxr_module() in https://github.com/godotengine/godot/blob/master/modules/openxr/register_types.cpp
            "OpenXRExtensionWrapper" -> Core

            // Symbols from another extension could be available in Core, but since GDExtension can currently not guarantee
            // the order of different extensions being loaded, we prevent implicit dependencies and require Server.
            "OpenXRExtensionWrapperExtension" -> Servers

            // See register_server_types() in https://github.com/godotengine/godot/blob/master/servers/register_server_types.cpp
            "PhysicsDirectBodyState2D", "PhysicsDirectBodyState2DExtension",
            "PhysicsDirectSpaceState2D", "PhysicsDirectSpaceState2DExtension",
            "PhysicsServer2D", "PhysicsServer2DExtension",
            "PhysicsServer2DManager",
            "PhysicsDirectBodyState3D", "PhysicsDirectBodyState3DExtension",
            "PhysicsDirectSpaceState3D", "PhysicsDirectSpaceState3DExtension",
            "PhysicsServer3D", "PhysicsServer3DExtension",
            "PhysicsServer3DManager",
            "PhysicsServer3DRenderingServerHandler",
            "RenderData", "RenderDataExtension",
            "RenderSceneData", "RenderSceneDataExtension",
            -> Servers

            // Declared final (un-inheritable) in Rust, but those are still servers.
            "AudioServer", "CameraServer", "NavigationServer2D", "NavigationServer3D",
            "RenderingServer", "TranslationServer", "XRServer", "DisplayServer",
            -> Servers

            // Work around wrong classification in https://github.com/godotengine/godot/issues/86206.
            // https://github.com/godotengine/godot/issues/103867
            "OpenXRInteractionProfileEditorBase", "OpenXRInteractionProfileEditor", "OpenXRBindingModifierEditor" -> {
                if (context.godotVersion.isBefore(4, 5)) Editor else null
            }

            // https://github.com/godotengine/godot/issues/86206
            "ResourceImporterOggVorbis", "ResourceImporterMP3" -> {
                if (context.godotVersion.isBefore(4, 3)) Editor else null
            }

            else -> null
        }

        /**
         * Logic for determining the API level of a given class.
         */
        context(_: Context)
        fun getApiLevel(engineClass: EngineClass): ClassCodegenLevel {
            // NOTE: We have to use a whitelist of known classes because Godot doesn't separate these out
            // beyond "editor" and "core" and some classes are also mis-classified in the JSON depending on the Godot version.
            val forcedClassification = classifyCodegenLevel(engineClass.name)
            if (forcedClassification != null) {
                return forcedClassification
            }

            // NOTE: Right now, Godot reports everything that's not "editor" as "core" in `extension_api.json`.
            // If it wasn't picked up by classify_codegen_level, and Godot reports it as "core" we will treat it as a scene class.
            return when (engineClass.apiType) {
                "editor" -> Editor

                "core" -> Scene

                "extension" -> Scene

                "editor_extension" -> Editor

                else -> {
                    // we don't know this classification
                    throw IllegalStateException("class ${engineClass.name} has unknown API type ${engineClass.apiType}")
                }
            }
        }
    }
}
