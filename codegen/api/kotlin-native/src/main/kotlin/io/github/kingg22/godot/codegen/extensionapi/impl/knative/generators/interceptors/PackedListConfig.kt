package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.interceptors

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.GenericBuiltinInterceptor

/** `Packed*Array` as `AbstractMutableList<E>` + `RandomAccess`: not generic, the element type is fixed. */
class PackedListConfig(private val element: String) : GenericBuiltinInterceptor.GenericConfig {
    context(context: Context)
    override val listElementType: TypeName get() = packedElementType(element)

    context(context: Context)
    override val superclass: TypeName
        get() = ClassName("kotlin.collections", "AbstractMutableList").parameterizedBy(listElementType)

    // Abstract members of AbstractMutableList that the engine methods implement; `size` is bridged by hand.
    override val viewMethods: Map<String, String> = mapOf("is_empty" to "isEmpty", "clear" to "clear")

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
