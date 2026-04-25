package io.github.kingg22.godot.codegen.extensionapi.impl.knative.impl

import com.squareup.kotlinpoet.*
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.BodyGenerator
import io.github.kingg22.godot.codegen.types.cinteropAlloc
import io.github.kingg22.godot.codegen.types.cinteropPtr
import io.github.kingg22.godot.codegen.types.memScoped

class KNativeImplGen(private val body: BodyGenerator) {
    // Field classification
    sealed interface FieldKind {
        /** Scalar C primitives: bool, int*, uint*, float, double, real_t, ObjectID */
        data class Primitive(val kotlinType: TypeName) : FieldKind

        /** Raw C pointer — maps to COpaquePointer */
        data object OpaquePointer : FieldKind

        /**
         * Godot builtin class (RID, Vector2, Color, …).
         * [sizeBytes] is the builtin's physical size from extension_api.json.
         * [kotlinType] is the generated class ClassName.
         */
        data class Builtin(val kotlinType: TypeName, val sizeBytes: Int) : FieldKind

        /** Godot native struct (e.g. `ObjectID`). */
        data class NativeStruct(val kotlinType: TypeName, val sizeBytes: Int) : FieldKind

        /**
         * Godot enum (`enum::ClassName.EnumName`).
         * Stored as Long; read via `GodotEnum.fromValue<T>()`.
         */
        data class GodotEnumKind(val kotlinType: TypeName) : FieldKind

        /**
         * Godot bitfield (`bitfield::ClassName.FlagName`).
         * Stored as Long; read as `EnumMask<T>(value)`.
         * Setter uses `.value` (EnumMask implements GodotEnum).
         */
        data class BitfieldKind(val innerType: TypeName, val maskType: TypeName) : FieldKind

        /** Array fields or anything else not yet implemented. */
        data object Unimplemented : FieldKind
    }

    private lateinit var implPackageRegistry: ImplementationPackageRegistry

    fun initialize(implementationPackageRegistry: ImplementationPackageRegistry) {
        this.implPackageRegistry = implementationPackageRegistry
    }

    context(context: Context)
    fun generateOffsetGetter(kind: FieldKind, publicType: TypeName, offsetBytes: Int): FunSpec = when (kind) {
        // ── primitive scalar ──────────────────────────────────────────
        is FieldKind.Primitive -> {
            val fnName = primitiveGetterName(kind.kotlinType)
                ?: error("Missing primitive getter helper for $publicType")
            FunSpec
                .getterBuilder()
                .addStatement(
                    "return %M(storage, %L)",
                    implPackageRegistry.memberNameForOrDefault(fnName),
                    offsetBytes,
                )
                .build()
        }

        // ── raw pointer ───────────────────────────────────────────────
        FieldKind.OpaquePointer ->
            FunSpec
                .getterBuilder()
                .addStatement(
                    "return %M(storage, %L)",
                    implPackageRegistry.memberNameForOrDefault("getPointer"),
                    offsetBytes,
                )
                .build()

        // ── builtin class — memcpy into a fresh default instance ──────
        is FieldKind.Builtin ->
            FunSpec
                .getterBuilder()
                .addStatement("val result = %T()", kind.kotlinType)
                .addStatement(
                    "%M(storage, %L, result.rawPtr, %L)",
                    implPackageRegistry.memberNameForOrDefault("getBuiltin"),
                    offsetBytes,
                    kind.sizeBytes,
                )
                .addStatement("return result")
                .build()

        // ── native struct — memcpy into a fresh allocated instance ──────
        is FieldKind.NativeStruct ->
            FunSpec
                .getterBuilder()
                .beginControlFlow("return %M", memScoped)
                .addStatement("val result = %M<%T>()", cinteropAlloc, kind.kotlinType)
                .addStatement(
                    "%M(storage, %L, result.%M, %L)",
                    implPackageRegistry.memberNameForOrDefault("getBuiltin"),
                    offsetBytes,
                    cinteropPtr,
                    kind.sizeBytes,
                )
                .addStatement("result")
                .endControlFlow()
                .build()

        // ── enum — read Long, convert via GodotEnum.fromValue<T>() ────
        is FieldKind.GodotEnumKind -> {
            val godotEnumClass = context.classNameForOrDefault("GodotEnum")
            FunSpec
                .getterBuilder()
                .addStatement(
                    "return %T.fromValue<%T>(%M(storage, %L))",
                    godotEnumClass,
                    kind.kotlinType,
                    implPackageRegistry.memberNameForOrDefault("getLong"),
                    offsetBytes,
                )
                .build()
        }

        // ── bitfield — read Long, wrap in EnumMask<T> ────────────────
        is FieldKind.BitfieldKind ->
            FunSpec
                .getterBuilder()
                .addStatement(
                    "return %T(%M(storage, %L))",
                    kind.maskType,
                    implPackageRegistry.memberNameForOrDefault("getLong"),
                    offsetBytes,
                )
                .build()

        FieldKind.Unimplemented -> body.todoGetter("Unimplemented field type setter: $publicType")
    }

    fun generateOffsetSetter(kind: FieldKind, publicType: TypeName, offsetBytes: Int): FunSpec = when (kind) {
        // ── primitive scalar ──────────────────────────────────────────
        is FieldKind.Primitive -> {
            val fnName = primitiveSetterName(kind.kotlinType)
                ?: error("Missing primitive setter helper for $publicType")
            FunSpec
                .setterBuilder()
                .addParameter("value", publicType)
                .addStatement(
                    "%M(storage, %L, value)",
                    implPackageRegistry.memberNameForOrDefault(fnName),
                    offsetBytes,
                )
                .build()
        }

        // ── raw pointer ───────────────────────────────────────────────
        FieldKind.OpaquePointer ->
            FunSpec
                .setterBuilder()
                .addParameter("value", publicType)
                .addStatement(
                    "%M(storage, %L, value)",
                    implPackageRegistry.memberNameForOrDefault("setPointer"),
                    offsetBytes,
                )
                .build()

        // ── builtin class — memcpy from value.rawPtr into struct ──────
        is FieldKind.Builtin ->
            FunSpec
                .setterBuilder()
                .addParameter("value", publicType)
                .addStatement(
                    "%M(storage, %L, value.rawPtr, %L)",
                    implPackageRegistry.memberNameForOrDefault("setBuiltin"),
                    offsetBytes,
                    kind.sizeBytes,
                )
                .build()

        // ── builtin class — memcpy from value.ptr into struct ──────
        is FieldKind.NativeStruct ->
            FunSpec
                .setterBuilder()
                .addParameter("value", publicType)
                .addStatement(
                    "%M(storage, %L, value.%M, %L)",
                    implPackageRegistry.memberNameForOrDefault("setBuiltin"),
                    offsetBytes,
                    cinteropPtr,
                    kind.sizeBytes,
                )
                .build()

        // ── enum + bitfield — both are GodotEnum, use .value ──────────
        is FieldKind.GodotEnumKind,
        is FieldKind.BitfieldKind,
        -> FunSpec
            .setterBuilder()
            .addParameter("value", publicType)
            .addStatement(
                "%M(storage, %L, value.value)",
                implPackageRegistry.memberNameForOrDefault("setLong"),
                offsetBytes,
            )
            .build()

        FieldKind.Unimplemented ->
            FunSpec
                .setterBuilder()
                .addParameter("value", publicType)
                .addCode(body.todoBody("Unimplemented field type setter: $publicType"))
                .build()
    }

    // Primitive name helpers
    private fun primitiveGetterName(kotlinType: TypeName): String? = when (kotlinType) {
        BOOLEAN -> "getBoolean"
        BYTE -> "getByte"
        U_BYTE -> "getUByte"
        SHORT -> "getShort"
        U_SHORT -> "getUShort"
        INT -> "getInt"
        U_INT -> "getUInt"
        LONG -> "getLong"
        U_LONG -> "getULong"
        FLOAT -> "getFloat"
        DOUBLE -> "getDouble"
        else -> null
    }

    private fun primitiveSetterName(kotlinType: TypeName): String? = when (kotlinType) {
        BOOLEAN -> "setBoolean"
        BYTE -> "setByte"
        U_BYTE -> "setUByte"
        SHORT -> "setShort"
        U_SHORT -> "setUShort"
        INT -> "setInt"
        U_INT -> "setUInt"
        LONG -> "setLong"
        U_LONG -> "setULong"
        FLOAT -> "setFloat"
        DOUBLE -> "setDouble"
        else -> null
    }
}
