@file:OptIn(ExperimentalForeignApi::class)

import io.github.kingg22.godot.api.GodotEnum
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.annotations.RegisterSignal
import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Signal
import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.asStringName
import io.github.kingg22.godot.api.builtin.asVariant
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.node.Node2D
import io.github.kingg22.godot.api.global.GodotError
import io.github.kingg22.godot.internal.binding.ClassDBBinding
import io.github.kingg22.godot.internal.binding.ObjectBinding
import io.github.kingg22.godot.internal.binding.VariantBinding
import io.github.kingg22.godot.internal.binding.allocConstTypePtrArray
import io.github.kingg22.godot.internal.binding.checkCallError
import io.github.kingg22.godot.internal.ffi.GDExtensionMethodBindPtr
import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtrVar
import io.github.kingg22.godot.internal.ffi.GDExtensionPtrBuiltInMethod
import io.github.kingg22.godot.internal.ffi.GDExtensionPtrConstructor
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value

@Godot class SpriteBench(nativePtr: COpaquePointer) : Node2D(nativePtr) {

    private val hintStr = "hint".asStringName()
    private val punchStr = "punch".asStringName()

    @RegisterSignal
    private lateinit var hint: Signal

    @RegisterSignal(RegisterSignal.Param(Variant.Type.INT, "value"))
    private lateinit var punch: Signal

    private val callable1 = Callable {
        println("Callable1: received")
    }

    private val callable2 = Callable { id: Long ->
        println("Callable2: received $id")
    }

    override fun _ready() {
        println("[SpriteBench] _ready started")
        hint = signalFix(this, hintStr)
        punch = signalFix(this, punchStr)

        // ✅ connect después de validar
        if (hint.isConnected(callable1)) {
            println("hint already connected to callable1")
        } else {
            val result1 = hint.connect(callable1)
            println("connect result: $result1 : ${GodotEnum.fromValue<GodotError>(result1)}")
        }

        if (punch.isConnected(callable2)) {
            println("punch already connected to callable2")
        } else {
            val result = punch.connect(callable2)
            println("connect punch result: $result : ${GodotEnum.fromValue<GodotError>(result)}")
        }

        // ✅ emit usando Signal directamente
        try {
            println("[SpriteBench] emitting **hint** via Signal.emit fixed")
            val error = hint.emitFix()
            println("emitSignalFix hint returned: $error")

            println("[SpriteBench] emitting **punch** via Signal.emit fixed")
            val error2 = punch.emitFix(12.asVariant())
            println("emitSignalFix punch returned: $error2")

            /* ✅ emit usando Object.emitSignal FIX
            println("[SpriteBench] emitting punch via emitSignalFix")
            val err = emitSignalFix(punchStr, 200.asVariant())
            println("emitSignalFix punch returned: $err")
             */
        } catch (e: Exception) {
            println("emitSignalFix failed: ${e.message}")
        } finally {
            println("[SpriteBench] _ready finished")
        }
    }
}

fun signalFix(obj: GodotObject, signal: StringName) = Signal(null).apply {
    memScoped {
        val objectPtr = alloc<GDExtensionObjectPtrVar>()
        objectPtr.`value` = obj.rawPtr
        constructorFptr_2.invoke(
            rawPtr,
            allocConstTypePtrArray(
                objectPtr.ptr,
                signal.rawPtr,
            ),
        )
    }
}

private val constructorFptr_2: GDExtensionPtrConstructor by lazy(PUBLICATION) {
    VariantBinding.instance.getPtrConstructorRaw(GDEXTENSION_VARIANT_TYPE_SIGNAL, 2)
        ?: error("Missing builtin constructor for Signal[2]")
}

fun Signal.emitFix(vararg args: Variant): GodotError = memScoped {
    val retPtr = alloc<LongVar>()
    methodSignalEmit_3286317445_Fn.invoke(
        rawPtr,
        allocConstTypePtrArray(*args.map { it.rawPtr }.toTypedArray()),
        retPtr.ptr,
        args.size,
    )
    return GodotEnum.fromValue(retPtr.value)
}

private val methodSignalEmit_3286317445_Fn: GDExtensionPtrBuiltInMethod by lazy(PUBLICATION) {
    StringName("emit").use { name ->
        VariantBinding.instance.getPtrBuiltinMethodRaw(GDEXTENSION_VARIANT_TYPE_SIGNAL, name.rawPtr, 3_286_317_445L)
            ?: error("Missing builtin method 'Signal.emit' hash: 3286317445")
    }
}

// **have errors**
fun GodotObject.emitSignalFix(signal: StringName, vararg args: Variant): GodotError = memScoped {
    val retPtr = alloc<LongVar>()

    val callError = ObjectBinding.instance.methodBindCall(
        methodObjectEmitSignal_Bind,
        rawPtr,
        signal.asVariant().rawPtr,
        *args.map { it.rawPtr }.toTypedArray(),
        rRet = retPtr.ptr,
    )

    checkCallError("emitSignal of $signal", callError)

    return GodotEnum.fromValue(retPtr.value)
}

private val methodObjectEmitSignal_Bind: GDExtensionMethodBindPtr by lazy(PUBLICATION) {
    StringName("Object").use { cn ->
        StringName("emit_signal").use { mn ->
            ClassDBBinding.instance.getMethodBindRaw(cn.rawPtr, mn.rawPtr, 4_047_867_050L)
                ?: error("Missing method bind 'Object.emit_signal', hash: 4_047_867_050")
        }
    }
}
