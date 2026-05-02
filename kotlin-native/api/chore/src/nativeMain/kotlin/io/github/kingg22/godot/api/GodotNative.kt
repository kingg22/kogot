package io.github.kingg22.godot.api

import kotlinx.cinterop.COpaquePointer
import org.jetbrains.annotations.ApiStatus

// TODO name

/** Marker interface for all Godot native objects backed by a [C pointer][COpaquePointer] or can be converted to one. */
@ExperimentalGodotKotlin
@ApiStatus.NonExtendable
public interface GodotNative {
    public val rawPtr: COpaquePointer
}
