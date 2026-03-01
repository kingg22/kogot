package io.github.kingg22.godot.codegen.impl.extensionapi.stubs

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.impl.checkAndNormalizeTypeName
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.sanitizeTypeName
import io.github.kingg22.godot.codegen.models.extensionapi.TypeMetaHolder

/**
 * [io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver] for Kotlin stubs, doesn't resolve pointers, typed arrays, etc.
 *
 * Maps Godot types → Kotlin/KotlinPoet TypeNames.
 *
 * Godot numeric conventions:
 * - `int`   = 64-bit signed → [Long]
 * - `float` = 64-bit IEEE 754 → [Double]  (note: differs from Kotlin's Float)
 * - `String` = UTF-32 → mapped to generated `GodotString`, not [String]
 *
 * @param packageName Base package for generated engine/builtin classes (e.g. `"godot"`).
 */
class KotlinStubTypeResolver(private val packageName: String) : TypeResolver {
    init {
        println("WARNING: using stub type resolver, not resolving pointers, typed arrays, etc.")
    }

    override fun resolve(holder: TypeMetaHolder): TypeName {
        // char32 meta is currently skipped → base type
        if (holder.meta == "char32") return resolve(holder.type)
        return super.resolve(holder)
    }

    override fun resolve(godotType: String): TypeName {
        var type = godotType.trim().removePrefix("const ").trim()

        // Strip pointer suffixes (e.g. "Object*")
        while (type.endsWith('*')) type = type.removeSuffix("*").trim()

        if (type.startsWith("typedarray::")) {
            val inner = resolve(type.removePrefix("typedarray::"))
            return LIST.parameterizedBy(inner)
        }

        // bitfield → Long (typed EnumMask value class is a future improvement)
        if (type.startsWith("bitfield::")) return LONG

        // Strip enum:: prefix, the class name underneath is what we need
        type = type.removePrefix("enum::")

        if (type.contains('.')) {
            val parentType = type.substringBeforeLast('.')
            val nestedType = type.substringAfterLast('.')
            type = checkAndNormalizeTypeName(parentType).renameGodotClass() + "." +
                checkAndNormalizeTypeName(nestedType).renameGodotClass()
        }

        val normalized = checkAndNormalizeTypeName(type).renameGodotClass()

        return when (normalized.lowercase()) {
            "void" -> UNIT
            "bool", "boolean" -> BOOLEAN
            "float" -> FLOAT
            "double" -> DOUBLE
            "int8_t", "int8", "byte" -> BYTE
            "uint8_t", "uint8", "ubyte" -> U_BYTE
            "int16_t", "int16", "short" -> SHORT
            "uint16_t", "uint16", "ushort" -> U_SHORT
            "int32_t", "int32", "int" -> INT
            "uint32_t", "uint32", "uint" -> U_INT
            "int64_t", "int64", "long", "intptr_t" -> LONG
            "uint64_t", "uint64", "ulong", "uintptr_t", "size_t" -> U_LONG
            "string" -> ClassName(packageName, "GodotString")
            else -> ClassName(packageName, normalized.split(".").map { sanitizeTypeName(it) })
        }
    }
}
