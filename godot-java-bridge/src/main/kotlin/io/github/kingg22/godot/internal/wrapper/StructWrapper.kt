@file:JvmName("StructWrapper")
@file:Suppress("ktlint:standard:function-naming", "FunctionName")

package io.github.kingg22.godot.internal.wrapper

import io.github.kingg22.godot.internal.ffm.GDExtensionCallableCustomInfo2
import io.github.kingg22.godot.internal.ffm.GDExtensionClassCreationInfo4
import io.github.kingg22.godot.internal.ffm.GDExtensionClassMethodInfo
import io.github.kingg22.godot.internal.ffm.GDExtensionClassVirtualMethodInfo
import io.github.kingg22.godot.internal.ffm.GDExtensionInstanceBindingCallbacks
import io.github.kingg22.godot.internal.ffm.GDExtensionMainLoopCallbacks
import io.github.kingg22.godot.internal.ffm.GDExtensionMethodInfo
import io.github.kingg22.godot.internal.ffm.GDExtensionPropertyInfo
import io.github.kingg22.godot.internal.ffm.GDExtensionScriptInstanceInfo3
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator

/**
 * Creates a new [GDExtensionInstanceBindingCallbacks] instance.
 * @return A pointer to instance
 * @see io.github.kingg22.godot.internal.ffm.GDExtensionInstanceBindingCreateCallback
 * @see io.github.kingg22.godot.internal.ffm.GDExtensionInstanceBindingFreeCallback
 * @see io.github.kingg22.godot.internal.ffm.GDExtensionInstanceBindingReferenceCallback
 */
@JvmOverloads
fun GDExtensionInstanceBindingCallbacks(
    createCallback: MemorySegment,
    freeCallback: MemorySegment,
    referenceCallback: MemorySegment,
    allocator: SegmentAllocator = Arena.ofAuto(),
): MemorySegment {
    val struct = GDExtensionInstanceBindingCallbacks.allocate(allocator)
    GDExtensionInstanceBindingCallbacks.create_callback(struct, createCallback)
    GDExtensionInstanceBindingCallbacks.free_callback(struct, freeCallback)
    GDExtensionInstanceBindingCallbacks.reference_callback(struct, referenceCallback)
    return struct
}

/**
 * Creates a new [GDExtensionCallableCustomInfo2] instance.
 *
 * Only [callFunc] and [token] are strictly required, however, [objectId] should be passed if it's not a static
 * method.
 *
 * `token` should point to an address that uniquely identifies the GDExtension (e.g., the
 * `GDExtensionClassLibraryPtr` passed to the entry symbol function.
 *
 * [hashFunc], [equalFunc], and [lessThanFunc] are optional. If not provided both [callFunc] and
 * [callableUserdata] together are used as the identity of the callable for hashing and comparison purposes.
 *
 * The hash returned by [hashFunc] is cached, `hash_func` will not be called more than once per callable.
 *
 * [isValidFunc] is necessary if the validity of the callable can change before destruction.
 *
 * [freeFunc] is necessary if [callableUserdata] needs to be cleaned up when the callable is freed.
 *
 * @return A pointer to instance
 */
@JvmOverloads
fun GDExtensionCallableCustomInfo2(
    token: MemorySegment,
    objectId: Long,
    callFunc: MemorySegment,
    isValidFunc: MemorySegment = MemorySegment.NULL,
    freeFunc: MemorySegment = MemorySegment.NULL,
    hashFunc: MemorySegment = MemorySegment.NULL,
    equalFunc: MemorySegment = MemorySegment.NULL,
    lessThanFunc: MemorySegment = MemorySegment.NULL,
    toStringFunc: MemorySegment = MemorySegment.NULL,
    getArgumentCountFunc: MemorySegment = MemorySegment.NULL,
    callableUserdata: MemorySegment = MemorySegment.NULL,
    allocator: SegmentAllocator = Arena.ofAuto(),
): MemorySegment {
    val struct = GDExtensionCallableCustomInfo2.allocate(allocator)
    GDExtensionCallableCustomInfo2.callable_userdata(struct, callableUserdata)
    GDExtensionCallableCustomInfo2.token(struct, token)
    GDExtensionCallableCustomInfo2.object_id(struct, objectId)
    GDExtensionCallableCustomInfo2.call_func(struct, callFunc)
    GDExtensionCallableCustomInfo2.is_valid_func(struct, isValidFunc)
    GDExtensionCallableCustomInfo2.free_func(struct, freeFunc)
    GDExtensionCallableCustomInfo2.hash_func(struct, hashFunc)
    GDExtensionCallableCustomInfo2.equal_func(struct, equalFunc)
    GDExtensionCallableCustomInfo2.less_than_func(struct, lessThanFunc)
    GDExtensionCallableCustomInfo2.to_string_func(struct, toStringFunc)
    GDExtensionCallableCustomInfo2.get_argument_count_func(struct, getArgumentCountFunc)
    return struct
}

/**
 * Create a new [GDExtensionClassCreationInfo5] instance
 * @see GDExtensionClassCreationInfo4
 *
 * @param createInstanceFunc (Default) constructor; mandatory. If the class is not instantiable, consider making it virtual or abstract.
 * @param freeInstanceFunc Destructor; mandatory.
 * @param classUserdata Per-class user data, later accessible in instance bindings.
 * @param getVirtualFunc Queries a virtual function by name and returns a callback to invoke the requested virtual function.
 * @param getVirtualCallDataFunc Paired with [callVirtualWithDataFunc], this is an alternative to [getVirtualFunc]
 * for extensions that need or benefit from extra data when calling virtual functions.
 * Returns user data that will be passed to [callVirtualWithDataFunc].
 * Returning [MemorySegment.NULL] from this function signals to Godot that the virtual function is not overridden.
 * Data returned from this function should be managed by the extension and must be valid until the extension is
 * deinitialized.
 * You should supply either [getVirtualFunc], or [getVirtualCallDataFunc] with [callVirtualWithDataFunc].
 * @param callVirtualWithDataFunc Used to call virtual functions when [getVirtualCallDataFunc] is not null.
 *
 * @return A pointer to instance
 */
@JvmOverloads
fun GDExtensionClassCreationInfo5(
    isVirtual: Boolean,
    isAbstract: Boolean,
    isExposed: Boolean,
    isRuntime: Boolean,
    createInstanceFunc: MemorySegment,
    freeInstanceFunc: MemorySegment,
    classUserdata: MemorySegment = MemorySegment.NULL,
    iconPath: String? = null,
    setFunc: MemorySegment = MemorySegment.NULL,
    getFunc: MemorySegment = MemorySegment.NULL,
    getPropertyListFunc: MemorySegment = MemorySegment.NULL,
    freePropertyListFunc: MemorySegment = MemorySegment.NULL,
    propertyCanReverFunc: MemorySegment = MemorySegment.NULL,
    propertyGetReverFunc: MemorySegment = MemorySegment.NULL,
    validatePropertyFunc: MemorySegment = MemorySegment.NULL,
    notificationFunc: MemorySegment = MemorySegment.NULL,
    toStringFunc: MemorySegment = MemorySegment.NULL,
    referenceFunc: MemorySegment = MemorySegment.NULL,
    unreferenceFunc: MemorySegment = MemorySegment.NULL,
    recreateInstanceFunc: MemorySegment = MemorySegment.NULL,
    getVirtualFunc: MemorySegment = MemorySegment.NULL,
    getVirtualCallDataFunc: MemorySegment = MemorySegment.NULL,
    callVirtualWithDataFunc: MemorySegment = MemorySegment.NULL,
    allocator: SegmentAllocator = Arena.ofAuto(),
): MemorySegment {
    val struct = GDExtensionClassCreationInfo4.allocate(allocator)

    GDExtensionClassCreationInfo4.is_virtual(struct, if (isVirtual) 1 else 0)
    GDExtensionClassCreationInfo4.is_abstract(struct, if (isAbstract) 1 else 0)
    GDExtensionClassCreationInfo4.is_exposed(struct, if (isExposed) 1 else 0)
    GDExtensionClassCreationInfo4.is_runtime(struct, if (isRuntime) 1 else 0)
    GDExtensionClassCreationInfo4.icon_path(struct, allocator.allocateFrom(iconPath))
    GDExtensionClassCreationInfo4.create_instance_func(struct, createInstanceFunc)
    GDExtensionClassCreationInfo4.free_instance_func(struct, freeInstanceFunc)

    GDExtensionClassCreationInfo4.set_func(struct, setFunc)
    GDExtensionClassCreationInfo4.get_func(struct, getFunc)
    GDExtensionClassCreationInfo4.get_property_list_func(struct, getPropertyListFunc)
    GDExtensionClassCreationInfo4.free_property_list_func(struct, freePropertyListFunc)
    GDExtensionClassCreationInfo4.property_can_revert_func(struct, propertyCanReverFunc)
    GDExtensionClassCreationInfo4.property_get_revert_func(struct, propertyGetReverFunc)
    GDExtensionClassCreationInfo4.validate_property_func(struct, validatePropertyFunc)
    GDExtensionClassCreationInfo4.notification_func(struct, notificationFunc)
    GDExtensionClassCreationInfo4.to_string_func(struct, toStringFunc)
    GDExtensionClassCreationInfo4.reference_func(struct, referenceFunc)
    GDExtensionClassCreationInfo4.unreference_func(struct, unreferenceFunc)
    GDExtensionClassCreationInfo4.recreate_instance_func(struct, recreateInstanceFunc)
    GDExtensionClassCreationInfo4.get_virtual_func(struct, getVirtualFunc)
    GDExtensionClassCreationInfo4.get_virtual_call_data_func(struct, getVirtualCallDataFunc)
    GDExtensionClassCreationInfo4.call_virtual_with_data_func(struct, callVirtualWithDataFunc)
    GDExtensionClassCreationInfo4.class_userdata(struct, classUserdata)

    return struct
}

/**
 * Create a new [GDExtensionClassMethodInfo] instance.
 * For more information, see the class documentation.
 *
 * @param methodFlags Bitfield of [io.github.kingg22.godot.internal.ffm.GDExtensionClassMethodFlags].
 * @param hasReturnValue If `has_return_value` is false, [returnValueInfo] and [returnValueMetadata] are ignored.
 * todo Consider dropping `has_return_value` and making the other two properties match
 * [GDExtensionClassMethodInfo] and [io.github.kingg22.godot.internal.ffm.GDExtensionClassVirtualMethodInfo]
 * for consistency in future version of this struct.
 *
 * @return A pointer to instance
 */
@JvmOverloads
fun GDExtensionClassMethodInfo(
    name: MemorySegment,
    callFunc: MemorySegment,
    methodFlags: Int,
    hasReturnValue: Boolean,
    returnValueMetadata: Int,
    argumentCount: Int,
    defaultArgumentCount: Int,
    methodUserdata: MemorySegment = MemorySegment.NULL,
    ptrcallFunc: MemorySegment = MemorySegment.NULL,
    returnValueInfo: MemorySegment = MemorySegment.NULL,
    argumentsInfo: MemorySegment = MemorySegment.NULL,
    argumentsMetadata: MemorySegment = MemorySegment.NULL,
    defaultArguments: MemorySegment = MemorySegment.NULL,
    allocator: SegmentAllocator = Arena.ofAuto(),
): MemorySegment {
    val struct = GDExtensionClassMethodInfo.allocate(allocator)
    GDExtensionClassMethodInfo.name(struct, name)
    GDExtensionClassMethodInfo.method_userdata(struct, methodUserdata)
    GDExtensionClassMethodInfo.call_func(struct, callFunc)
    GDExtensionClassMethodInfo.ptrcall_func(struct, ptrcallFunc)
    GDExtensionClassMethodInfo.method_flags(struct, methodFlags)
    GDExtensionClassMethodInfo.has_return_value(struct, if (hasReturnValue) 1.toByte() else 0.toByte())
    GDExtensionClassMethodInfo.return_value_info(struct, returnValueInfo)
    GDExtensionClassMethodInfo.return_value_metadata(struct, returnValueMetadata)
    GDExtensionClassMethodInfo.argument_count(struct, argumentCount)
    GDExtensionClassMethodInfo.arguments_info(struct, argumentsInfo)
    GDExtensionClassMethodInfo.arguments_metadata(struct, argumentsMetadata)
    GDExtensionClassMethodInfo.default_argument_count(struct, defaultArgumentCount)
    GDExtensionClassMethodInfo.default_arguments(struct, defaultArguments)
    return struct
}

/**
 * Creates a new [GDExtensionScriptInstanceInfo3] backed by an [Arena.ofAuto] allocation.
 * @param getClassCategoryFunc Optional. Set to NULL for the default behavior.
 * @return A pointer to instance
 */
@JvmOverloads
fun GDExtensionScriptInstanceInfo3(
    setFunc: MemorySegment,
    getFunc: MemorySegment,
    getPropertyListFunc: MemorySegment,
    freePropertyListFunc: MemorySegment,
    propertyCanRevertFunc: MemorySegment,
    propertyGetRevertFunc: MemorySegment,
    getOwnerFunc: MemorySegment,
    getPropertyStateFunc: MemorySegment,
    getMethodListFunc: MemorySegment,
    freeMethodListFunc: MemorySegment,
    getPropertyTypeFunc: MemorySegment,
    validatePropertyFunc: MemorySegment,
    hasMethodFunc: MemorySegment,
    getMethodArgumentCountFunc: MemorySegment,
    callFunc: MemorySegment,
    notificationFunc: MemorySegment,
    toStringFunc: MemorySegment,
    refcountIncrementedFunc: MemorySegment,
    refcountDecrementedFunc: MemorySegment,
    getScriptFunc: MemorySegment,
    isPlaceholderFunc: MemorySegment,
    setFallbackFunc: MemorySegment,
    getFallbackFunc: MemorySegment,
    getLanguageFunc: MemorySegment,
    freeFunc: MemorySegment,
    getClassCategoryFunc: MemorySegment = MemorySegment.NULL,
    allocator: SegmentAllocator = Arena.ofAuto(),
): MemorySegment {
    val struct = GDExtensionScriptInstanceInfo3.allocate(allocator)
    GDExtensionScriptInstanceInfo3.set_func(struct, setFunc)
    GDExtensionScriptInstanceInfo3.get_func(struct, getFunc)
    GDExtensionScriptInstanceInfo3.get_property_list_func(struct, getPropertyListFunc)
    GDExtensionScriptInstanceInfo3.free_property_list_func(struct, freePropertyListFunc)
    GDExtensionScriptInstanceInfo3.get_class_category_func(struct, getClassCategoryFunc)
    GDExtensionScriptInstanceInfo3.property_can_revert_func(struct, propertyCanRevertFunc)
    GDExtensionScriptInstanceInfo3.property_get_revert_func(struct, propertyGetRevertFunc)
    GDExtensionScriptInstanceInfo3.get_owner_func(struct, getOwnerFunc)
    GDExtensionScriptInstanceInfo3.get_property_state_func(struct, getPropertyStateFunc)
    GDExtensionScriptInstanceInfo3.get_method_list_func(struct, getMethodListFunc)
    GDExtensionScriptInstanceInfo3.free_method_list_func(struct, freeMethodListFunc)
    GDExtensionScriptInstanceInfo3.get_property_type_func(struct, getPropertyTypeFunc)
    GDExtensionScriptInstanceInfo3.validate_property_func(struct, validatePropertyFunc)
    GDExtensionScriptInstanceInfo3.has_method_func(struct, hasMethodFunc)
    GDExtensionScriptInstanceInfo3.get_method_argument_count_func(struct, getMethodArgumentCountFunc)
    GDExtensionScriptInstanceInfo3.call_func(struct, callFunc)
    GDExtensionScriptInstanceInfo3.notification_func(struct, notificationFunc)
    GDExtensionScriptInstanceInfo3.to_string_func(struct, toStringFunc)
    GDExtensionScriptInstanceInfo3.refcount_incremented_func(struct, refcountIncrementedFunc)
    GDExtensionScriptInstanceInfo3.refcount_decremented_func(struct, refcountDecrementedFunc)
    GDExtensionScriptInstanceInfo3.get_script_func(struct, getScriptFunc)
    GDExtensionScriptInstanceInfo3.is_placeholder_func(struct, isPlaceholderFunc)
    GDExtensionScriptInstanceInfo3.set_fallback_func(struct, setFallbackFunc)
    GDExtensionScriptInstanceInfo3.get_fallback_func(struct, getFallbackFunc)
    GDExtensionScriptInstanceInfo3.get_language_func(struct, getLanguageFunc)
    GDExtensionScriptInstanceInfo3.free_func(struct, freeFunc)
    return struct
}

/**
 * Create a new [GDExtensionClassVirtualMethodInfo] instance.
 * For more information, see the class documentation.
 * @param methodFlags Bitfield of [io.github.kingg22.godot.internal.ffm.GDExtensionClassMethodFlags].
 * @return A pointer to instance
 */
@JvmOverloads
fun GDExtensionClassVirtualMethodInfo(
    name: MemorySegment,
    methodFlags: Int,
    returnValue: MemorySegment,
    returnValueMetadata: Int,
    argumentCount: Int,
    arguments: MemorySegment = MemorySegment.NULL,
    argumentsMetadata: MemorySegment = MemorySegment.NULL,
    allocator: SegmentAllocator = Arena.ofAuto(),
): MemorySegment {
    val struct = GDExtensionClassVirtualMethodInfo.allocate(allocator)
    GDExtensionClassVirtualMethodInfo.name(struct, name)
    GDExtensionClassVirtualMethodInfo.method_flags(struct, methodFlags)
    GDExtensionClassVirtualMethodInfo.return_value(struct, returnValue)
    GDExtensionClassVirtualMethodInfo.return_value_metadata(struct, returnValueMetadata)
    GDExtensionClassVirtualMethodInfo.argument_count(struct, argumentCount)
    GDExtensionClassVirtualMethodInfo.arguments(struct, arguments)
    GDExtensionClassVirtualMethodInfo.arguments_metadata(struct, argumentsMetadata)
    return struct
}

/**
 * Create a new [GDExtensionMainLoopCallbacks] instance.
 * @param startupFunc
 * [GDExtensionMainLoopStartupCallback][io.github.kingg22.godot.internal.ffm.GDExtensionMainLoopStartupCallback]
 * Will be called after Godot is started and is fully initialized.
 * @param shutdownFunc
 * [GDExtensionMainLoopShutdownCallback][io.github.kingg22.godot.internal.ffm.GDExtensionMainLoopShutdownCallback]
 * Will be called before Godot is shutdown when it is still fully initialized.
 * @param frameFunc
 * [GDExtensionMainLoopFrameCallback][io.github.kingg22.godot.internal.ffm.GDExtensionMainLoopFrameCallback]
 * Will be called for each process frame. This will run after all `_process()` methods on Node, and before
 * `ScriptServer::frame()`.
 * This is intended to be the equivalent of `ScriptLanguage::frame()` for GDExtension language bindings that
 * don't use the script API.
 * @return A pointer to instance
 */
@JvmOverloads
fun GDExtensionMainLoopCallbacks(
    startupFunc: MemorySegment,
    shutdownFunc: MemorySegment,
    frameFunc: MemorySegment,
    allocator: SegmentAllocator = Arena.ofAuto(),
): MemorySegment {
    val struct = GDExtensionMainLoopCallbacks.allocate(allocator)
    GDExtensionMainLoopCallbacks.startup_func(struct, startupFunc)
    GDExtensionMainLoopCallbacks.shutdown_func(struct, shutdownFunc)
    GDExtensionMainLoopCallbacks.frame_func(struct, frameFunc)
    return struct
}

/**
 * Create a new [GDExtensionMethodInfo] instance.
 * @param returnValue [io.github.kingg22.godot.internal.ffm.GDExtensionPropertyInfo] pointer.
 * @param argumentCount Arguments: [defaultArguments] is an array of size [argumentCount].
 * @param defaultArgumentCount Default arguments: [defaultArguments] is an array of size [defaultArgumentCount].
 * @param flags Bitfield of [io.github.kingg22.godot.internal.ffm.GDExtensionClassMethodFlags].
 * @param arguments Array of [io.github.kingg22.godot.internal.ffm.GDExtensionPropertyInfo] pointers.
 * @param defaultArguments Array of `GDExtensionVariant` pointer.
 * @return A pointer to instance.
 */
@JvmOverloads
fun GDExtensionMethodInfo(
    name: MemorySegment,
    returnValue: MemorySegment,
    flags: Int,
    id: Int,
    argumentCount: Int,
    defaultArgumentCount: Int,
    arguments: MemorySegment = MemorySegment.NULL,
    defaultArguments: MemorySegment = MemorySegment.NULL,
    allocator: SegmentAllocator = Arena.ofAuto(),
): MemorySegment {
    val struct = GDExtensionMethodInfo.allocate(allocator)
    GDExtensionMethodInfo.name(struct, name)
    GDExtensionMethodInfo.return_value(struct, returnValue)
    GDExtensionMethodInfo.flags(struct, flags)
    GDExtensionMethodInfo.id(struct, id)
    GDExtensionMethodInfo.argument_count(struct, argumentCount)
    GDExtensionMethodInfo.arguments(struct, arguments)
    GDExtensionMethodInfo.default_argument_count(struct, defaultArgumentCount)
    GDExtensionMethodInfo.default_arguments(struct, defaultArguments)
    return struct
}

/**
 * Creates a new [GDExtensionPropertyInfo] backed by an [Arena.ofAuto] allocation.
 * @param type The type of the property [io.github.kingg22.godot.internal.ffm.GDExtensionVariantType].
 * @param hint Bitfield of [io.github.kingg22.godot.internal.ffm.GDExtensionPropertyHint].
 * @param usage Bitfield of [io.github.kingg22.godot.internal.ffm.GDExtensionPropertyUsageFlags].
 * @return A pointer to instance
 */
@JvmOverloads
fun GDExtensionPropertyInfo(
    type: Int,
    name: MemorySegment,
    hint: Int,
    usage: Int,
    className: MemorySegment = MemorySegment.NULL,
    hintString: MemorySegment = MemorySegment.NULL,
    allocator: SegmentAllocator = Arena.ofAuto(),
): MemorySegment {
    val struct = GDExtensionPropertyInfo.allocate(allocator)
    GDExtensionPropertyInfo.type(struct, type)
    GDExtensionPropertyInfo.name(struct, name)
    GDExtensionPropertyInfo.class_name(struct, className)
    GDExtensionPropertyInfo.hint(struct, hint)
    GDExtensionPropertyInfo.hint_string(struct, hintString)
    GDExtensionPropertyInfo.usage(struct, usage)
    return struct
}
