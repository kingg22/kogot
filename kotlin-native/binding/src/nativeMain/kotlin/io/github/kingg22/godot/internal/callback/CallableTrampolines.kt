package io.github.kingg22.godot.internal.callback

import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.StringBinding
import io.github.kingg22.godot.internal.binding.getInstance
import io.github.kingg22.godot.internal.ffi.FALSE
import io.github.kingg22.godot.internal.ffi.GDExtensionBool
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomEqual
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomFree
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomGetArgumentCount
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomHash
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomIsValid
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomLessThan
import io.github.kingg22.godot.internal.ffi.GDExtensionCallableCustomToString
import io.github.kingg22.godot.internal.ffi.TRUE
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.set
import kotlinx.cinterop.staticCFunction

/**
 * Static C trampolines for custom callable callbacks.
 *
 * These trampolines are invoked by Godot when a custom callable is called.
 * They extract the userdata (which is a StableRef containing a Long ID),
 * retrieve the KotlinCallable from the map, and dispatch to it.
 */
@OptIn(ExperimentalForeignApi::class)
@InternalBinding
public object CallableTrampolines {

    /**
     * Free trampoline - invoked when the callable is destroyed.
     * Cleans up the userdata from the map.
     */
    public val freeFunc: GDExtensionCallableCustomFree = staticCFunction { userdata ->
        val ref = requireNotNull(userdata).asStableRef<Long>()
        val id = ref.get()
        CallableUserdataMap.remove(id)
        ref.dispose()
    }

    /**
     * Hash trampoline - returns hash of the callable.
     */
    public val hashFunc: GDExtensionCallableCustomHash = staticCFunction { userdata ->
        CallableUserdataMap.get(userdata.getInstance())?.hash()?.toUInt() ?: 0u
    }

    /**
     * Is valid trampoline - checks if callable is still valid.
     */
    public val isValidFunc: GDExtensionCallableCustomIsValid = staticCFunction { userdata ->
        val id = userdata.getInstance<Long>()
        val callable = CallableUserdataMap.get(id)
        if (callable != null && callable.isValid()) GDExtensionBool.TRUE else GDExtensionBool.FALSE
    }

    /**
     * Equal trampoline - checks equality with another callable.
     */
    public val equalFunc: GDExtensionCallableCustomEqual = staticCFunction { userdataA, userdataB ->
        val callableA = CallableUserdataMap.get(userdataA.getInstance())
        val callableB = CallableUserdataMap.get(userdataB.getInstance())
        if (callableA != null && callableB != null &&
            callableA.equals(callableB)
        ) {
            GDExtensionBool.TRUE
        } else {
            GDExtensionBool.FALSE
        }
    }

    /**
     * Less than trampoline - comparison for sorting.
     */
    public val lessThanFunc: GDExtensionCallableCustomLessThan = staticCFunction { userdataA, userdataB ->
        val callableA = CallableUserdataMap.get(userdataA.getInstance())
        val callableB = CallableUserdataMap.get(userdataB.getInstance())
        if (callableA != null && callableB != null &&
            callableA.lessThan(callableB)
        ) {
            GDExtensionBool.TRUE
        } else {
            GDExtensionBool.FALSE
        }
    }

    /**
     * Argument count trampoline - returns the number of arguments.
     */
    public val getArgumentCountFunc: GDExtensionCallableCustomGetArgumentCount = staticCFunction { userdata, rIsValid ->
        val callable = CallableUserdataMap.get(userdata.getInstance())
        val arity = callable?.arity()?.toLong() ?: 0L
        if (rIsValid != null) {
            val validFlag = if (callable != null && callable.isValid()) GDExtensionBool.TRUE else GDExtensionBool.FALSE
            rIsValid[0] = validFlag
        }
        arity
    }

    /**
     * To string trampoline - returns string representation for debugging.
     */
    public val toStringFunc: GDExtensionCallableCustomToString = staticCFunction { userdata, rIsValid, rReturnPtr ->
        val callableId = userdata.getInstance<Long>()
        val callable = CallableUserdataMap.get(callableId)
        if (rIsValid != null) {
            val validFlag = if (callable != null && callable.isValid()) GDExtensionBool.TRUE else GDExtensionBool.FALSE
            rIsValid[0] = validFlag
        }
        val string = callable?.toString() ?: "Invalid Callable"
        StringBinding.instance.newWithUtf8Chars(rReturnPtr, string)
    }
}
