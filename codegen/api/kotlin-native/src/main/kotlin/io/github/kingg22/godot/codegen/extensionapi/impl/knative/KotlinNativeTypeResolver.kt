package io.github.kingg22.godot.codegen.extensionapi.impl.knative

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.checkAndNormalizeTypeName
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.sanitizeTypeName
import io.github.kingg22.godot.codegen.types.*

private val PRIMITIVE_TYPES = PRIMITIVE_NUMERIC_TYPES + setOf(
    "char", "int8_t", "int8",
    "short", "int16_t", "int16",
    "int", "int32_t", "int32",
    "long long", "int64_t", "int64", "long", "intptr_t",
    "uchar", "unsigned char", "uint8_t", "uint8",
    "unsigned short", "ushort", "uint16_t", "uint16", "char16_t",
    "unsigned int", "uint", "uint32_t", "uint32", "char32_t",
    "unsigned long long", "ulong", "uint64_t", "uint64",
    "uintptr_t", "size_t",
)

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
 * - typedarray::X → `Array<X>` (parameterized generic type)
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

    context(ctx: Context)
    override fun resolve(godotType: String, metaType: String?): TypeName {
        if (metaType != null && metaType != "required") {
            runCatching { resolveWithMeta(godotType, metaType) }
                .onFailure {
                    println(
                        "ERROR: failed to resolve type with meta: $godotType ($metaType).\n${it.stackTraceToString()}",
                    )
                }
                .onSuccess { return it }
        }

        val stripped = godotType.trim().removePrefix("const ").trim()

        // Pointer type: ends with * after stripping const
        if (stripped.endsWith("*")) {
            return resolvePointer(stripped)
        }

        return resolvePlain(stripped)
    }

    // ── Pointer resolution ────────────────────────────────────────────────────

    context(context: Context)
    private fun resolvePointer(type: String): TypeName {
        // Strip one level of pointer
        val inner = type.removeSuffix("*").trim().removePrefix("const ").trim()

        return when {
            // Special case of FFI
            inner == "GDExtensionInitializationFunction" -> context.classNameForOrDefault(
                "GDExtensionInitializationFunction",
            )

            // 1. void* y handles GDExtension → COpaquePointer?
            inner == "void" || isOpaqueHandle(inner) -> COPAQUE_POINTER

            // 2. Multi-level pointers (e.g. uint_8**) → CPointer<CPointerVar<T>>
            inner.endsWith("*") -> {
                val innerType = resolvePointer(inner.removeSuffix("*"))
                C_POINTER.parameterizedBy(C_POINTER_VAR_OF.parameterizedBy(innerType))
            }

            // 3. Primitivos → CPointer<XVar> (e.g. int* → CPointer<IntVar>)
            isPrimitive(inner) -> {
                val varType = primitiveToVarType(inner)
                C_POINTER.parameterizedBy(varType)
            }

            // 4. Estructuras C nativas (definidas en .def) → CPointer<T>?
            isNativeStruct(inner) -> {
                val structType = resolveNativeStruct(inner)
                C_POINTER.parameterizedBy(structType)
            }

            // 5. Clases Godot (builtin o engine) → COpaquePointer?
            // NO USAR CPointer<GeneratedClass> porque no heredan CPointed
            else -> COPAQUE_POINTER
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

    context(context: Context)
    private fun resolvePlain(type: String): TypeName {
        val clean = type.removePrefix("const ").trim()

        if (clean.startsWith("bitfield::")) {
            // bitfield::EnumName → EnumMask<EnumName>
            val enumTypeStr = clean.removePrefix("bitfield::")
            val enumTypeName = resolve("enum::$enumTypeStr")
            val enumMaskClass = context.classNameForOrDefault("EnumMask")
            return enumMaskClass.parameterizedBy(enumTypeName)
        }

        if (clean.startsWith("typedarray::")) {
            // typedarray::Node → Array<Node>
            val innerType = clean.removePrefix("typedarray::")
            val godotArrayClass = context.classNameForOrDefault("Array", "GodotArray", typedClass = true)

            // Resolver el tipo interno
            val innerTypeName = resolve(innerType)

            return godotArrayClass.parameterizedBy(innerTypeName)
        }

        if (clean.startsWith("typeddictionary")) {
            // typeddictionary::KeyType:ValueType → Dictionary<KeyType, ValueType>
            // Ejemplo: typeddictionary::int:String → Dictionary<Int, GodotString>
            val innerPart = clean.removePrefix("typeddictionary::")

            // Split por ':' para obtener K y V
            val parts = innerPart.split(",", ";", limit = 2)
            if (parts.size != 2) {
                error(
                    "Invalid typeddictionary format: $clean, expected 2 types separated by ',' or ';', got ${parts.size}, raw: $innerPart.",
                )
            }

            val keyTypeStr = parts[0].trim()
            val valueTypeStr = parts[1].trim()

            // Resolver tipos (pueden ser primitivos o clases)
            val keyType = resolve(keyTypeStr)

            val valueType = resolve(valueTypeStr)

            val godotDictClass = context.classNameForOrDefault("Dictionary", typedClass = true)

            return godotDictClass.parameterizedBy(keyType, valueType)
        }

        // Nested enum class handler
        if (clean.startsWith("enum::") && clean.contains('.')) {
            val clean = clean.removePrefix("enum::")
            val parentType = clean.substringBeforeLast('.')
            val nestedType = clean.substringAfterLast('.')
            val parentRaw = checkAndNormalizeTypeName(parentType)
            val nestedRaw = checkAndNormalizeTypeName(nestedType)
            val rawQualified = "$parentRaw.$nestedRaw"
            val parentName = sanitizeTypeName(parentRaw.renameGodotClass())
            val nestedName = sanitizeTypeName(nestedRaw.renameGodotClass())

            // Cases like: Vector2i is a specialized of Vector2, so we need to check if the parent is a Godot class
            if (parentName.endsWith('i') && context.isGodotType(parentName.dropLast(1))) {
                val baseParentName = parentName.dropLast(1)
                val qualifiedBase = "$baseParentName$nestedName"
                return context.classNameForOrDefault(qualifiedBase, baseParentName, nestedName)
            }

            return context.classNameForOrDefault(rawQualified, parentName, nestedName)
        }

        if (clean.startsWith("enum::")) {
            val rawName = checkAndNormalizeTypeName(clean.removePrefix("enum::"))
            val kotlinName = sanitizeTypeName(rawName.renameGodotClass())
            return context.classNameForOrDefault(rawName, kotlinName)
        }

        val raw = checkAndNormalizeTypeName(clean)

        return when (raw.lowercase()) {
            // void
            "void" -> UNIT

            // Boolean
            "bool", "boolean" -> BOOLEAN

            // Signed integers
            "char", "int8_t", "int8" -> BYTE

            "short", "int16_t", "int16" -> SHORT

            "int32_t", "int32" -> INT

            // Godot int = int64
            "int", "long long", "int64_t", "int64", "long", "intptr_t" -> LONG

            // Unsigned integers
            "uchar", "unsigned char", "uint8_t", "uint8" -> U_BYTE

            "unsigned short", "ushort", "uint16_t", "uint16", "char16_t" -> U_SHORT

            "unsigned int", "uint", "uint32_t", "uint32", "char32_t", "char32" -> U_INT

            "unsigned long long", "ulong", "uint64_t", "uint64",
            "uintptr_t", "size_t",
            -> U_LONG

            // Floating point
            // Godot float = double
            "float" -> DOUBLE

            "double" -> DOUBLE

            // Basado en el header de la API
            "real_t" -> if (context.isDoublePrecision) DOUBLE else FLOAT

            // String → generated GodotString wrapper
            "string" -> context.classNameForOrDefault("String")

            "required" -> error("Unexpected 'required' type: $type")

            // Everything else → generated class in the target package
            else -> {
                val kotlinName = sanitizeTypeName(raw.renameGodotClass())
                context.classNameForOrDefault(raw, kotlinName)
            }
        }
    }

    // ── Meta-driven resolution ─────────────────────────────────────────────────

    /**
     * Uses Godot's `meta` hint to pick a more precise Kotlin primitive.
     * Only called when meta is non-null and the type is not marked required.
     */
    context(ctx: Context)
    private fun resolveWithMeta(baseType: String, meta: String): TypeName = when (meta.lowercase()) {
        "int8" -> BYTE

        "int16" -> SHORT

        "int32" -> INT

        "int64" -> LONG

        "uint8" -> U_BYTE

        "uint16" -> U_SHORT

        "uint32" -> U_INT

        "uint64" -> U_LONG

        "float" -> if (ctx.isDoublePrecision) DOUBLE else FLOAT

        "double" -> DOUBLE

        "char16" -> U_SHORT

        "char32" -> U_INT

        else -> {
            // unknown meta → fall back
            // FIXME: enable with logger.debug/verbose
            // println("WARNING: Unknown meta type: '$meta', fallback to type: '$baseType'")
            resolve(baseType)
        }
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

            "long long", "int64_t", "int64", "long", "intptr_t" -> LONG_VAR

            "uchar", "unsigned char", "uint8_t", "uint8" -> U_BYTE_VAR

            "unsigned short", "ushort", "uint16_t", "uint16", "char16_t" -> U_SHORT_VAR

            "unsigned int", "uint", "uint32_t", "uint32", "char32_t" -> U_INT_VAR

            "unsigned long long", "ulong", "uint64_t", "uint64", "uintptr_t", "size_t" -> U_LONG_VAR

            // "float" in builtin_class_member_offsets always means C float (32-bit) = kotlin.Float.
            "float" -> FLOAT_VAR

            // "double" (and the plain-type path for GDScript float) maps to kotlin.Double.
            "double" -> DOUBLE_VAR

            // fallback
            else -> error("Unknown primitive: $type")
        }
    }

    context(context: Context)
    private fun isNativeStruct(type: String): Boolean {
        // Buscar en las estructuras nativas del contexto
        return context.nativeStructureTypes.contains(type)
    }

    context(context: Context)
    private fun resolveNativeStruct(type: String): TypeName {
        val ns = context.nativeStructureTypes.find { it == type } ?: error("Unknown native struct: $type")
        val className = sanitizeTypeName(ns.renameGodotClass())
        return context.classNameForOrDefault(ns, className)
    }
}
