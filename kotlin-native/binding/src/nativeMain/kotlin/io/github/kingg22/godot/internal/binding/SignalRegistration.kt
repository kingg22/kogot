package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.PropertyUsageFlags
import io.github.kingg22.godot.api.annotations.RegisterSignal
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.internal.toGDE
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.toEnumMask
import io.github.kingg22.godot.internal.ffi.GDExtensionPropertyInfo
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped

/**
 * Registers [signal] on the extension class [className].
 *
 * Each argument follows Godot's own `PropertyInfo` defaults: `class_name` is empty unless the argument is an object
 * of a known class, `hint_string` is a `String` and an empty usage means [PropertyUsageFlags.DEFAULT].
 */
@InternalBinding
public fun registerCustomSignal(className: String, signal: RegisterSignal) {
    className.toStringName().use { classNameSn ->
        signal.name.toStringName().use { signalName ->
            withTransientStringNames { recordName ->
                withTransientGodotStrings { recordString ->
                    memScoped {
                        val argumentsInfo = allocArray<GDExtensionPropertyInfo>(signal.params.size) { index ->
                            val param = signal.params[index]
                            this.type = param.type.toGDE()
                            this.name = recordName(param.name)
                            this.class_name = recordName(if (param.type == Variant.Type.OBJECT) param.className else "")
                            this.hint = param.hint.value.toUInt()
                            this.hint_string = recordString(param.hintString)
                            this.usage = param.usages
                                .ifEmpty { arrayOf(PropertyUsageFlags.DEFAULT) }
                                .toEnumMask()
                                .value
                                .toUInt()
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
}
