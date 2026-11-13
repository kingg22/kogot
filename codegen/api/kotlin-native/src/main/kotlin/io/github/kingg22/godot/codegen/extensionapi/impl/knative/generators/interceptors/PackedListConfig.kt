package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.interceptors

import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.GenericBuiltinInterceptor

/** `Packed*Array` as a `MutableList<E>` through `GodotPackedList<E>`: not generic, the element type is fixed. */
class PackedListConfig(private val element: String) : GenericBuiltinInterceptor.GenericConfig {
    context(context: Context)
    override val superclass: TypeName
        get() = collectionBase("GodotPackedList", packedElementType(element))

    // The engine methods are the abstract members of `GodotPackedList`: they only need `override`.
    override val viewMethods: Map<String, String> = mapOf(
        "size" to "size",
        "is_empty" to "isEmpty",
        "clear" to "clear",
        "get" to "get",
        "set" to "set",
        "insert" to "insert",
        "remove_at" to "removeAt",
    )

    // `get` and `set` have String overloads (GodotString and String): the one that matches the element type overrides.
    context(context: Context)
    override fun viewSignature(engineName: String): GenericBuiltinInterceptor.ViewSignature? {
        val elementType = packedElementType(element)
        return when (engineName) {
            "get" -> GenericBuiltinInterceptor.ViewSignature(listOf(LONG), elementType)
            "set", "insert" -> GenericBuiltinInterceptor.ViewSignature(listOf(LONG, elementType), null)
            else -> null
        }
    }

    companion object {

        /** `Packed*Array` name → the element type its `get` returns. */
        val PACKED_ELEMENTS = mapOf(
            "PackedByteArray" to "Long",
            "PackedInt32Array" to "Long",
            "PackedInt64Array" to "Long",
            "PackedFloat32Array" to "Double",
            "PackedFloat64Array" to "Double",
            "PackedStringArray" to "String",
            "PackedVector2Array" to "Vector2",
            "PackedVector3Array" to "Vector3",
            "PackedColorArray" to "Color",
            "PackedVector4Array" to "Vector4",
        )
    }
}

context(context: Context)
private fun packedElementType(name: String): TypeName = when (name) {
    "Long" -> LONG
    "Double" -> DOUBLE
    "String" -> STRING
    else -> context.classNameForOrDefault(name)
}
