package io.github.kingg22.godot

import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.api.singleton.ResourceLoader
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtr

/**
 * Loads the resource at [path] via [ResourceLoader] and safely casts it to [T].
 *
 * [typeHint] defaults to [T]'s simple name, following the same Kotlin-class-name-as-Godot-class-name
 * convention already used by [castTo]. There is no runtime Godot-class-name -> Kotlin-constructor
 * registry yet, so the caller must supply [factory] (e.g. `::Texture2D`) to construct [T] from the
 * loaded native pointer.
 *
 * `ResourceLoader.load` hands Kotlin one owned engine reference per call, and this function is the one
 * place that knows it — so it is always settled here (issue #114): adopted by the new wrapper on a first
 * sighting, released when the same still-live resource is loaded again (the wrapper already owns its
 * own), and released when the cast fails. Loading the same path in a loop therefore does not make the
 * engine refcount climb.
 *
 * @throws ClassCastException if the resource at [path] is not actually a [T].
 * @see ResourceLoader.load
 */
public inline fun <reified T : Resource> GD.load(
    path: String,
    typeHint: String = T::class.simpleName!!,
    cacheMode: ResourceLoader.CacheMode = ResourceLoader.CacheMode.REUSE,
    noinline factory: (nativePtr: GDExtensionObjectPtr) -> T,
): T {
    val godotClassName = T::class.simpleName!!
    return castToOwnedInternal(
        ResourceLoader.instance.load(path, typeHint, cacheMode).rawPtr,
        godotClassName,
        factory,
    ) ?: throw ClassCastException("Failed to cast pointer to $godotClassName")
}

/**
 * Like [load], but returns `null` instead of throwing when the resource at [path] is not a [T]. The
 * reference `ResourceLoader.load` handed over is released in that case too.
 */
public inline fun <reified T : Resource> GD.loadOrNull(
    path: String,
    typeHint: String = T::class.simpleName!!,
    cacheMode: ResourceLoader.CacheMode = ResourceLoader.CacheMode.REUSE,
    noinline factory: (nativePtr: GDExtensionObjectPtr) -> T,
): T? = castToOwnedInternal(
    ResourceLoader.instance.load(path, typeHint, cacheMode).rawPtr,
    T::class.simpleName!!,
    factory,
)
