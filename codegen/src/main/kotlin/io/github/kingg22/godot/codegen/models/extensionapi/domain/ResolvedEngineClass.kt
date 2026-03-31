package io.github.kingg22.godot.codegen.models.extensionapi.domain

import io.github.kingg22.godot.codegen.models.extensionapi.EngineClass

data class ResolvedEngineClass(
    val raw: EngineClass,
    val isSingleton: Boolean,
    val isSingletonExtensible: Boolean,
    val enums: List<ResolvedEnum>,
    // Colecciones que incluyen miembros heredados
    val allMethods: List<EngineClass.ClassMethod>,
    val allProperties: List<EngineClass.ClassProperty>,
    val allSignals: List<EngineClass.Signal>,
) {
    val name get() = raw.name
    val shortName get() = raw.name.substringAfterLast('.')
    val apiType get() = raw.apiType
    val isRefcounted get() = raw.isRefcounted
    val inherits get() = raw.inherits
    val methods get() = raw.methods
    val properties get() = raw.properties
    val signals get() = raw.signals

    /**
     * Determina si la clase se puede instanciar.
     * Sobrescribe el valor de Godot si es necesario (ej. Tweens).
     */
    val isInstantiable: Boolean get() = isClassInstantiable(name) ?: raw.isInstantiable

    /**
     * Determina si la clase es abstracta según las reglas de Godot.
     */
    val isAbstract: Boolean get() = isClassAbstract(name)
}

/**
 * Whether a class can be instantiated (overrides Godot's defaults in some cases).
 *
 * Returns `null` if the Godot default should be taken.
 */
private fun isClassInstantiable(className: String): Boolean? {
    // The default constructor is available but callers meet with the following Godot error:
    // "ERROR: XY can't be created directly. Use create_tween() method."
    // for the following classes XY:
    // Tween, PropertyTweener, PropertyTweener, IntervalTweener, CallbackTweener, MethodTweener, SubtweenTweener,

    if (className == "Tween" || className.endsWith("Tweener")) {
        return false
    }

    return null
}

/**
 * Whether a class is "Godot abstract".
 *
 * Abstract in Godot is different from the usual term in OOP. It means:
 * 1. The class has no default constructor. However, it can still be instantiated through other means; e.g. FileAccess::open().
 * 2. The class can not be inherited from *outside the engine*. It's possible for other engine classes to inherit from it, but not extension ones.
 */
private fun isClassAbstract(className: String): Boolean {
    // Get this list by running following command in Godot repo:
    // rg GDREGISTER_ABSTRACT_CLASS | rg -v '#define' | sd '.+\((\w+)\).+' '| "$1"' | sort | uniq > abstract.txt

    // Note: singletons are currently not declared abstract in Godot, but they are separately considered for the "final" property.
    return abstractClasses.contains(className)
}

private val abstractClasses = hashSetOf(
    "AnimationMixer",
    "AudioEffectSpectrumAnalyzerInstance",
    "AudioStreamGeneratorPlayback",
    "AudioStreamPlaybackInteractive",
    "AudioStreamPlaybackPlaylist",
    "AudioStreamPlaybackPolyphonic",
    "AudioStreamPlaybackSynchronized",
    "BaseMaterial3D",
    "CanvasItem",
    "CollisionObject2D",
    "CollisionObject3D",
    "CompressedTextureLayered",
    "CSGPrimitive3D",
    "CSGShape3D",
    "DirAccess",
    "DisplayServer",
    "EditorDebuggerSession",
    "EditorExportPlatform",
    "EditorExportPlatformAppleEmbedded",
    "EditorExportPlatformPC",
    "EditorExportPreset",
    "EditorFileSystem",
    "EditorInterface",
    "EditorResourcePreview",
    "EditorToaster",
    "EditorUndoRedoManager",
    "ENetPacketPeer",
    "FileAccess",
    "FileSystemDock",
    "Font",
    "GDExtensionManager",
    "GPUParticlesAttractor3D",
    "GPUParticlesCollision3D",
    "ImageFormatLoader",
    "ImageTextureLayered",
    "Input",
    "InputEvent",
    "InputEventFromWindow",
    "InputEventGesture",
    "InputEventMouse",
    "InputEventWithModifiers",
    "InstancePlaceholder",
    "IP",
    "JavaScriptBridge",
    "JavaScriptObject",
    "Joint2D",
    "Joint3D",
    "Light2D",
    "Light3D",
    "Lightmapper",
    "MultiplayerAPI",
    "MultiplayerPeer",
    "NavigationServer2D",
    "NavigationServer3D",
    "Node3DGizmo",
    "Noise",
    "Occluder3D",
    "OpenXRBindingModifier",
    "OpenXRCompositionLayer",
    "OpenXRFutureResult",
    "OpenXRHapticBase",
    "OpenXRInteractionProfileEditorBase",
    "PackedDataContainerRef",
    "PacketPeer",
    "PhysicsBody2D",
    "PhysicsBody3D",
    "PhysicsDirectBodyState2D",
    "PhysicsDirectBodyState3D",
    "PhysicsDirectSpaceState2D",
    "PhysicsDirectSpaceState3D",
    "PhysicsServer2D",
    "PhysicsServer3D",
    "PlaceholderTextureLayered",
    "RenderData",
    "RenderingDevice",
    "RenderingServer",
    "RenderSceneBuffers",
    "RenderSceneData",
    "ResourceImporter",
    "ResourceUID",
    "SceneState",
    "SceneTreeTimer",
    "Script",
    "ScriptEditor",
    "ScriptEditorBase",
    "ScriptLanguage",
    "ScrollBar",
    "Separator",
    "Shader",
    "Shape2D",
    "Shape3D",
    "SkinReference",
    "Slider",
    "SpriteBase3D",
    "StreamPeer",
    "TextServer",
    "TextureLayeredRD",
    "TileSetSource",
    "TLSOptions",
    "TreeItem",
    "Tweener",
    "Viewport",
    "VisualShaderNode",
    "VisualShaderNodeConstant",
    "VisualShaderNodeGroupBase",
    "VisualShaderNodeOutput",
    "VisualShaderNodeParameter",
    "VisualShaderNodeParticleEmitter",
    "VisualShaderNodeResizableBase",
    "VisualShaderNodeSample3D",
    "VisualShaderNodeTextureParameter",
    "VisualShaderNodeVarying",
    "VisualShaderNodeVectorBase",
    "WebRTCDataChannel",
    "WebXRInterface",
    "WorkerThreadPool",
    "XRInterface",
    "XRTracker",
)
