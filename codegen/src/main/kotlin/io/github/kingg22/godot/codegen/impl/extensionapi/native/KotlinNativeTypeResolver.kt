package io.github.kingg22.godot.codegen.impl.extensionapi.native

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.impl.checkAndNormalizeTypeName
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.PackageRegistry
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.sanitizeTypeName
import io.github.kingg22.godot.codegen.models.extensionapi.TypeMetaHolder

/**
 * TypeResolver for Kotlin Native (cinterop) backend.
 *
 * Maps Godot/C types → KotlinPoet TypeNames suitable for use with kotlinx.cinterop.
 *
 * ## Pointer strategy
 * - Opaque handle types (GDExtensionObjectPtr, etc.) → `COpaquePointer` (nullable)
 * - Typed pointers to known Godot classes → `CPointer<T>` where T is the generated class
 * - Primitive pointers (int*, float*, etc.) → `CPointer<IntVar>`, `CPointer<FloatVar>`, etc.
 * - void* → `COpaquePointer`
 * - const X* → same as X* (const is not expressible in Kotlin Native types directly)
 * - typedarray::X → `CPointer<GodotArray>` (pointer to array) (nullable) // FIXME can be idiomatic?
 *
 * ## Numeric mapping (C → Kotlin Native)
 * | C                         | Kotlin        |
 * |---------------------------|---------------|
 * | char / int8_t             | Byte          |
 * | unsigned char / uint8_t   | UByte         |
 * | short / int16_t           | Short         |
 * | unsigned short / uint16_t | UShort        |
 * | int / int32_t             | Int           |
 * | unsigned int / uint32_t   | UInt          |
 * | long long / int64_t       | Long          |
 * | unsigned long long / uint64_t | ULong     |
 * | float                     | Float         |
 * | double                    | Double        |
 * | char16_t                  | UShort (char16_t = uint16_t in cinterop) |
 * | char32_t                  | UInt   (char32_t = uint32_t in cinterop) |
 * | size_t                    | ULong  (64-bit target assumption)         |
 * | intptr_t                  | Long                                      |
 * | uintptr_t                 | ULong                                     |
 */
class KotlinNativeTypeResolver : TypeResolver {
    context(_: PackageRegistry)
    fun resolveOf(holder: TypeMetaHolder): TypeName {
        // meta hint: use it to pick a more precise primitive
        // e.g. meta = "int32" on a Variant int → Int instead of Long
        if (!holder.isRequired()) {
            runCatching { resolveWithMeta(holder.type, holder.meta!!) }
                .onSuccess { return it }
        }
        return resolveOf(holder.type)
    }

    context(_: PackageRegistry)
    fun resolveOf(godotType: String): TypeName {
        val stripped = godotType.trim().removePrefix("const ").trim()

        // Pointer type: ends with * after stripping const
        if (stripped.endsWith("*")) {
            return resolvePointer(stripped)
        }

        return resolvePlain(stripped)
    }

    context(context: Context)
    override fun resolve(holder: TypeMetaHolder): TypeName = context(context) {
        resolveOf(holder)
    }

    context(context: Context)
    override fun resolve(godotType: String): TypeName = context(context) {
        resolveOf(godotType)
    }

    // ── Pointer resolution ────────────────────────────────────────────────────

    context(_: PackageRegistry)
    private fun resolvePointer(type: String): TypeName {
        // Strip one level of pointer
        val inner = type.removeSuffix("*").trim().removePrefix("const ").trim()

        return when {
            // void* or opaque handles → COpaquePointer
            inner == "void" || isOpaqueHandle(inner) -> COPAQUE_POINTER.copy(nullable = true)

            // Multi-level pointers (e.g. Object**) → CPointer<CPointerVar<T>>
            inner.endsWith("*") -> {
                val innerType = resolvePointer(inner)
                C_POINTER.parameterizedBy(toCPointerVarOf(innerType)).copy(nullable = true)
            }

            // Primitive pointer → CPointer<XVar> (e.g. int* → CPointer<IntVar>)
            isPrimitive(inner) -> {
                val varType = primitiveToVarType(inner)
                C_POINTER.parameterizedBy(varType).copy(nullable = true)
            }

            // Known generated class → CPointer<GeneratedClass>
            else -> {
                val innerTypeName = resolvePlain(inner)
                C_POINTER.parameterizedBy(innerTypeName).copy(nullable = true)
            }
        }
    }

    /**
     * True for Godot's opaque handle typedefs (GDExtensionObjectPtr, etc.).
     * These are all aliases for `void*` in C, so they map to COpaquePointer.
     */
    private fun isOpaqueHandle(type: String): Boolean {
        val clean = type.removePrefix("const ").trim()
        return (clean.startsWith("GDExtension") && clean.endsWith("Ptr")) ||
            clean == "GDObjectInstanceID" ||
            clean == "GDExtensionScriptInstanceDataPtr" ||
            clean == "GDExtensionScriptLanguagePtr"
    }

    // ── Plain (non-pointer) type resolution ───────────────────────────────────

    context(context: PackageRegistry)
    private fun resolvePlain(type: String): TypeName {
        var clean = type.removePrefix("const ").trim()
            .removePrefix("enum::").trim()
            .removePrefix("bitfield::").trim()

        if (clean.startsWith("typedarray::")) {
            // typedarray in native → CPointer<GodotArray> or similar;
            // for now map to the inner type pointer to stay ABI-accurate
            // val inner = resolveOf(clean.removePrefix("typedarray::"))
            val godotArrayClass = context.classNameForOrDefault("Array", "GodotArray")
            return C_POINTER.parameterizedBy(godotArrayClass).copy(nullable = true)
        }

        if (clean.contains('.')) {
            val parentType = clean.substringBeforeLast('.')
            val nestedType = clean.substringAfterLast('.')
            clean = checkAndNormalizeTypeName(parentType).renameGodotClass() + "." +
                checkAndNormalizeTypeName(nestedType).renameGodotClass()
        }

        val normalized = checkAndNormalizeTypeName(clean).renameGodotClass()

        return when (normalized.lowercase()) {
            // void
            "void" -> UNIT

            // Boolean
            "bool", "boolean" -> BOOLEAN

            // Signed integers
            "char", "int8_t", "int8" -> BYTE

            "short", "int16_t", "int16" -> SHORT

            "int", "int32_t", "int32" -> INT

            "long long", "int64_t", "int64", "long", "intptr_t" -> LONG

            // Unsigned integers
            "uchar", "unsigned char", "uint8_t", "uint8" -> U_BYTE

            "unsigned short", "ushort", "uint16_t", "uint16", "char16_t" -> U_SHORT

            "unsigned int", "uint", "uint32_t", "uint32", "char32_t", "char32" -> U_INT

            "unsigned long long", "ulong", "uint64_t", "uint64",
            "uintptr_t", "size_t",
            -> U_LONG

            // Floating point
            "float" -> FLOAT

            "double" -> DOUBLE

            // String → generated GodotString wrapper
            "string" -> context.classNameForOrDefault("String")

            "required" -> error("Unexpected 'required' type: $type")

            // Everything else → generated class in the target package
            else -> {
                val normalizedList = normalized.split(".").map { sanitizeTypeName(it) }.toTypedArray()
                context.classNameForOrDefault(normalized, *normalizedList)
            }
        }
    }

    // ── Meta-driven resolution ─────────────────────────────────────────────────

    /**
     * Uses Godot's `meta` hint to pick a more precise Kotlin primitive.
     * Only called when meta is non-null and the type is not marked required.
     */
    context(_: PackageRegistry)
    private fun resolveWithMeta(baseType: String, meta: String): TypeName = when (meta.lowercase()) {
        "int8" -> BYTE
        "int16" -> SHORT
        "int32" -> INT
        "int64" -> LONG
        "uint8" -> U_BYTE
        "uint16" -> U_SHORT
        "uint32" -> U_INT
        "uint64" -> U_LONG
        "float" -> FLOAT
        "double" -> DOUBLE
        "char16" -> U_SHORT
        "char32" -> U_INT
        else -> resolveOf(baseType) // unknown meta → fall back
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isPrimitive(type: String): Boolean {
        val clean = type.lowercase().removePrefix("const ").trim()
        return clean in PRIMITIVE_TYPES
    }

    private fun primitiveToVarType(type: String): TypeName {
        check(isPrimitive(type)) { "Not a primitive: $type" }
        val clean = type.lowercase().removePrefix("const ").trim()
        return when (clean) {
            "char", "int8_t", "int8" -> BYTE_VAR

            "short", "int16_t", "int16" -> SHORT_VAR

            "int", "int32_t", "int32" -> INT_VAR

            "long long", "int64_t", "int64", "long",
            "intptr_t",
            -> LONG_VAR

            "uchar", "unsigned char", "uint8_t", "uint8",
            -> U_BYTE_VAR

            "unsigned short", "ushort", "uint16_t", "uint16",
            "char16_t",
            -> U_SHORT_VAR

            "unsigned int", "uint", "uint32_t", "uint32",
            "char32_t",
            -> U_INT_VAR

            "unsigned long long", "ulong", "uint64_t", "uint64",
            "uintptr_t", "size_t",
            -> U_LONG_VAR

            "float" -> FLOAT_VAR

            "double" -> DOUBLE_VAR

            // fallback
            else -> error("Unknown primitive: $type")
        }
    }

    /** Wraps a TypeName in CPointerVarOf<T> for double-pointer scenarios. */
    private fun toCPointerVarOf(inner: TypeName): TypeName = C_POINTER_VAR_OF.parameterizedBy(inner)

    // ── TypeName constants ────────────────────────────────────────────────────

    companion object {
        val COPAQUE_POINTER = ClassName("kotlinx.cinterop", "COpaquePointer")
        val C_POINTER = ClassName("kotlinx.cinterop", "CPointer")
        val C_POINTER_VAR_OF = ClassName("kotlinx.cinterop", "CPointerVarOf")

        // CVar types (used for pointer targets)
        val BYTE_VAR = ClassName("kotlinx.cinterop", "ByteVar")
        val SHORT_VAR = ClassName("kotlinx.cinterop", "ShortVar")
        val INT_VAR = ClassName("kotlinx.cinterop", "IntVar")
        val LONG_VAR = ClassName("kotlinx.cinterop", "LongVar")
        val U_BYTE_VAR = ClassName("kotlinx.cinterop", "UByteVar")
        val U_SHORT_VAR = ClassName("kotlinx.cinterop", "UShortVar")
        val U_INT_VAR = ClassName("kotlinx.cinterop", "UIntVar")
        val U_LONG_VAR = ClassName("kotlinx.cinterop", "ULongVar")
        val FLOAT_VAR = ClassName("kotlinx.cinterop", "FloatVar")
        val DOUBLE_VAR = ClassName("kotlinx.cinterop", "DoubleVar")

        private val PRIMITIVE_TYPES = setOf(
            "char", "int8_t", "int8",
            "short", "int16_t", "int16",
            "int", "int32_t", "int32",
            "long long", "int64_t", "int64", "long", "intptr_t",
            "uchar", "unsigned char", "uint8_t", "uint8",
            "unsigned short", "ushort", "uint16_t", "uint16", "char16_t",
            "unsigned int", "uint", "uint32_t", "uint32", "char32_t",
            "unsigned long long", "ulong", "uint64_t", "uint64",
            "uintptr_t", "size_t",
            "float", "double",
        )
    }
}
