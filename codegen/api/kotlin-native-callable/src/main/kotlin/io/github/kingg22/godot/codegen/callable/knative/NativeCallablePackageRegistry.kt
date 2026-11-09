package io.github.kingg22.godot.codegen.callable.knative

import io.github.kingg22.godot.codegen.services.PackageRegistry

/**
 * Packages the typed `Callable { ... }` factories and `TypedSignal` extensions reference, derived from the API root
 * package (`<root>.api...`).
 */
class NativeCallablePackageRegistry(override val rootPackage: String) : PackageRegistry {
    private val packages = buildMap {
        val builtin = "$rootPackage.api.builtin"
        put("Callable", builtin)
        put("Variant", builtin)
        put("MustBeVariant", builtin)
        put("getValue", builtin)
        put("toVariant", builtin)
        put("GodotError", "$rootPackage.api")
        put("GodotObject", "$rootPackage.api.core")
        put("TypedSignal", "$rootPackage.api.signal")
        put("SignalConnection", "$rootPackage.api.signal")
        put("CallableFactory", "$rootPackage.api.internal.callable")
        put("InternalBinding", "$rootPackage.internal.binding")
    }

    override fun packageFor(godotName: String): String? = packages[godotName]
}
