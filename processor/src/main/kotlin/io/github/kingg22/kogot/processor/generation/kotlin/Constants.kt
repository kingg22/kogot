package io.github.kingg22.kogot.processor.generation.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

private const val KOTLIN_PKG = "kotlin"
val K_SUPPRESS = ClassName(KOTLIN_PKG, "Suppress")
val K_OPT_IN = ClassName(KOTLIN_PKG, "OptIn")
private const val GODOT_PKG = "io.github.kingg22.godot"
private const val GODOT_INTERNAL_BINDING_PKG = "$GODOT_PKG.internal.binding"

val InternalBindingClassName = ClassName(GODOT_INTERNAL_BINDING_PKG, "InternalBinding")
private const val K_CINTEROP_PKG = "kotlinx.cinterop"
val ExperimentalForeignApi = ClassName(K_CINTEROP_PKG, "ExperimentalForeignApi")

val STATIC_C_FUNCTION = MemberName(K_CINTEROP_PKG, "staticCFunction")
val MEM_SCOPED = MemberName(K_CINTEROP_PKG, "memScoped")
val C_VALUE = MemberName(K_CINTEROP_PKG, "cValue")
val AS_STABLE_REF = MemberName(K_CINTEROP_PKG, "asStableRef")

val ObjectBindingClassName = ClassName(GODOT_INTERNAL_BINDING_PKG, "ObjectBinding")
val BindingProcAddressHolderClassName = ClassName(GODOT_INTERNAL_BINDING_PKG, "BindingProcAddressHolder")
val ClassDBBindingClassName = ClassName(GODOT_INTERNAL_BINDING_PKG, "ClassDBBinding")
val InstanceStorageClassName = ClassName(GODOT_INTERNAL_BINDING_PKG, "InstanceStorage")
val NodeVirtualDispatcherClassName = ClassName(GODOT_INTERNAL_BINDING_PKG, "NodeVirtualDispatcher")

private const val GODOT_INTERNAL_FFI_PKG = "$GODOT_PKG.internal.ffi"
val GDExtensionObjectPtrClassName = ClassName(GODOT_INTERNAL_FFI_PKG, "GDExtensionObjectPtr")
val GDExtensionInstanceBindingCallbacksClassName =
    ClassName(GODOT_INTERNAL_FFI_PKG, "GDExtensionInstanceBindingCallbacks")

val BindingInitializationCallbacksClassName =
    ClassName(GODOT_INTERNAL_BINDING_PKG, "BindingInitializationCallbacks")

val AS_STRING_NAME = MemberName("$GODOT_PKG.api.builtin", "asStringName")

val CREATE_FREE_INSTANCE_FUN = MemberName(GODOT_INTERNAL_BINDING_PKG, "createFreeInstanceFunc")

val REGISTER_CLASS = MemberName(GODOT_INTERNAL_BINDING_PKG, "registerClass")

val CREATE_INSTANCE_FUN = MemberName(GODOT_INTERNAL_BINDING_PKG, "createInstanceFunc")

val ClassDBClassName = ClassName("$GODOT_PKG.api.singleton", "ClassDB")
