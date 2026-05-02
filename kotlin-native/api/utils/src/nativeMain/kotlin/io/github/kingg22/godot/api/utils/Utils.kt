@file:Suppress("NOTHING_TO_INLINE")

package io.github.kingg22.godot.api.utils

import io.github.kingg22.godot.api.builtin.GodotString
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.asVariantString
import io.github.kingg22.godot.api.global.GodotError

public inline fun GD.typeString(type: Variant.Type): GodotString = typeString(type.value)

public inline fun GD.errorString(error: GodotError): GodotString = errorString(error.value)

public inline fun GD.print(arg1: String, vararg args: String) {
    print(arg1.asVariantString(), *args.map { it.asVariantString() }.toTypedArray())
}

public inline fun GD.printRich(arg1: String, vararg args: String) {
    printRich(arg1.asVariantString(), *args.map { it.asVariantString() }.toTypedArray())
}

public inline fun GD.printerr(arg1: String, vararg args: String) {
    printerr(arg1.asVariantString(), *args.map { it.asVariantString() }.toTypedArray())
}

public inline fun GD.printt(arg1: String, vararg args: String) {
    printt(arg1.asVariantString(), *args.map { it.asVariantString() }.toTypedArray())
}

public inline fun GD.prints(arg1: String, vararg args: String) {
    prints(arg1.asVariantString(), *args.map { it.asVariantString() }.toTypedArray())
}

public inline fun GD.printraw(arg1: String, vararg args: String) {
    printraw(arg1.asVariantString(), *args.map { it.asVariantString() }.toTypedArray())
}

public inline fun GD.printVerbose(arg1: String, vararg args: String) {
    printVerbose(arg1.asVariantString(), *args.map { it.asVariantString() }.toTypedArray())
}

public inline fun GD.pushError(arg1: String, vararg args: String) {
    pushError(arg1.asVariantString(), *args.map { it.asVariantString() }.toTypedArray())
}

public inline fun GD.pushWarning(arg1: String, vararg args: String) {
    pushWarning(arg1.asVariantString(), *args.map { it.asVariantString() }.toTypedArray())
}
