package io.github.kingg22.godot.sample

import io.github.kingg22.godot.api.builtin.Color
import io.github.kingg22.godot.api.builtin.Vector2i
import io.github.kingg22.godot.api.builtin.Vector3
import io.github.kingg22.godot.api.builtin.asVariantString
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.internal.binding.getDouble
import io.github.kingg22.godot.internal.binding.getFloat
import io.github.kingg22.godot.internal.binding.getInt
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.reinterpret

/**
 * Runtime layout verification.
 *
 * Allocates each builtin with nativeHeap using the codegen-resolved size+align,
 * then writes and reads back known field values via member offsets.
 * If alignment is wrong, the read-back values will be garbage or Godot will crash.
 *
 * Run with: godot --headless --path <project>
 * Exit code 0 = all tests passed, 1 = failures.
 */
fun testBuiltinLayouts(): Boolean {
    var passed = true

    // Vector3: float_64 -> size=24, align=4 (float fields)
    // We write x=1.0, y=2.0, z=3.0 and read them back via member offsets
    fun testVector3(): Boolean {
        val v = Vector3(1.0, 2.0, 3.0)
        val base = v.rawPtr.reinterpret<ByteVar>()
        // Member offsets from JSON: x=0, y=4, z=8 (float_32) or x=0, y=8, z=16 (double_64)
        // The codegen embeds the correct offsets per build config
        val x = getDouble(base, Vector3.OFFSET_X)
        val y = getDouble(base, Vector3.OFFSET_Y)
        val z = getDouble(base, Vector3.OFFSET_Z)
        val ok = x == 1.0 && y == 2.0 && z == 3.0
        if (!ok) GD.print("FAIL testVector3: expected (1,2,3) got ($x,$y,$z)".asVariantString())
        return ok
    }

    // Vector2i: int32 fields, align=4 always
    fun testVector2i(): Boolean {
        val v = Vector2i(10, 20)
        val base = v.rawPtr.reinterpret<ByteVar>()
        val x = getInt(base, Vector2i.OFFSET_X)
        val y = getInt(base, Vector2i.OFFSET_Y)
        val ok = x == 10 && y == 20
        if (!ok) GD.print("FAIL testVector2i: expected (10,20) got ($x,$y)".asVariantString())
        return ok
    }

    // Color: always float, align=4
    fun testColor(): Boolean {
        val c = Color(0.5, 0.25, 0.75, 1.0)
        val base = c.rawPtr.reinterpret<ByteVar>()
        val r = getDouble(base, Color.OFFSET_R)
        val g = getDouble(base, Color.OFFSET_G)
        val b = getDouble(base, Color.OFFSET_B)
        val a = getDouble(base, Color.OFFSET_A)
        val ok = r == 0.5 && g == 0.25 && b == 0.75 && a == 1.0
        if (!ok) GD.print("FAIL testColor: expected (0.5,0.25,0.75,1.0) got ($r,$g,$b,$a)".asVariantString())
        return ok
    }

    passed = passed and testVector3()
    passed = passed and testVector2i()
    passed = passed and testColor()

    GD.print(
        if (passed) {
            "testBuiltinLayouts: ALL PASSED".asVariantString()
        } else {
            "testBuiltinLayouts: FAILURES DETECTED".asVariantString()
        },
    )
    return passed
}
