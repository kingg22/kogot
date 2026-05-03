package io.github.kingg22.godot.codegen.callable.knative

import io.github.kingg22.godot.codegen.services.PackageRegistry

class NativeCallablePackageRegistry(override val rootPackage: String) : PackageRegistry {
    private val internalBindingPackage = run {
        val godotPackage = rootPackage.substringBefore("godot.")
        "$godotPackage.godot.internal.binding"
    }
    private val internalCallablePackage = if (rootPackage.endsWith("internal.callable")) {
        rootPackage
    } else {
        "$rootPackage.internal.callback"
    }

    private val packages = buildMap {
        put("InternalBinding", internalBindingPackage)
        put("KotlinCallable", internalCallablePackage)
    }

    override fun packageFor(godotName: String): String? = packages[godotName] ?: run {
        if (godotName.startsWith("Callable")) packages["KotlinCallable"] else null
    }
}
