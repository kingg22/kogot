@file:OptIn(ExperimentalForeignApi::class)

import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.internal.binding.ClassDBBinding
import io.github.kingg22.godot.internal.binding.ObjectBinding
import io.github.kingg22.godot.internal.binding.allocGdBool
import io.github.kingg22.godot.internal.ffi.GDExtensionConstTypePtrVar
import io.github.kingg22.godot.internal.ffi.GDExtensionMethodBindPtr
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.value

fun Node.addChildTest(
    node: Node,
    forceReadableName: Boolean = false,
    internal: Node.InternalMode = Node.InternalMode.DISABLED,
) {
    memScoped {
        // 1. Node* → necesitamos Node**
        val nodeVar = alloc<COpaquePointerVar>()
        nodeVar.value = node.rawPtr

        // 2. bool → puntero a bool
        val forceReadableNameVar = allocGdBool(forceReadableName)

        // 3. enum (int64) → puntero a int64
        val internalVar = alloc<LongVar>()
        internalVar.value = internal.value

        // 4. array de argumentos: TODOS son punteros a los valores
        val args = allocArray<GDExtensionConstTypePtrVar>(3)
        args[0] = nodeVar.ptr // Node**
        args[1] = forceReadableNameVar // bool*
        args[2] = internalVar.ptr // int64*

        // 5. llamada
        ObjectBinding.instance.methodBindPtrcallRaw(
            methodNodeAddChild_Bind,
            this@addChildTest.rawPtr,
            args,
            null,
        )
    }
}

private val methodNodeAddChild_Bind: GDExtensionMethodBindPtr by lazy(PUBLICATION) {
    StringName("Node").use { cn ->
        StringName("add_child").use { mn ->
            ClassDBBinding.instance.getMethodBindRaw(cn.rawPtr, mn.rawPtr, 3_863_233_950L)
                ?: error("Missing method bind 'Node.add_child' hash:3863233950")
        }
    }
}
