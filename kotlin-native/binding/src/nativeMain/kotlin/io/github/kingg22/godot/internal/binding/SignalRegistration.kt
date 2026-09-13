package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.annotations.RegisterSignal
import io.github.kingg22.godot.api.builtin.internal.toGDE
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.toEnumMask
import io.github.kingg22.godot.internal.ffi.GDExtensionPropertyInfo
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped

@InternalBinding
public fun registerCustomSignal(className: String, signal: RegisterSignal) {
    className.toStringName().use { classNameSn ->
        signal.name.toStringName().use { signalName ->
            withTransientStringNames { record ->
                memScoped {
                    val argumentsInfo = allocArray<GDExtensionPropertyInfo>(signal.params.size) { index ->
                        val param = signal.params[index]
                        this.type = param.type.toGDE()
                        this.name = record(param.name)
                        this.class_name = classNameSn.rawPtr
                        this.hint = param.hints.toEnumMask().value.toUInt()
                        this.hint_string = record(param.hintString)
                        this.usage = param.usages.toEnumMask().value.toUInt()
                    }
                    ClassDBBinding.registerExtensionClassSignalRaw(
                        BindingProcAddressHolder.library,
                        classNameSn.rawPtr,
                        signalName.rawPtr,
                        argumentsInfo,
                        signal.params.size.toLong(),
                    )
                }
            }
        }
    }
}
