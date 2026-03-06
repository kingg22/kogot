package io.github.kingg22.godot.codegen.impl.extensionapi.native

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.impl.extensionapi.EmptyContext
import io.github.kingg22.godot.codegen.models.extensionapi.MethodReturn
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.Executable

class KotlinNativeTypeResolverTest {
    private val resolver = KotlinNativeTypeResolver()
    private val testContext = EmptyContext()

    @Test
    fun `when each type is passed to resolve, then returns expected type`() {
        assertAll(
            TYPES_EXPECTED.map { (godotType, expected) ->
                val resolved = context(testContext) {
                    resolver.resolve(godotType)
                }

                Executable { assertEquals(expected, resolved, "Failed resolving $godotType") }
            },
        )
    }

    @Test
    fun `when 'required' is passed to resolve, then throws`() {
        // Si llega al resolver como tipo raw, debe lanzar excepción.
        val exception = assertThrows<IllegalStateException> {
            context(testContext) {
                resolver.resolve("required")
            }
        }

        assertTrue(exception.message?.startsWith("Unexpected 'required' type") == true)
    }

    @Test
    fun `when meta is required, then type is resolved`() {
        // ── "required" meta marker ────────────────────────────────────────────────
        // "required" aparece solo en TypeMetaHolder.meta, no como tipo base.
        val tested = MethodReturn(
            type = "String",
            meta = "required",
        )

        val resolved = context(testContext) {
            resolver.resolve(tested)
        }
        assertEquals(ClassName("", "GodotString"), resolved)
    }

    @Test
    fun `when meta is null, then type is resolved`() {
        // ── "required" meta marker ────────────────────────────────────────────────
        // "required" aparece solo en TypeMetaHolder.meta, no como tipo base.
        val tested = MethodReturn("String", null)

        val resolved = context(testContext) {
            resolver.resolve(tested)
        }
        assertEquals(ClassName("", "GodotString"), resolved)
    }
}

private val TYPES_EXPECTED = mapOf(
    // ── Primitivos ya testeados ───────────────────────────────────────────────
    "String" to ClassName("", "GodotString"),
    "float" to FLOAT,
    "int" to INT,
    "Variant" to ClassName("", "Variant"),
    "bool" to BOOLEAN,

    // ── Builtin classes → clases generadas ───────────────────────────────────
    "PackedStringArray" to ClassName("", "PackedStringArray"),
    "PackedFloat64Array" to ClassName("", "PackedFloat64Array"),
    "PackedByteArray" to ClassName("", "PackedByteArray"),
    "PackedInt32Array" to ClassName("", "PackedInt32Array"),
    "PackedInt64Array" to ClassName("", "PackedInt64Array"),
    "PackedFloat32Array" to ClassName("", "PackedFloat32Array"),
    "PackedVector2Array" to ClassName("", "PackedVector2Array"),
    "PackedVector3Array" to ClassName("", "PackedVector3Array"),
    "PackedVector4Array" to ClassName("", "PackedVector4Array"),
    "PackedColorArray" to ClassName("", "PackedColorArray"),
    "StringName" to ClassName("", "StringName"),
    "Callable" to ClassName("", "Callable"),
    "Object" to ClassName("", "GodotObject"), // rename: Object colisiona con java.lang.Object
    "Array" to ClassName("", "VariantArray"), // rename: Array colisiona con kotlin.Array y este es untyped
    "Dictionary" to ClassName("", "VariantDictionary"), // Alias para Dictionary<Variant, Variant> untyped
    "RID" to ClassName("", "Rid"),
    "Range" to ClassName("", "GodotRange"), // rename: colisiona con kotlin.ranges.IntRange etc

    // ── Enums / bitfields → clases generadas ─────────────────────────────────
    "enum::Error" to ClassName("", "GodotError"),
    "enum::Corner" to ClassName("", "Corner"),
    "enum::Theme.DataType" to ClassName("", "Theme", "DataType"),
    "enum::IP.WAAD" to ClassName("", "Ip", "Waad"),
    "bitfield::TextServerTextOverrunFlag" to LONG, // TODO generate bitfield/enum mask value class typed enum

    // ── Meta numeric types ────────────────────────────────────────────────────
    "int64" to LONG,
    "int32" to INT,
    "int16" to SHORT,
    "int8" to BYTE,
    "uint64" to U_LONG,
    "uint32" to U_INT,
    "uint16" to U_SHORT,
    "uint8" to U_BYTE,
    "double" to DOUBLE,
    "char32" to U_INT,

    // ── typedarray ────────────────────────────────────────────────────────────

    "typedarray::Dictionary" to ClassName("", "GodotArray")
        .parameterizedBy(ClassName("", "VariantDictionary")),
    "typedarray::Vector2i" to ClassName("", "GodotArray")
        .parameterizedBy(ClassName("", "Vector2i")),
    "typedarray::String" to ClassName("", "GodotArray")
        .parameterizedBy(ClassName("", "GodotString")),
    "typedarray::int" to ClassName("", "GodotArray")
        .parameterizedBy(INT),
    "typedarray::RegExMatch" to ClassName("", "GodotArray")
        .parameterizedBy(ClassName("", "RegExMatch")),
    "typedarray::RDPipelineSpecializationConstant" to ClassName("", "GodotArray")
        .parameterizedBy(ClassName("", "RDPipelineSpecializationConstant")),

    // typeddictionary
    "typeddictionary::int;String" to ClassName("", "Dictionary")
        .parameterizedBy(INT, ClassName("", "GodotString")),
    "typeddictionary::Color;Color" to ClassName("", "Dictionary")
        .parameterizedBy(ClassName("", "Color"), ClassName("", "Color")),

    // ── Pointers ──────────────────────────────────────────────────────────────
    "void*" to COPAQUE_POINTER,
    "const void*" to COPAQUE_POINTER,
    "const Glyph*" to COPAQUE_POINTER,
    "AudioFrame*" to COPAQUE_POINTER,
    "float*" to C_POINTER.parameterizedBy(FLOAT_VAR),
    "int32_t*" to C_POINTER.parameterizedBy(INT_VAR),
    "uint8_t*" to C_POINTER.parameterizedBy(U_BYTE_VAR),
    "const uint8_t*" to C_POINTER.parameterizedBy(U_BYTE_VAR),
    // const uint8_t** → CPointer<CPointerVarOf<CPointer<UByteVar>>>
    "const uint8_t **" to COPAQUE_POINTER,
    "CaretInfo*" to COPAQUE_POINTER,
)
