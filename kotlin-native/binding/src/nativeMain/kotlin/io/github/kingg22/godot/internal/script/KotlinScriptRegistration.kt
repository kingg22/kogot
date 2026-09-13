package io.github.kingg22.godot.internal.script

import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.core.ScriptLanguageExtensionVirtualCalls
import io.github.kingg22.godot.api.core.refcounted.ResourceFormatLoaderVirtualCalls
import io.github.kingg22.godot.api.core.refcounted.ResourceFormatSaverVirtualCalls
import io.github.kingg22.godot.api.core.refcounted.ScriptExtensionVirtualCalls
import io.github.kingg22.godot.api.internal.checkGodotError
import io.github.kingg22.godot.api.singleton.Engine
import io.github.kingg22.godot.api.singleton.ResourceLoader
import io.github.kingg22.godot.api.singleton.ResourceSaver
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.pushError
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.ObjectBinding
import io.github.kingg22.godot.internal.binding.createInstanceFunc
import io.github.kingg22.godot.internal.binding.registerClass
import io.github.kingg22.godot.internal.binding.resolveVirtualCall
import io.github.kingg22.godot.internal.ffi.*
import kotlinx.cinterop.staticCFunction

/**
 * Registers Kotlin as a Godot scripting language: [KotlinScriptLanguage], [KotlinScript],
 * [KotlinResourceFormatLoader] and [KotlinResourceFormatSaver] as ClassDB extension classes, then
 * instantiates and hands one singleton of each to the engine (issue #42).
 *
 * Called once from the KSP-generated `onInitScene()` of every project that applies the Kogot processor
 * (see `GodotBindingGenerator.generateCallbacksFile`), alongside feeding that project's own
 * `ScriptFileRegistry` into [KotlinScriptRegistry].
 */
@InternalBinding
public object KotlinScriptRegistration {
    /** The single registered [KotlinScriptLanguage] instance, returned by [KotlinScript._getLanguage]. */
    public lateinit var language: KotlinScriptLanguage
        private set

    /** The single [KotlinResourceFormatLoader] handed to [ResourceLoader]; retained so it can be removed on deinit. */
    public lateinit var loader: KotlinResourceFormatLoader
        private set

    /** The single [KotlinResourceFormatSaver] handed to [ResourceSaver]; retained so it can be removed on deinit. */
    public lateinit var saver: KotlinResourceFormatSaver
        private set

    private var registered = false

    public fun registerKotlinScriptLanguageSupport() {
        if (registered) return
        registered = true

        registerClass<KotlinScriptLanguage>(
            "KotlinScriptLanguage",
            "ScriptLanguageExtension",
            staticCFunction { _, notifyPostInitialize ->
                createInstanceFunc(
                    "ScriptLanguageExtension",
                    "KotlinScriptLanguage",
                    notifyPostInitialize == GDExtensionBool.TRUE,
                    ::KotlinScriptLanguage,
                )
            },
            staticCFunction { _, funcNamePtr, _ ->
                resolveVirtualCall(funcNamePtr) { funcName ->
                    when (funcName) {
                        "_get_name" as Any -> ScriptLanguageExtensionVirtualCalls.getName

                        "_init" as Any -> ScriptLanguageExtensionVirtualCalls.init

                        "_get_type" as Any -> ScriptLanguageExtensionVirtualCalls.getType

                        "_get_extension" as Any -> ScriptLanguageExtensionVirtualCalls.getExtension

                        "_finish" as Any -> ScriptLanguageExtensionVirtualCalls.finish

                        "_get_reserved_words" as Any -> ScriptLanguageExtensionVirtualCalls.getReservedWords

                        "_get_comment_delimiters" as Any -> ScriptLanguageExtensionVirtualCalls.getCommentDelimiters

                        "_get_string_delimiters" as Any -> ScriptLanguageExtensionVirtualCalls.getStringDelimiters

                        "_has_named_classes" as Any -> ScriptLanguageExtensionVirtualCalls.hasNamedClasses

                        "_validate" as Any -> ScriptLanguageExtensionVirtualCalls.validate

                        "_get_recognized_extensions" as Any ->
                            ScriptLanguageExtensionVirtualCalls.getRecognizedExtensions

                        "_supports_builtin_mode" as Any -> ScriptLanguageExtensionVirtualCalls.supportsBuiltinMode

                        "_create_script" as Any -> ScriptLanguageExtensionVirtualCalls.createScript

                        "_is_control_flow_keyword" as Any -> ScriptLanguageExtensionVirtualCalls.isControlFlowKeyword

                        "_make_template" as Any -> ScriptLanguageExtensionVirtualCalls.makeTemplate

                        "_is_using_templates" as Any -> ScriptLanguageExtensionVirtualCalls.isUsingTemplates

                        "_validate_path" as Any -> ScriptLanguageExtensionVirtualCalls.validatePath

                        "_supports_documentation" as Any -> ScriptLanguageExtensionVirtualCalls.supportsDocumentation

                        "_can_inherit_from_file" as Any -> ScriptLanguageExtensionVirtualCalls.canInheritFromFile

                        "_find_function" as Any -> ScriptLanguageExtensionVirtualCalls.findFunction

                        "_make_function" as Any -> ScriptLanguageExtensionVirtualCalls.makeFunction

                        "_can_make_function" as Any -> ScriptLanguageExtensionVirtualCalls.canMakeFunction

                        "_open_in_external_editor" as Any -> ScriptLanguageExtensionVirtualCalls.openInExternalEditor

                        "_overrides_external_editor" as Any ->
                            ScriptLanguageExtensionVirtualCalls.overridesExternalEditor

                        // Unblocked by issue #141 / PR #142 (virtual-dispatch engine-class arguments are
                        // now null-safe regardless of extension_api.json default-value metadata) — see
                        // KotlinScriptLanguage._completeCode/_lookupCode for the crash this previously
                        // caused (owner: GodotObject is null whenever the script being edited isn't
                        // attached to any live scene object, the ordinary FileSystem-dock-editing case).
                        "_complete_code" as Any -> ScriptLanguageExtensionVirtualCalls.completeCode

                        "_lookup_code" as Any -> ScriptLanguageExtensionVirtualCalls.lookupCode

                        "_auto_indent_code" as Any -> ScriptLanguageExtensionVirtualCalls.autoIndentCode

                        "_add_global_constant" as Any -> ScriptLanguageExtensionVirtualCalls.addGlobalConstant

                        "_add_named_global_constant" as Any ->
                            ScriptLanguageExtensionVirtualCalls.addNamedGlobalConstant

                        "_remove_named_global_constant" as Any ->
                            ScriptLanguageExtensionVirtualCalls.removeNamedGlobalConstant

                        "_thread_enter" as Any -> ScriptLanguageExtensionVirtualCalls.threadEnter

                        "_thread_exit" as Any -> ScriptLanguageExtensionVirtualCalls.threadExit

                        "_debug_get_error" as Any -> ScriptLanguageExtensionVirtualCalls.debugGetError

                        "_debug_get_stack_level_count" as Any ->
                            ScriptLanguageExtensionVirtualCalls.debugGetStackLevelCount

                        "_debug_get_stack_level_line" as Any ->
                            ScriptLanguageExtensionVirtualCalls.debugGetStackLevelLine

                        "_debug_get_stack_level_function" as Any ->
                            ScriptLanguageExtensionVirtualCalls.debugGetStackLevelFunction

                        "_debug_get_stack_level_source" as Any ->
                            ScriptLanguageExtensionVirtualCalls.debugGetStackLevelSource

                        "_debug_get_stack_level_locals" as Any ->
                            ScriptLanguageExtensionVirtualCalls.debugGetStackLevelLocals

                        "_debug_get_stack_level_members" as Any ->
                            ScriptLanguageExtensionVirtualCalls.debugGetStackLevelMembers

                        "_debug_get_stack_level_instance" as Any ->
                            ScriptLanguageExtensionVirtualCalls.debugGetStackLevelInstance

                        "_debug_get_globals" as Any -> ScriptLanguageExtensionVirtualCalls.debugGetGlobals

                        "_debug_parse_stack_level_expression" as Any ->
                            ScriptLanguageExtensionVirtualCalls.debugParseStackLevelExpression

                        "_reload_all_scripts" as Any -> ScriptLanguageExtensionVirtualCalls.reloadAllScripts

                        "_reload_scripts" as Any -> ScriptLanguageExtensionVirtualCalls.reloadScripts

                        "_reload_tool_script" as Any -> ScriptLanguageExtensionVirtualCalls.reloadToolScript

                        "_get_public_constants" as Any -> ScriptLanguageExtensionVirtualCalls.getPublicConstants

                        "_profiling_start" as Any -> ScriptLanguageExtensionVirtualCalls.profilingStart

                        "_profiling_stop" as Any -> ScriptLanguageExtensionVirtualCalls.profilingStop

                        "_profiling_set_save_native_calls" as Any ->
                            ScriptLanguageExtensionVirtualCalls.profilingSetSaveNativeCalls

                        "_frame" as Any -> ScriptLanguageExtensionVirtualCalls.frame

                        "_handles_global_class_type" as Any ->
                            ScriptLanguageExtensionVirtualCalls.handlesGlobalClassType

                        "_get_global_class_name" as Any -> ScriptLanguageExtensionVirtualCalls.getGlobalClassName

                        "_get_built_in_templates" as Any -> ScriptLanguageExtensionVirtualCalls.getBuiltInTemplates

                        "_debug_get_current_stack_info" as Any ->
                            ScriptLanguageExtensionVirtualCalls.debugGetCurrentStackInfo

                        "_get_public_functions" as Any -> ScriptLanguageExtensionVirtualCalls.getPublicFunctions

                        "_get_public_annotations" as Any -> ScriptLanguageExtensionVirtualCalls.getPublicAnnotations

                        else -> null
                    }
                }
            },
        )

        registerClass<KotlinScript>(
            "KotlinScript",
            "ScriptExtension",
            staticCFunction { _, notifyPostInitialize ->
                createInstanceFunc(
                    "ScriptExtension",
                    "KotlinScript",
                    notifyPostInitialize == GDExtensionBool.TRUE,
                    ::KotlinScript,
                )
            },
            staticCFunction { _, funcNamePtr, _ ->
                resolveVirtualCall(funcNamePtr) { funcName ->
                    when (funcName) {
                        "_can_instantiate" as Any -> ScriptExtensionVirtualCalls.canInstantiate
                        "_get_source_code" as Any -> ScriptExtensionVirtualCalls.getSourceCode
                        "_set_source_code" as Any -> ScriptExtensionVirtualCalls.setSourceCode
                        "_has_source_code" as Any -> ScriptExtensionVirtualCalls.hasSourceCode
                        "_is_valid" as Any -> ScriptExtensionVirtualCalls.isValid
                        "_is_tool" as Any -> ScriptExtensionVirtualCalls.isTool
                        "_get_instance_base_type" as Any -> ScriptExtensionVirtualCalls.getInstanceBaseType
                        "_get_language" as Any -> ScriptExtensionVirtualCalls.getLanguage
                        "_reload" as Any -> ScriptExtensionVirtualCalls.reload
                        "_instance_create" as Any -> ScriptExtensionVirtualCalls.instanceCreate
                        "_editor_can_reload_from_file" as Any -> ScriptExtensionVirtualCalls.editorCanReloadFromFile
                        "_get_base_script" as Any -> ScriptExtensionVirtualCalls.getBaseScript
                        "_get_global_name" as Any -> ScriptExtensionVirtualCalls.getGlobalName
                        "_inherits_script" as Any -> ScriptExtensionVirtualCalls.inheritsScript
                        "_placeholder_instance_create" as Any -> ScriptExtensionVirtualCalls.placeholderInstanceCreate
                        "_get_doc_class_name" as Any -> ScriptExtensionVirtualCalls.getDocClassName
                        "_has_method" as Any -> ScriptExtensionVirtualCalls.hasMethod
                        "_has_static_method" as Any -> ScriptExtensionVirtualCalls.hasStaticMethod
                        "_get_method_info" as Any -> ScriptExtensionVirtualCalls.getMethodInfo
                        "_has_script_signal" as Any -> ScriptExtensionVirtualCalls.hasScriptSignal
                        "_has_property_default_value" as Any -> ScriptExtensionVirtualCalls.hasPropertyDefaultValue
                        "_get_property_default_value" as Any -> ScriptExtensionVirtualCalls.getPropertyDefaultValue
                        "_update_exports" as Any -> ScriptExtensionVirtualCalls.updateExports
                        "_get_member_line" as Any -> ScriptExtensionVirtualCalls.getMemberLine
                        "_get_constants" as Any -> ScriptExtensionVirtualCalls.getConstants
                        "_is_placeholder_fallback_enabled" as Any ->
                            ScriptExtensionVirtualCalls.isPlaceholderFallbackEnabled
                        "_get_rpc_config" as Any -> ScriptExtensionVirtualCalls.getRpcConfig
                        "_get_documentation" as Any -> ScriptExtensionVirtualCalls.getDocumentation
                        "_get_script_signal_list" as Any -> ScriptExtensionVirtualCalls.getScriptSignalList
                        "_get_script_method_list" as Any -> ScriptExtensionVirtualCalls.getScriptMethodList
                        "_get_script_property_list" as Any -> ScriptExtensionVirtualCalls.getScriptPropertyList
                        "_get_members" as Any -> ScriptExtensionVirtualCalls.getMembers
                        else -> null
                    }
                }
            },
        )

        registerClass<KotlinResourceFormatLoader>(
            "KotlinResourceFormatLoader",
            "ResourceFormatLoader",
            staticCFunction { _, notifyPostInitialize ->
                createInstanceFunc(
                    "ResourceFormatLoader",
                    "KotlinResourceFormatLoader",
                    notifyPostInitialize == GDExtensionBool.TRUE,
                    ::KotlinResourceFormatLoader,
                )
            },
            staticCFunction { _, funcNamePtr, _ ->
                resolveVirtualCall(funcNamePtr) { funcName ->
                    when (funcName) {
                        "_get_recognized_extensions" as Any -> ResourceFormatLoaderVirtualCalls.getRecognizedExtensions
                        "_handles_type" as Any -> ResourceFormatLoaderVirtualCalls.handlesType
                        "_get_resource_type" as Any -> ResourceFormatLoaderVirtualCalls.getResourceType
                        "_load" as Any -> ResourceFormatLoaderVirtualCalls.load
                        else -> null
                    }
                }
            },
        )

        registerClass<KotlinResourceFormatSaver>(
            "KotlinResourceFormatSaver",
            "ResourceFormatSaver",
            staticCFunction { _, notifyPostInitialize ->
                createInstanceFunc(
                    "ResourceFormatSaver",
                    "KotlinResourceFormatSaver",
                    notifyPostInitialize == GDExtensionBool.TRUE,
                    ::KotlinResourceFormatSaver,
                )
            },
            staticCFunction { _, funcNamePtr, _ ->
                resolveVirtualCall(funcNamePtr) { funcName ->
                    when (funcName) {
                        "_get_recognized_extensions" as Any -> ResourceFormatSaverVirtualCalls.getRecognizedExtensions
                        "_recognize" as Any -> ResourceFormatSaverVirtualCalls.recognize
                        "_save" as Any -> ResourceFormatSaverVirtualCalls.save
                        else -> null
                    }
                }
            },
        )

        val languagePtr = createInstanceFunc(
            "ScriptLanguageExtension",
            "KotlinScriptLanguage",
            false,
            ::KotlinScriptLanguage,
        ) ?: error("Failed to create the KotlinScriptLanguage singleton")
        language = KotlinScriptLanguage(languagePtr)
        checkGodotError(
            "Kotlin Script Language registration to engine",
            Engine.instance.registerScriptLanguage(language),
        )

        val loaderPtr = createInstanceFunc(
            "ResourceFormatLoader",
            "KotlinResourceFormatLoader",
            false,
            ::KotlinResourceFormatLoader,
        ) ?: error("Failed to create the KotlinResourceFormatLoader singleton")
        loader = KotlinResourceFormatLoader(loaderPtr)
        ResourceLoader.instance.addResourceFormatLoader(loader)

        val saverPtr = createInstanceFunc(
            "ResourceFormatSaver",
            "KotlinResourceFormatSaver",
            false,
            ::KotlinResourceFormatSaver,
        ) ?: error("Failed to create the KotlinResourceFormatSaver singleton")
        saver = KotlinResourceFormatSaver(saverPtr)
        ResourceSaver.instance.addResourceFormatSaver(saver)
    }

    /**
     * Reverses [registerKotlinScriptLanguageSupport]: unregisters the script language, resource-format
     * loader and saver from the engine and frees the singletons that were handed to it (issue #42).
     *
     * Godot has no GC — an [Object] created by the extension and given to a still-live engine singleton
     * outlives `ObjectDB::cleanup()` and is reported as a leaked instance at exit unless it is torn down
     * here. Called from the KSP-generated `onDeInitScene()`, the reverse-order counterpart of the
     * `onInitScene()` that calls [registerKotlinScriptLanguageSupport].
     *
     * Runs during shutdown, so it never throws: a non-OK unregister result is logged, not raised.
     */
    public fun unregisterKotlinScriptLanguageSupport() {
        if (!registered) return
        registered = false

        if (::language.isInitialized) {
            val error = Engine.instance.unregisterScriptLanguage(language)
            if (error != GodotError.OK) {
                GD.pushError("Kotlin Script Language unregistration from engine failed: $error")
            }
            // ScriptLanguageExtension is a plain Object (not RefCounted): the engine stored a raw
            // pointer and won't delete it, so destroy it ourselves. This calls back into
            // freeInstanceFunc, which disposes the StableRef.
            ObjectBinding.destroyRaw(language.rawPtr)
        }

        // KotlinResourceFormatLoader / -Saver are RefCounted. kogot never runs reference counting on
        // its wrappers, so the reference taken at construction is still outstanding after the engine
        // drops its own: remove from the engine's Ref<> array first, then release that construction
        // reference and free if it was the last one (the Ref<T> destructor pattern). Both paths call
        // back into freeInstanceFunc, which disposes the StableRef.
        if (::loader.isInitialized) {
            ResourceLoader.instance.removeResourceFormatLoader(loader)
            if (loader.unreference()) ObjectBinding.destroyRaw(loader.rawPtr)
        }
        if (::saver.isInitialized) {
            ResourceSaver.instance.removeResourceFormatSaver(saver)
            if (saver.unreference()) ObjectBinding.destroyRaw(saver.rawPtr)
        }

        KotlinScriptRegistry.clear()
    }
}
