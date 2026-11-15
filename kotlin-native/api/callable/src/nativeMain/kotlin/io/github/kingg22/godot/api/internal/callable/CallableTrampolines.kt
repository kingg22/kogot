package io.github.kingg22.godot.api.internal.callable

import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.StringBinding
import io.github.kingg22.godot.internal.binding.getInstance
import io.github.kingg22.godot.internal.binding.guardCallback
import io.github.kingg22.godot.internal.ffi.*
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.set
import kotlinx.cinterop.staticCFunction

/**
 * Static C trampolines for custom callable callbacks.
 *
 * These trampolines are invoked by Godot when a custom callable is called.
 * Each one reads its [KotlinCallable] back from the userdata `StableRef` and dispatches to it. They run on
 * whatever thread copies, compares or frees the `Callable`, and call user-overridable members (`equals`,
 * `hashCode`, `toString`), so each body is guarded: a failure is reported and answered with a neutral value
 * instead of aborting the process (issue #113).
 */
@InternalBinding
public object CallableTrampolines {

    /**
     * Free trampoline - invoked when the callable is destroyed.
     * Cleans up the userdata from the map.
     */
    public val freeFunc: GDExtensionCallableCustomFree = staticCFunction { userdata ->
        guardCallback("Callable.free") { userdata?.asStableRef<KotlinCallable>()?.dispose() }
    }

    /**
     * Hash trampoline - returns hash of the callable.
     */
    public val hashFunc: GDExtensionCallableCustomHash = staticCFunction { userdata ->
        guardCallback("Callable.hash", 0u) { userdata?.getInstance<KotlinCallable>()?.hashCode()?.toUInt() ?: 0u }
    }

    /**
     * Is valid trampoline - checks if callable is still valid.
     */
    public val isValidFunc: GDExtensionCallableCustomIsValid = staticCFunction { userdata ->
        guardCallback("Callable.is_valid", GDExtensionBool.FALSE) {
            val callable = userdata?.getInstance<KotlinCallable>()
            if (callable != null && callable.isValid()) GDExtensionBool.TRUE else GDExtensionBool.FALSE
        }
    }

    /**
     * Equal trampoline - checks equality with another callable.
     */
    public val equalFunc: GDExtensionCallableCustomEqual = staticCFunction { userdataA, userdataB ->
        guardCallback("Callable.equal", GDExtensionBool.FALSE) {
            val callableA = userdataA?.getInstance<KotlinCallable>()
            val callableB = userdataB?.getInstance<KotlinCallable>()
            if (callableA != null && callableB != null && callableA == callableB) {
                GDExtensionBool.TRUE
            } else {
                GDExtensionBool.FALSE
            }
        }
    }

    /**
     * Less than trampoline - comparison for sorting.
     */
    public val lessThanFunc: GDExtensionCallableCustomLessThan = staticCFunction { userdataA, userdataB ->
        guardCallback("Callable.less_than", GDExtensionBool.FALSE) {
            val callableA = userdataA?.getInstance<KotlinCallable>()
            val callableB = userdataB?.getInstance<KotlinCallable>()
            if (callableA != null && callableB != null && callableA.lessThan(callableB)) {
                GDExtensionBool.TRUE
            } else {
                GDExtensionBool.FALSE
            }
        }
    }

    /**
     * Argument count trampoline - returns the number of arguments.
     */
    public val getArgumentCountFunc: GDExtensionCallableCustomGetArgumentCount = staticCFunction { userdata, rIsValid ->
        if (rIsValid != null) rIsValid[0] = GDExtensionBool.FALSE
        guardCallback("Callable.get_argument_count", 0L) {
            val callable = userdata?.getInstance<KotlinCallable>()
            val arity = callable?.arity() ?: 0L
            if (rIsValid != null) {
                val validFlag = if (callable != null && callable.isValid()) GDExtensionBool.TRUE else GDExtensionBool.FALSE
                rIsValid[0] = validFlag
            }
            arity
        }
    }

    /**
     * To string trampoline - returns string representation for debugging.
     */
    public val toStringFunc: GDExtensionCallableCustomToString = staticCFunction { userdata, rIsValid, rReturnPtr ->
        if (rIsValid != null) rIsValid[0] = GDExtensionBool.FALSE
        guardCallback("Callable.to_string") {
            val callable = userdata?.getInstance<KotlinCallable>()
            val string = callable?.toString() ?: "Invalid Callable"
            StringBinding.newWithUtf8Chars(rReturnPtr, string)
            if (rIsValid != null && callable != null && callable.isValid()) rIsValid[0] = GDExtensionBool.TRUE
        }
    }
}
