@file:Suppress("ktlint:standard:filename")

package io.github.kingg22.godot.codegen.extensionapi.impl.experimental

import io.github.kingg22.godot.codegen.extensionapi.ExperimentalTypesRegistry
import io.github.kingg22.godot.codegen.extensionapi.ExperimentalTypesRegistry.GodotElementType

@JvmField
val ExperimentalTypes4_6_1 = ExperimentalTypesRegistry {
    // 1
    addMember(
        "AudioStreamPlayer2D",
        "playback_type",
        GodotElementType.PROPERTY,
        getterName = "get_playback_type",
        setterName = "set_playback_type",
    )

    // 2
    addClass(
        "CompositorEffect",
        "The implementation may change as more of the rendering internals are exposed over time.",
    )

    // 3
    addMember(
        "AudioStreamPlayer",
        "playback_type",
        GodotElementType.PROPERTY,
        getterName = "get_playback_type",
        setterName = "set_playback_type",
    )

    // 4
    addMember(
        "AudioStreamPlayer3D",
        "playback_type",
        GodotElementType.PROPERTY,
        getterName = "get_playback_type",
        setterName = "set_playback_type",
    )

    // 5
    addClass(
        "SkeletonModification2DPhysicalBones",
        "Physical bones may be changed in the future to perform the position update of [Bone2D] on their own, without needing this resource.",
    )

    // 6
    addClass("NavigationAgent3D")

    // 7
    addClass("NavigationMesh")

    // 8
    addMember("AudioStream", "can_be_sampled", GodotElementType.METHOD)

    // 9
    addMember("AudioStream", "generate_sample", GodotElementType.METHOD)

    // 10
    addClass("EditorDock")

    // 11
    addClass("AudioSample")

    // 12
    addMember("EngineDebugger", "get_depth", GodotElementType.METHOD)

    // 13
    addMember("EngineDebugger", "get_lines_left", GodotElementType.METHOD)

    // 14
    addMember("EngineDebugger", "set_depth", GodotElementType.METHOD)

    // 15
    addMember("EngineDebugger", "set_lines_left", GodotElementType.METHOD)

    // 16
    addMember("SubViewportContainer", "_propagate_input_event", GodotElementType.METHOD)

    // 17
    addClass("SkeletonModification2DJiggle")

    // 18
    addClass("NavigationRegion2D")

    // 19–24 AudioServer methods
    addMember("AudioServer", "get_input_buffer_length_frames", GodotElementType.METHOD)
    addMember("AudioServer", "get_input_frames", GodotElementType.METHOD)
    addMember("AudioServer", "get_input_frames_available", GodotElementType.METHOD)
    addMember("AudioServer", "is_stream_registered_as_sample", GodotElementType.METHOD)
    addMember("AudioServer", "register_stream_as_sample", GodotElementType.METHOD)
    addMember("AudioServer", "set_input_device_active", GodotElementType.METHOD)

    // 25–28 AudioServer constants
    addMember("AudioServer", "PLAYBACK_TYPE_DEFAULT", GodotElementType.CONSTANT)
    addMember("AudioServer", "PLAYBACK_TYPE_STREAM", GodotElementType.CONSTANT)
    addMember("AudioServer", "PLAYBACK_TYPE_SAMPLE", GodotElementType.CONSTANT)
    addMember("AudioServer", "PLAYBACK_TYPE_MAX", GodotElementType.CONSTANT)

    // 29–33
    addClass("NavigationLink2D")
    addClass("NavigationPathQueryResult2D")
    addMember("EditorInterface", "popup_create_dialog", GodotElementType.METHOD)
    addClass("SkeletonModification2DTwoBoneIK")
    addClass("DPITexture")

    // 34–36 ProjectSettings
    addMember("ProjectSettings", "audio/general/default_playback_type", GodotElementType.PROPERTY)
    addMember("ProjectSettings", "audio/general/default_playback_type.web", GodotElementType.PROPERTY)
    addMember(
        "ProjectSettings",
        "rendering/driver/threads/thread_model",
        GodotElementType.PROPERTY,
        "This setting has several known bugs which can lead to crashing, especially when using particles or resizing the window. Not recommended for use in production at this stage.",
    )

    // 37–38 Control constants
    addMember(
        "Control",
        "NOTIFICATION_MOUSE_ENTER_SELF",
        GodotElementType.CONSTANT,
        "The reason this notification is sent may change in the future.",
    )
    addMember(
        "Control",
        "NOTIFICATION_MOUSE_EXIT_SELF",
        GodotElementType.CONSTANT,
        "The reason this notification is sent may change in the future.",
    )

    // 39–44
    addClass("SkeletonModification2DFABRIK")
    addClass("XRFaceTracker")
    addClass("XRBodyTracker")
    addClass("SkeletonModificationStack2D")
    addClass("NavigationRegion3D")
    addClass("XRFaceModifier3D")

    // 45–51 BaseMaterial3D properties
    addMember(
        "BaseMaterial3D",
        "depth_test",
        GodotElementType.PROPERTY,
        "May be affected by future rendering pipeline changes.",
    )
    addMember(
        "BaseMaterial3D",
        "stencil_color",
        GodotElementType.PROPERTY,
        "May be affected by future rendering pipeline changes.",
    )
    addMember(
        "BaseMaterial3D",
        "stencil_compare",
        GodotElementType.PROPERTY,
        "May be affected by future rendering pipeline changes.",
    )
    addMember(
        "BaseMaterial3D",
        "stencil_flags",
        GodotElementType.PROPERTY,
        "May be affected by future rendering pipeline changes.",
    )
    addMember(
        "BaseMaterial3D",
        "stencil_mode",
        GodotElementType.PROPERTY,
        "May be affected by future rendering pipeline changes.",
    )
    addMember(
        "BaseMaterial3D",
        "stencil_outline_thickness",
        GodotElementType.PROPERTY,
        "May be affected by future rendering pipeline changes.",
    )
    addMember(
        "BaseMaterial3D",
        "stencil_reference",
        GodotElementType.PROPERTY,
        "May be affected by future rendering pipeline changes.",
    )

    // 52–63
    addClass("NavigationMeshSourceGeometryData2D")
    addClass("SkeletonModification2D")
    addClass("NavigationObstacle3D")
    addClass("Compositor", "More customization of the rendering pipeline will be added in the future.")
    addClass("NavigationPolygon")
    addClass("SkeletonModification2DStackHolder")
    addClass("NavigationServer2D")
    addClass("StreamPeerGZIP")
    addClass("NavigationServer3D")
    addClass("NavigationLink3D")
    addClass("NavigationPathQueryResult3D")
    addClass("AudioSamplePlayback")

    // 64
    addMember("LightmapGI", "shadowmask_mode", GodotElementType.PROPERTY)

    // 65
    addMember(
        "TextureRect",
        "expand_mode",
        GodotElementType.PROPERTY,
        "Using EXPAND_FIT_* may result in unstable behavior in some Container controls. This behavior may be re-evaluated and changed in the future.",
    )

    // 66–72
    addClass("NavigationAgent2D")
    addClass("SkeletonModification2DCCDIK")
    addClass("NavigationPathQueryParameters3D")
    addClass("SkeletonModification2DLookAt")
    addClass("XRBodyModifier3D")
    addClass("NavigationMeshSourceGeometryData3D")
    addClass("NavigationObstacle2D")

    // 73–74
    addMember("AudioStreamPlayback", "get_sample_playback", GodotElementType.METHOD)
    addMember("AudioStreamPlayback", "set_sample_playback", GodotElementType.METHOD)

    // 75–76
    addClass("NavigationPathQueryParameters2D")
    addClass("AnimationNodeExtension")
}
