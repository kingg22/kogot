package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.impl.KNativeImplGen
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.impl.KNativeImplGen.FieldKind
import io.github.kingg22.godot.codegen.extensionapi.resolver.NativeStructureParser.NativeStructureField
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.impl.sanitizeTypeName
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedNativeStructure
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import io.github.kingg22.godot.codegen.types.BYTE_VAR
import io.github.kingg22.godot.codegen.types.COPAQUE_POINTER
import io.github.kingg22.godot.codegen.types.C_POINTER
import io.github.kingg22.godot.codegen.types.C_STRUCT_VAR
import io.github.kingg22.godot.codegen.types.C_STRUCT_VAR_TYPE
import io.github.kingg22.godot.codegen.types.INTERPRET_C_POINTER
import io.github.kingg22.godot.codegen.types.K_SUPPRESS
import io.github.kingg22.godot.codegen.utils.error
import io.github.kingg22.godot.codegen.utils.logger

private val primitiveKotlinTypes = setOf(BOOLEAN, BYTE, U_BYTE, SHORT, U_SHORT, INT, U_INT, LONG, U_LONG, FLOAT, DOUBLE)

class KNativeStructureGenerator(private val typeResolver: TypeResolver, private val bodyImpl: KNativeImplGen) {
    private val logger = logger()
    private data class FieldLayout(val name: String, val offset: Int, val size: Int, val align: Int)
    private data class StructLayout(val size: Int, val align: Int, val fields: List<FieldLayout>)

    context(context: Context, _: GDExtensionInterface?)
    fun generateFile(ns: ResolvedNativeStructure): FileSpec? {
        val spec = generateSpec(ns) ?: return null
        return createFile(spec, spec.name!!, context.packageForOrDefault(spec.name!!))
    }

    context(_: Context, _: GDExtensionInterface?)
    fun generateSpec(ns: ResolvedNativeStructure): TypeSpec? {
        val fields = ns.fields

        // Determinar si la estructura puede ser CStructVar pura
        val canBeCStructVar = canGenerateAsCStructVar(fields)

        return if (canBeCStructVar) {
            // ESTRATEGIA 1: CStructVar (compatible con cinterop)
            if (ns.name == "ObjectID" || ns.name == "AudioFrame") return null
            error(
                "Generating CStructVar for ${ns.name} with fields: ${fields.joinToString { it.name }}, " +
                    "must be defined in header file",
            )
        } else {
            generateOpaqueWrapper(ns, fields)
        }
    }

    // ESTRATEGIA 2: Wrapper Opaco (para estructuras con tipos de API)
    context(context: Context, _: GDExtensionInterface?)
    private fun generateOpaqueWrapper(ns: ResolvedNativeStructure, fields: List<NativeStructureField>): TypeSpec {
        val className = sanitizeTypeName(ns.name.renameGodotClass())
        val layout = calculateStructLayout(context, ns.name, fields)

        return TypeSpec
            .classBuilder(className)
            .experimentalApiAnnotation(ns.name)
            .addKdoc(
                "Native structure wrapper\n\nThis structure contains references to generated API types.",
            )
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter("handle", COPAQUE_POINTER)
                    .build(),
            )
            .superclass(C_STRUCT_VAR)
            .addSuperclassConstructorParameter("handle.rawValue")
            .addProperty(
                PropertySpec
                    .builder("storage", C_POINTER.parameterizedBy(BYTE_VAR), KModifier.PRIVATE)
                    .getter(
                        FunSpec
                            .getterBuilder()
                            .addStatement(
                                "return %M(rawPtr) ?: error(%S)",
                                INTERPRET_C_POINTER,
                                "Null raw pointer for native struct storage",
                            )
                            .build(),
                    )
                    .build(),
            )
            .addProperties(
                fields.map { field ->
                    val type = typeResolver.resolve(field.type)
                    val resolvedType = if (field.arraySize != null) {
                        context.classNameForOrDefault("Array", typedClass = true).parameterizedBy(type)
                    } else {
                        type
                    }
                    val fieldLayout = layout.fields.first { it.name == field.name }

                    val kind = classifyField(field, resolvedType, fieldLayout.size)

                    // ── signatures (property type visible to users) ───────────
                    val publicType = fieldPublicType(kind, resolvedType)

                    // ── bodies ────────────────────────────────────────────────
                    val getter = bodyImpl.generateOffsetGetter(kind, publicType, fieldLayout.offset)
                    val setter = bodyImpl.generateOffsetSetter(kind, publicType, fieldLayout.offset)

                    val kotlinName = safeIdentifier(field.name)

                    PropertySpec
                        .builder(kotlinName, publicType)
                        .mutable(true)
                        .getter(getter)
                        .setter(setter)
                        .apply {
                            if (kotlinName != field.name) addKdoc("Original name: `%S`", field.name)

                            if (field.arraySize != null) {
                                addKdoc("\n\nArray size: %L, type: `%T`", field.arraySize!!, type)
                            } else {
                                addKdoc("\n\nOriginal type: `%S`", field.type)
                            }

                            addKdoc(
                                "\n\nLayout: offset=%L, size=%L, align=%L",
                                fieldLayout.offset,
                                fieldLayout.size,
                                fieldLayout.align,
                            )

                            if (field.defaultValue != null) {
                                addKdoc("\n\nDefault value: `%L`", field.defaultValue!!)
                            }
                        }
                        .build()
                },
            )
            .addType(
                TypeSpec
                    .companionObjectBuilder()
                    .superclass(C_STRUCT_VAR_TYPE)
                    .addSuperclassConstructorParameter("size = %L, align = %L", layout.size, layout.align)
                    .addAnnotation(
                        AnnotationSpec
                            .builder(K_SUPPRESS)
                            .addMember("%S", "DEPRECATION")
                            .build(),
                    )
                    .build(),
            )
            .build()
    }

    // HELPERS
    context(_: Context, _: GDExtensionInterface?)
    private fun canGenerateAsCStructVar(fields: List<NativeStructureField>): Boolean = fields.all { field ->
        val type = field.type.removePrefix("const ").trim().removeSuffix("*").trim()

        fun isPrimitiveCType(type: String): Boolean = type.lowercase() in setOf(
            "int8_t", "uint8_t", "int16_t", "uint16_t",
            "int32_t", "uint32_t", "int64_t", "uint64_t",
            "float", "double", "bool",
            "char", "short", "int", "long",
            "size_t", "intptr_t", "uintptr_t",
        )

        fun isOpaquePointer(type: String): Boolean = type == "void" ||
            (type.startsWith("GDExtension") && type.endsWith("Ptr")) ||
            type.endsWith("*")

        // Solo permitir primitivos, punteros opacos y tipos nativos conocidos
        isPrimitiveCType(type) ||
            isOpaquePointer(type) ||
            isKnownNativeType(type)
    }

    context(ctx: Context, extensionInterface: GDExtensionInterface?)
    private fun isKnownNativeType(type: String): Boolean {
        // Tipos que sabemos que son seguros para CStructVar
        // (otros structs nativos que ya fueron generados)
        return ctx.nativeStructureTypes.contains(type) ||
            extensionInterface?.types?.any { it.name == type } == true ||
            extensionInterface?.interfaces?.any { it.name == type || it.legacyTypeName == type } == true
    }

    context(context: Context, _: GDExtensionInterface?)
    private fun classifyField(field: NativeStructureField, resolvedType: TypeName, fieldSizeBytes: Int): FieldKind {
        val clean = field.type.removePrefix("const ").trim()

        // Raw pointer
        if (clean.endsWith("*")) return FieldKind.OpaquePointer

        // Enum / bitfield — check prefix on the raw type string
        if (clean.startsWith("enum::")) {
            return FieldKind.GodotEnumKind(resolvedType)
        }

        if (clean.startsWith("bitfield::")) {
            check(resolvedType is ParameterizedTypeName) {
                "Bitfield type must be parameterized of EnumMask: $resolvedType"
            }
            return FieldKind.BitfieldKind(innerType = resolvedType.rawType, maskType = resolvedType)
        }

        // Scalar C primitives
        if (resolvedType in primitiveKotlinTypes) return FieldKind.Primitive(resolvedType)

        // Godot builtin class — has a known layout size in extension_api.json
        if (context.isBuiltin(clean) || field.arraySize != null) {
            return FieldKind.Builtin(kotlinType = resolvedType, sizeBytes = fieldSizeBytes)
        }

        if (isKnownNativeType(clean)) return FieldKind.NativeStruct(resolvedType, fieldSizeBytes)

        logger.error { "Unknown native struct field type '$clean' in ${field.name}" }
        return FieldKind.Unimplemented
    }

    private fun calculateStructLayout(
        context: Context,
        structName: String,
        fields: List<NativeStructureField>,
    ): StructLayout {
        fun alignUp(value: Int, align: Int): Int {
            if (align <= 1) return value
            val rem = value % align
            return if (rem == 0) value else value + (align - rem)
        }

        fun pointerLayout(): Pair<Int, Int> = 8 to 8 // Kotlin/Native targets for Godot are 64-bit today.

        fun primitiveLayout(type: String): Pair<Int, Int>? = when (type) {
            "bool" -> 1 to 1
            "char" -> 1 to 1
            "int8_t" -> 1 to 1
            "uint8_t" -> 1 to 1
            "int16_t" -> 2 to 2
            "uint16_t" -> 2 to 2
            "short" -> 2 to 2
            "int32_t" -> 4 to 4
            "uint32_t" -> 4 to 4
            "int" -> 4 to 4
            "float" -> 4 to 4
            "int64_t" -> 8 to 8
            "uint64_t" -> 8 to 8
            "double" -> 8 to 8
            "real_t" -> 8 to 8
            "ObjectID" -> 8 to 8
            else -> null
        }

        fun lookupLayout(type: String): Pair<Int, Int> {
            val clean = type.removePrefix("const ").trim()
            if (clean.endsWith("*")) return pointerLayout()

            primitiveLayout(clean)?.let { return it }

            if (clean.startsWith("enum::") || clean.startsWith("bitfield::")) {
                return 8 to 8
            }

            // Builtins: exact name in extension_api.json (e.g. "Vector2", "RID", "StringName")
            context.model.builtins.firstOrNull { it.name == clean }?.layout?.let { layout ->
                val size = layout.size
                val align = when {
                    size <= 1 -> 1
                    size <= 2 -> 2
                    size <= 4 -> 4
                    else -> 8
                }
                return size to align
            }

            // Nested native structures (value fields).
            context.model.nativeStructures.firstOrNull { it.name == clean }?.let { nested ->
                val nestedFields = nested.fields
                val nestedLayout = calculateStructLayout(context, nested.name, nestedFields)
                return nestedLayout.size to nestedLayout.align
            }

            // Fallback: treat as pointer to keep generator running (but warn loudly).
            logger.error { "Unknown native struct field type '$clean' in $structName; treating as pointer." }
            return pointerLayout()
        }

        var offset = 0
        var structAlign = 1
        val out = ArrayList<FieldLayout>(fields.size)

        for (f in fields) {
            val clean = f.type.removePrefix("const ").trim()
            val (elemSize, fieldAlign) = lookupLayout(clean)

            val fieldSize = if (f.arraySize != null) elemSize * f.arraySize!! else elemSize
            structAlign = maxOf(structAlign, fieldAlign)

            offset = alignUp(offset, fieldAlign)
            out += FieldLayout(name = f.name, offset = offset, size = fieldSize, align = fieldAlign)
            offset += fieldSize
        }

        val size = alignUp(offset, structAlign)
        return StructLayout(size = size, align = structAlign, fields = out)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Signatures — public property type visible to callers
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /** Returns the Kotlin [TypeName] that should appear in the property declaration. */
    private fun fieldPublicType(kind: FieldKind, fallback: TypeName): TypeName = when (kind) {
        is FieldKind.Primitive -> kind.kotlinType
        is FieldKind.OpaquePointer -> COPAQUE_POINTER
        is FieldKind.Builtin -> kind.kotlinType
        is FieldKind.NativeStruct -> kind.kotlinType
        is FieldKind.GodotEnumKind -> kind.kotlinType
        is FieldKind.BitfieldKind -> kind.maskType
        FieldKind.Unimplemented -> fallback
    }
}
