@file:OptIn(InternalBinding::class)

package io.github.kingg22.godot.test

import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.ffi.*
import io.github.kingg22.godot.test.generated.GeneratedBindings
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.pointed
import kotlinx.cinterop.staticCFunction

private val callbacks = StableRef.create(GeneratedBindings())

@Suppress("unused")
@CName("kogot_test_init")
fun kogotTestInit(
    pGetProcAddress: GDExtensionInterfaceGetProcAddress,
    pLibrary: GDExtensionClassLibraryPtr,
    initialization: CPointer<GDExtensionInitialization>,
): GDExtensionBool {
    BindingProcAddressHolder.initialize(pGetProcAddress, pLibrary)
    val init = initialization.pointed
    init.initialize = staticCFunction { userdata, level -> callbacks.get().initialize(userdata, level) }
    init.deinitialize = staticCFunction { userdata, level -> callbacks.get().deinitialize(userdata, level) }
    init.userdata = null
    init.minimum_initialization_level = GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_SCENE
    return GDExtensionBool.TRUE
}
