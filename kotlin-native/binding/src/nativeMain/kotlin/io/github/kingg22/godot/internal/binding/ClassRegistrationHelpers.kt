package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.GodotNotification
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.RefCounted
import io.github.kingg22.godot.internal.ffi.*
import kotlinx.cinterop.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Per-instance `notification` hook wired into every registered class through [classCreationInfo6].
 *
 * Forwards each Godot `NOTIFICATION_*` to [GodotNotification._notification] when the class opts in by
 * implementing [GodotNotification] (e.g. to release native handles on `NOTIFICATION_PREDELETE`);
 * a no-op otherwise. `_notification` has no entry in `extension_api.json` and no `get_virtual` slot,
 * so this callback is the only path GDExtension classes have to observe notifications.
 *
 * It deliberately does **not** call `_ready()` here: Godot already dispatches `_ready` through the
 * `get_virtual` table (`NodeVirtualCalls.ready`), and calling it from here as well ran every node's
 * `_ready` twice.
 */
@InternalBinding
public val notificationFunc: GDExtensionClassNotification2 = staticCFunction { instancePtr, what, _ ->
    guardCallback("kogot._notification") {
        (instancePtr.instanceOrNull() as? GodotNotification)?._notification(what)
    }
}

/**
 * Creates a create_instance function for the given class.
 *
 * Must be called with [staticCFunction]
 *
 * The instance is registered through an [InstanceHandle] (see there). A `RefCounted` instance additionally owns one
 * engine reference of its own, given back by its cleaner once Kotlin no longer uses it (issue #114, #213): the object
 * then lives exactly as long as either the engine or Kotlin uses it. The reference `classdb_construct_object3` hands
 * to the creator is untouched here: the engine keeps it when the engine instantiates the class, and Kotlin-side
 * creators give it back (see [createInstanceFromKotlin]).
 *
 * @param T The class type to instantiate
 * @param parentClassName The Godot parent class name (e.g., "Node2D", "Sprite2D")
 * @param factory A lambda that creates a new instance of T wrapped in StableRef
 */
@InternalBinding
public fun <T : GodotObject> createInstanceFunc(
    parentClassName: String,
    className: String,
    notifyPostInitialize: Boolean = false,
    factory: (parentPtr: GDExtensionObjectPtr) -> T,
): GDExtensionObjectPtr? {
    contract { callsInPlace(factory, InvocationKind.AT_MOST_ONCE) }

    // Throwable, not Exception: this runs inside the engine's create_instance callback, and anything that escapes
    // a staticCFunction aborts the process (issue #113).
    try {
        val base = parentClassName.toStringName().use { str ->
            ClassDBBinding.constructObject3Raw(str.rawPtr)
        } ?: error("Failed to construct base $parentClassName")

        val instance = try {
            factory(base)
        } catch (t: Throwable) {
            // Nothing references the half-built base yet: destroy it instead of leaking it.
            ObjectBinding.destroyRaw(base)
            reportCallbackFailure("$className.<init>", t)
            return null
        }
        val selfPtr = createInstanceData(instance, base)

        className.toStringName().use { str ->
            ObjectBinding.setInstanceRaw(
                base,
                str.rawPtr,
                selfPtr,
            )
        }

        memScoped {
            ObjectBinding.setInstanceBindingRaw(
                pO = base,
                pToken = BindingProcAddressHolder.library,
                // Mirrors `instance` for castTo/GD.load/instantiate<T>() lookups (issue #114). The instance's
                // lifetime is governed by the InstanceHandle registered above, so this mirror is weak.
                pBinding = wrapForEagerBinding(instance),
                pCallbacks = cValue<GDExtensionInstanceBindingCallbacks> {
                    create_callback = null
                    free_callback = identityFreeCallback
                    reference_callback = null
                }.ptr,
            )
        }

        // After object_set_instance: the reference() fires reference_func, which needs the instance data.
        if (instance is RefCounted) attachRefCountedRelease(instance, base, takeOwnReference = true)

        // Send NOTIFICATION_POSTINITIALIZE if Godot requests it
        if (notifyPostInitialize) {
            instance.notification(GodotObject.NOTIFICATION_POSTINITIALIZE.toInt())
        }

        return base
    } catch (t: Throwable) {
        reportCallbackFailure("$className.create_instance (base $parentClassName)", t)
        return null
    }
}

/**
 * For the binding's own Kotlin-side creation of a `@Godot` class (the script classes, `instantiate<T>()`): creates
 * the object through [createInstanceFunc] and returns the canonical Kotlin instance, not a second wrapper around the
 * same pointer. For a `RefCounted`, the reference `classdb_construct_object3` handed to the creator is given back
 * right away, so the instance's own reference is the only one Kotlin holds (issue #213). Returns `null` if the
 * creation failed (already reported).
 */
@InternalBinding
public fun <T : GodotObject> createInstanceFromKotlin(
    parentClassName: String,
    className: String,
    factory: (parentPtr: GDExtensionObjectPtr) -> T,
): T? {
    var created: T? = null
    // No NOTIFICATION_POSTINITIALIZE, as before for these Kotlin-side creations (see #214 for sending it).
    val ptr = createInstanceFunc(parentClassName, className, notifyPostInitialize = false) { base ->
        factory(base).also { created = it }
    } ?: return null
    val instance = created ?: return null
    if (instance is RefCounted) releaseCreationReference(ptr)
    return instance
}

/**
 * `reference_func` / `unreference_func` of every registered class: Godot calls them on the 1↔2 refcount transitions
 * of a `RefCounted`, from any thread, and never for other objects. Keeps the [InstanceHandle] strong while the engine
 * holds references besides the instance's own one (refcount above 1), weak otherwise (issue #114).
 *
 * The refcount is read again after switching, so a transition racing on another thread leaves the handle matching
 * the latest count instead of the one this call started from.
 */
private fun syncInstanceStrength(instancePtr: GDExtensionClassInstancePtr?) {
    val handle = instancePtr?.asStableRef<Any>()?.get() as? InstanceHandle ?: return
    val refCounted = RefCounted(handle.rawPtr)
    var count = refCounted.getReferenceCount()
    while (true) {
        if (count > 1) {
            // false: already collected, the object goes on without its Kotlin half (see InstanceHandle).
            val _ = handle.makeStrong()
        } else {
            handle.makeWeak()
        }
        val latest = refCounted.getReferenceCount()
        if ((latest > 1) == (count > 1)) return
        count = latest
    }
}

@InternalBinding
public val referenceFunc: GDExtensionClassReference = staticCFunction { instancePtr ->
    guardCallback("kogot.reference") { syncInstanceStrength(instancePtr) }
}

@InternalBinding
public val unreferenceFunc: GDExtensionClassUnreference = staticCFunction { instancePtr ->
    guardCallback("kogot.unreference") { syncInstanceStrength(instancePtr) }
}

/**
 * Creates a free_instance function that disposes the per-instance StableRef.
 *
 * `userData` (Godot's `p_class_userdata`) is the *per-class* [StableRef] created once in
 * [registerClass], shared by every instance of that class — it must **not** be disposed here, only
 * when the class itself is unregistered. Disposing it on every instance free used to double-dispose
 * it (and leave it dangling for the class's other, still-alive instances) as soon as a second instance
 * of the same class was ever freed; see issue #114.
 */
@InternalBinding
public val freeInstanceFunc: GDExtensionClassFreeInstance = staticCFunction { _, ptr ->
    guardCallback("kogot.free_instance") { ptr?.asStableRef<Any>()?.dispose() }
}

@InternalBinding
public val createToStringFunc: GDExtensionClassToString = staticCFunction { instancePtr, isValidPtr, outStrPtr ->
    if (isValidPtr == null) return@staticCFunction
    if (instancePtr == null) {
        isValidPtr.pointed.value = GDExtensionBool.FALSE
        return@staticCFunction
    }

    isValidPtr.pointed.value = GDExtensionBool.FALSE
    guardCallback("kogot.to_string") {
        val toStringMsg = instancePtr.instanceOrNull().toString().utf16
        memScoped {
            StringBinding.newWithUtf16CharsRaw(outStrPtr, toStringMsg.ptr)
        }
        isValidPtr.pointed.value = GDExtensionBool.TRUE
    }
}

@InternalBinding
public fun classCreationInfo6(
    createInstance: GDExtensionClassCreateInstance2,
    getVirtual: GDExtensionClassGetVirtual2,
    ptrUserData: COpaquePointer,
    freeInstance: GDExtensionClassFreeInstance = freeInstanceFunc,
    isVirtual: GDExtensionBool = GDExtensionBool.FALSE,
    isAbstract: GDExtensionBool = GDExtensionBool.FALSE,
    isExposed: GDExtensionBool = GDExtensionBool.TRUE,
): CValue<GDExtensionClassCreationInfo6> = cValue {
    is_virtual = isVirtual
    is_abstract = isAbstract
    is_exposed = isExposed
    set_func = null
    get_func = null
    get_property_list_func = null
    free_property_list_func = null
    property_can_revert_func = null
    property_get_revert_func = null
    validate_property_func = null
    notification_func = notificationFunc
    to_string_func = createToStringFunc
    reference_func = referenceFunc
    unreference_func = unreferenceFunc
    recreate_instance_func = null
    get_virtual_func = getVirtual
    get_virtual_call_data_func = null
    call_virtual_with_data_func = null
    class_userdata = ptrUserData
    create_instance_func = createInstance
    free_instance_func = freeInstance
}

/**
 * Registers a class with Godot's ClassDB.
 *
 * @param T The Kotlin class type
 * @param className The Godot class name to register
 * @param parentClassName The Godot parent class name (e.g., "Node2D", "Sprite2D")
 * @param createInstance The create_instance function
 * @param getVirtual The get_virtual function
 */
@InternalBinding
public inline fun <reified T : GodotObject> registerClass(
    className: String,
    parentClassName: String,
    createInstance: GDExtensionClassCreateInstance2,
    getVirtual: GDExtensionClassGetVirtual2,
) {
    val info = classCreationInfo6(createInstance, getVirtual, StableRef.create(T::class).asCPointer())

    className.toStringName().use { classStringName ->
        parentClassName.toStringName().use { parentStringName ->
            memScoped {
                ClassDBBinding.registerExtensionClass6Raw(
                    BindingProcAddressHolder.library,
                    classStringName.rawPtr,
                    parentStringName.rawPtr,
                    info.ptr,
                )
            }
        }
    }
}

/**
 * Reverses [registerClass] for [className]: hands the class name back to
 * `classdb_unregister_extension_class`, which drops the class together with every method, property
 * and signal registered on it — including the interned signal-name [io.github.kingg22.godot.api.builtin.StringName]s
 * that would otherwise be reported as `Orphan StringName` at exit.
 *
 * Call only once every instance of the class is freed (Godot refuses to unregister a class that still
 * has live instances). Invoked from the KSP-generated `GeneratedBindings.onDeInitScene()`.
 */
@InternalBinding
public fun unregisterClass(className: String) {
    className.toStringName().use { classStringName ->
        ClassDBBinding.unregisterExtensionClassRaw(
            BindingProcAddressHolder.library,
            classStringName.rawPtr,
        )
    }
}
