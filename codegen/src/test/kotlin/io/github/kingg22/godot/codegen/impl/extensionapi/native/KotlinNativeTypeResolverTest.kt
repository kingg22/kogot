package io.github.kingg22.godot.codegen.impl.extensionapi.native

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.impl.extensionapi.EmptyContext
import io.github.kingg22.godot.codegen.models.extensionapi.TypeMetaHolder
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

                Executable { assertEquals(expected, resolved) }
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
        val tested = object : TypeMetaHolder {
            override val type: String = "String"
            override val meta: String = "required"
        }

        val resolved = context(testContext) {
            resolver.resolve(tested)
        }
        assertEquals(ClassName("", "GodotString"), resolved)
    }

    @Test
    fun `when meta is null, then type is resolved`() {
        // ── "required" meta marker ────────────────────────────────────────────────
        // "required" aparece solo en TypeMetaHolder.meta, no como tipo base.
        val tested = object : TypeMetaHolder {
            override val type: String = "String"
            override val meta: String? = null
        }

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
    "Array" to ClassName("", "GodotArray"), // rename: Array colisiona con kotlin.Array
    "Dictionary" to ClassName("", "Dictionary"),
    "RID" to ClassName("", "Rid"),
    "Range" to ClassName("", "GodotRange"), // rename: colisiona con kotlin.ranges.IntRange etc

    // ── Enums / bitfields → clases generadas ─────────────────────────────────
    "enum::Error" to ClassName("", "GodotError"),
    "enum::Corner" to ClassName("", "Corner"),
    "enum::Theme.DataType" to ClassName("", "ThemeDataType"),
    "enum::IP.WAAD" to ClassName("", "IpWaad"),
    "bitfield::TextServerTextOverrunFlag" to ClassName("", "TextServerTextOverrunFlag"),

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
    "typedarray::Dictionary" to KotlinNativeTypeResolver.C_POINTER
        .parameterizedBy(ClassName("", "GodotArray"))
        .copy(true),

    /*
    // FIXME idiomatic flow
    "typedarray::Dictionary" to LIST.parameterizedBy(ClassName("", "Dictionary")),
    "typedarray::Vector2i" to LIST.parameterizedBy(ClassName("", "Vector2i")),
    "typedarray::String" to LIST.parameterizedBy(ClassName("", "GodotString")),
    "typedarray::int" to LIST.parameterizedBy(INT),
    "typedarray::RegExMatch" to LIST.parameterizedBy(ClassName("", "RegExMatch")),
    "typedarray::RDPipelineSpecializationConstant" to
        LIST.parameterizedBy(ClassName("", "RDPipelineSpecializationConstant")),
     */

    // ── Pointers ──────────────────────────────────────────────────────────────
    "void*" to KotlinNativeTypeResolver.COPAQUE_POINTER.copy(nullable = true),
    "const void*" to KotlinNativeTypeResolver.COPAQUE_POINTER.copy(nullable = true),
    "const Glyph*" to KotlinNativeTypeResolver.C_POINTER
        .parameterizedBy(ClassName("", "Glyph"))
        .copy(nullable = true),
    "AudioFrame*" to KotlinNativeTypeResolver.C_POINTER
        .parameterizedBy(ClassName("", "AudioFrame"))
        .copy(nullable = true),
    "float*" to KotlinNativeTypeResolver.C_POINTER
        .parameterizedBy(KotlinNativeTypeResolver.FLOAT_VAR)
        .copy(nullable = true),
    "int32_t*" to KotlinNativeTypeResolver.C_POINTER
        .parameterizedBy(KotlinNativeTypeResolver.INT_VAR)
        .copy(nullable = true),
    "uint8_t*" to KotlinNativeTypeResolver.C_POINTER
        .parameterizedBy(KotlinNativeTypeResolver.U_BYTE_VAR)
        .copy(nullable = true),
    "const uint8_t*" to KotlinNativeTypeResolver.C_POINTER
        .parameterizedBy(KotlinNativeTypeResolver.U_BYTE_VAR)
        .copy(nullable = true),
    // const uint8_t** → CPointer<CPointerVarOf<CPointer<UByteVar>>>
    "const uint8_t **" to KotlinNativeTypeResolver.C_POINTER
        .parameterizedBy(
            KotlinNativeTypeResolver.C_POINTER_VAR_OF
                .parameterizedBy(
                    KotlinNativeTypeResolver.C_POINTER
                        .parameterizedBy(KotlinNativeTypeResolver.U_BYTE_VAR)
                        .copy(nullable = true),
                ),
        )
        .copy(nullable = true),
    "CaretInfo*" to KotlinNativeTypeResolver.C_POINTER
        .parameterizedBy(ClassName("", "CaretInfo"))
        .copy(nullable = true),
)
