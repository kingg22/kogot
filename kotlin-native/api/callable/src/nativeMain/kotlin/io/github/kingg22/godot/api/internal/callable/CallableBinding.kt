package io.github.kingg22.godot.api.internal.callable

import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.CallableBinding
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomCall
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomInfo2
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped

@InternalBinding
public object CallableBinding {

    // Call the low-level Godot function to create custom callable and fill the callable object
    public fun createCustom(callableCustomInfo2: CValue<GDExtensionCallableCustomInfo2>, callablePtr: COpaquePointer) {
        memScoped {
            CallableBinding.instance.customCreate2Raw(
                callablePtr,
                callableCustomInfo2.ptr,
            )
        }
    }

    public fun storeKotlinCallable(lambda: Function<*>): COpaquePointer =
        StableRef.create(wrapLambda(lambda)).asCPointer()

    /**
     * Creates a [GDExtensionCallableCustomInfo2] struct.
     * Delegates to [CallableTrampolines]
     * @param userdata Expects a [KotlinCallable] as [COpaquePointer]
     * @param call The custom function to call this callable
     * @param objectId 0 means _custom_, not bind to object
     */
    public fun createCallableCustomInfo2(
        userdata: COpaquePointer,
        call: GDExtensionCallableCustomCall,
        objectId: ULong = 0uL,
    ): CValue<GDExtensionCallableCustomInfo2> = cValue {
        object_id = objectId
        callable_userdata = userdata
        call_func = call
        token = BindingProcAddressHolder.library
        is_valid_func = CallableTrampolines.isValidFunc
        free_func = CallableTrampolines.freeFunc
        hash_func = CallableTrampolines.hashFunc
        equal_func = CallableTrampolines.equalFunc
        less_than_func = CallableTrampolines.lessThanFunc
        to_string_func = CallableTrampolines.toStringFunc
        get_argument_count_func = CallableTrampolines.getArgumentCountFunc
    }
}
