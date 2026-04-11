package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.asStringName
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.internal.ffi.*
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.staticCFunction
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Inline function to get an instance from a COpaquePointer.
 */
@InternalBinding
public inline fun <reified T : Any> COpaquePointer?.getInstance(): T {
    contract { returns() implies (this@getInstance != null) }
    return requireNotNull(this).asStableRef<T>().get()
}

/**
 * Notification function that calls [Node._ready] when [Node.NOTIFICATION_READY] is received.
 *
 * Godot sends `NOTIFICATION_READY` (value 13) when a node enters the scene tree and is ready.
 * This function dispatches it to the Kotlin instance's `_ready` method.
 */
@InternalBinding
public val notificationFunc: GDExtensionClassNotification2 = staticCFunction { instancePtr, notification, _ ->
    if (notification.toLong() == Node.NOTIFICATION_READY) {
        instancePtr.getInstance<Node>()._ready()
    }
}

/**
 * Creates a create_instance function for the given class.
 *
 * Must be called with [staticCFunction]
 *
 * @param T The class type to instantiate
 * @param parentClassName The Godot parent class name (e.g., "Node2D", "Sprite2D")
 * @param factory A lambda that creates a new instance of T wrapped in StableRef
 */
@InternalBinding
public fun <T : GodotObject> createInstanceFunc(
    parentClassName: String,
    className: String,
    factory: (parentPtr: GDExtensionObjectPtr) -> T,
    notifyPostInitialize: Boolean = false,
): GDExtensionObjectPtr? {
    contract { callsInPlace(factory, InvocationKind.AT_MOST_ONCE) }

    println("[Kogot] CreateInstance: Creating $parentClassName instance")
    val base = ClassDBBinding.instance.constructObject2Raw(parentClassName.asStringName().rawPtr)
        ?: error("Failed to construct base $parentClassName")
    println("[Kogot] CreateInstance: Base $parentClassName constructed. $base")

    val instance = try {
        factory(base)
    } catch (e: Exception) {
        println("Failed to create instance of $parentClassName")
        e.printStackTrace()
        return null
    }
    val selfRef = StableRef.create(instance)
    val selfPtr = selfRef.asCPointer()

    ObjectBinding.instance.setInstanceRaw(
        base,
        className.asStringName().rawPtr,
        selfPtr,
    )

    memScoped {
        ObjectBinding.instance.setInstanceBindingRaw(
            pO = base,
            pToken = BindingProcAddressHolder.library,
            pBinding = selfPtr,
            pCallbacks = cValue<GDExtensionInstanceBindingCallbacks> {
                create_callback = null
                free_callback = null
                reference_callback = null
            }.ptr,
        )
    }

    // Send NOTIFICATION_POSTINITIALIZE if Godot requests it
    if (notifyPostInitialize) {
        println("[Kogot] CreateInstance: Sending NOTIFICATION_POSTINITIALIZE")
        instance.notification(GodotObject.NOTIFICATION_POSTINITIALIZE.toInt())
    }

    println("[Kogot] CreateInstance: Instance created successfully, instance: ${instance.rawPtr}. $instance")
    return base
}

/**
 * Creates a free_instance function that disposes the StableRef.
 */
@InternalBinding
public fun createFreeInstanceFunc(): GDExtensionClassFreeInstance = staticCFunction { _, ptr ->
    require(ptr != null) { "FreeInstance: ptr is null" }
    println("[Kogot] FreeInstance: Freeing $ptr")
    ptr.asStableRef<Any>().dispose()
}

@InternalBinding
public fun classCreationInfo5(
    createInstance: GDExtensionClassCreateInstance2,
    freeInstance: GDExtensionClassFreeInstance,
    getVirtual: GDExtensionClassGetVirtual2,
    ptrUserData: COpaquePointer,
): CValue<GDExtensionClassCreationInfo5> = cValue<GDExtensionClassCreationInfo5> {
    is_virtual = GDExtensionBool.FALSE
    is_abstract = GDExtensionBool.FALSE
    is_exposed = GDExtensionBool.TRUE
    set_func = null
    get_func = null
    get_property_list_func = null
    free_property_list_func = null
    property_can_revert_func = null
    property_get_revert_func = null
    validate_property_func = null
    notification_func = notificationFunc
    to_string_func = null
    reference_func = null
    unreference_func = null
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
 * @param freeInstance The free_instance function
 * @param getVirtual The get_virtual function
 */
@InternalBinding
public inline fun <reified T : Any> registerClass(
    className: String,
    parentClassName: String,
    createInstance: GDExtensionClassCreateInstance2,
    freeInstance: GDExtensionClassFreeInstance,
    getVirtual: GDExtensionClassGetVirtual2,
) {
    val info = classCreationInfo5(createInstance, freeInstance, getVirtual, StableRef.create(T::class).asCPointer())

    memScoped {
        ClassDBBinding.instance.registerExtensionClass5Raw(
            BindingProcAddressHolder.library,
            className.asStringName().rawPtr,
            parentClassName.asStringName().rawPtr,
            info.ptr,
        )
    }

    println("[Kogot] Registered class: '$className' extends '$parentClassName'")
}
