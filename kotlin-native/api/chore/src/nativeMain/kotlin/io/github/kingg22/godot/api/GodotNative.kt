package io.github.kingg22.godot.api

import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode
import kotlinx.cinterop.COpaquePointer
import org.jetbrains.annotations.ApiStatus

// TODO name

/** Marker interface for all Godot native objects backed by a [C pointer][COpaquePointer] or can be converted to one. */
@UsedFromCodegenGeneratedCode
@ApiStatus.NonExtendable
@SubclassOptInRequired(InternalForInheritanceGodotApi::class)
public interface GodotNative {
    @UsedFromCodegenGeneratedCode
    public val rawPtr: COpaquePointer
}
