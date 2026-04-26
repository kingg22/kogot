package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.*
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.utils.logger
import io.github.kingg22.godot.codegen.utils.warning
import io.github.kingg22.godot.codegen.utils.withExceptionContext

private val TYPED_ARRAY_REGEX = Regex("""Array\[[\w:]+]\(.*\)""")
private val NUMERIC_LITERAL_REGEX = Regex("""-?\d+(\.\d+)?([eE][+-]?\d+)?""", RegexOption.IGNORE_CASE)
private val EMPTY_ARRAY_REGEX = Regex("""Packed\w+Array\(\)""")
private val CLASS_CONSTRUCTOR_REGEX = Regex("""[A-Z][a-zA-Z0-9]*\(.*\)""")
private val ENUM_CONSTANT_REGEX = Regex("""[A-Z_][A-Z0-9_]*""")

/**
 * Mapeos especiales para constructores que Godot serializa flat
 * pero Kotlin espera agrupados.
 */
private val SPECIAL_CONSTRUCTOR_MAPPINGS = mapOf(
    // Transform3D: 12 floats → 4 Vector3
    "Transform3D" to ConstructorMapper(
        rawArgCount = 12,
        groupingSize = 3,
        groupType = "Vector3",
    ),

    // Transform2D: 6 floats → 3 Vector2
    "Transform2D" to ConstructorMapper(
        rawArgCount = 6,
        groupingSize = 2,
        groupType = "Vector2",
    ),

    // Basis: 9 floats → 3 Vector3
    "Basis" to ConstructorMapper(
        rawArgCount = 9,
        groupingSize = 3,
        groupType = "Vector3",
    ),

    // Projection: 16 floats → 4 Vector4
    "Projection" to ConstructorMapper(
        rawArgCount = 16,
        groupingSize = 4,
        groupType = "Vector4",
    ),
)

private data class ConstructorMapper(val rawArgCount: Int, val groupingSize: Int, val groupType: String)

/**
 * Generates default value expressions for function parameters.
 *
 * Converts Godot default values (strings from JSON) into valid Kotlin code.
 *
 * ## Supported patterns:
 * - `null` → `null`
 * - `true`, `false` → `true`, `false`
 * - Numeric literals: `0`, `1.0`, `0u`, `1.5f`
 * - String literals: `""`, `"text"`
 * - Empty collections: `[]`, `{}`
 * - Constructor calls: `Vector2(0, 0)`, `Color(1, 1, 1, 1)`
 * - Enum constants: `SIDE_LEFT`, `HORIZONTAL`
 * - Bitfield combinations: `FLAG_A | FLAG_B`
 * - Special cases: `nil` → `Variant.NIL`
 */
class DefaultValueGenerator(private val typeResolver: TypeResolver) {
    private val logger = logger()
    private val cache = LinkedHashMap<Triple<String, String, TypeName>, CodeBlock>(2048)

    context(_: Context)
    fun generate(defaultValue: String, godotType: String, resolvedType: TypeName): CodeBlock? {
        val defaultValue = defaultValue.trim()
        if (defaultValue.isBlank()) return null
        return cache.getOrPut(Triple(defaultValue, godotType, resolvedType)) {
            withExceptionContext({ "parse default value '$defaultValue' for type $resolvedType" }) {
                parseDefaultValue(defaultValue, resolvedType, godotType) ?: return null
            }
        }
    }

    context(_: Context)
    fun generate(argument: MethodArg, resolvedType: TypeName): CodeBlock? {
        val defaultValue = argument.defaultValue?.trim()
        if (defaultValue.isNullOrBlank()) return null
        return generate(defaultValue, argument.type, resolvedType)
    }

    context(context: Context)
    private fun parseDefaultValue(value: String, kotlinType: TypeName?, godotType: String): CodeBlock? = when {
        // Infinity
        value.equals("inf", ignoreCase = true) -> parseInfinityConstant(value, godotType, kotlinType)

        // NaN
        value.equals("nan", ignoreCase = true) -> parseNaNConstant(godotType, kotlinType)

        // nil → Variant.NIL (object singleton)
        godotType == "Variant" && (value == "nil" || value == "null") ->
            CodeBlock.of("%T()", context.classNameForOrDefault("Variant"))

        // Boolean
        value == "true" || value == "false" -> CodeBlock.of(value)

        // null
        value == "null" -> CodeBlock.of("null")

        // NodePath literals: ^"path" → NodePath("path")
        (value.startsWith("^\"") || (value.startsWith('"') && godotType == "NodePath")) &&
            value.endsWith('"') -> parseNodePathLiteral(value)

        // StringName literals: &"text" → StringName("text")
        (value.startsWith("&\"") || (value.startsWith('"') && godotType == "StringName")) &&
            value.endsWith('"') -> parseStringNameLiteral(value)

        // String literal
        value.startsWith('"') && value.endsWith('"') && godotType == "String" -> parseStringLiteral(value)

        // TypedArray literals: Array[RID]([]) → Array()
        isTypedArrayLiteral(value) -> parseTypedArrayLiteral(value)

        // Si kotlinType es null, parseNumericValue lo manejará vía inferencia básica o fallbacks
        isNumericLiteral(value) -> parseNumericValue(value, kotlinType, godotType)

        // Empty array: [], Array[], PackedStringArray()
        isEmptyArray(value) -> parseEmptyArray(value)

        // Empty dictionary: {}
        value == "{}" -> {
            val dictClass = context.classNameForOrDefault("Dictionary")
            CodeBlock.of("%T()", dictClass)
        }

        // Constructor calls: Vector2(0, 0), Color(1, 1, 1)
        isConstructorCall(value) -> parseConstructorCall(value)

        // Enum constants: SIDE_LEFT, HORIZONTAL
        isEnumConstant(value) -> parseEnumConstant(value, godotType)

        // Unknown → null fallback
        else -> {
            logger.warning { "Unknown default value pattern: '$value' for type $godotType" }
            null
        }
    }

    // Numeric Literals (con soporte de notación científica e inferencia del kotlin type system)
    private fun isNumericLiteral(value: String): Boolean {
        // Soporta: 123, -45, 1.5, 1e-05, 2.5e10
        // NO esperamos sufijos de Kotlin (u, f, L) - esos los agregamos nosotros
        return value.matches(NUMERIC_LITERAL_REGEX)
    }

    // Numeric Values (primitivos, enums, o variants)
    context(context: Context)
    private fun parseNumericValue(value: String, kotlinType: TypeName?, godotType: String): CodeBlock = when {
        // 1. Es un Enum → buscar constant por valor
        godotType.startsWith("enum::") -> parseEnumFromValue(value.toLong(), godotType)

        // 2. Es Variant → wrappear en Variant subclass
        godotType == "Variant" -> parseVariantFromValue(value, godotType)

        // 3. Es Bitfield Enum -> Generar
        godotType.startsWith("bitfield::") -> {
            check(kotlinType != null && kotlinType is ParameterizedTypeName) {
                "Bitfield type must be EnumMask parameterized, got: $kotlinType"
            }
            CodeBlock.of("%T(%L)", kotlinType.rawType, value.toLong())
        }

        else -> {
            // Si no tenemos el TypeName de Kotlin, usamos una versión simplificada de literales
            if (kotlinType != null) {
                parseNumericLiteral(value, kotlinType)
            } else {
                // Inferencia básica para llamadas recursivas sin TypeResolver a mano
                if (value.contains('.') || value.contains('e', ignoreCase = true)) {
                    CodeBlock.of("%L", value)
                } else {
                    CodeBlock.of("%L.0", value)
                }
            }
        }
    }

    private fun parseInfinityConstant(value: String, type: String, kotlinType: TypeName?): CodeBlock {
        // Determinar si es float o double
        val isDouble = kotlinType == DOUBLE || type == "double"

        return when {
            // Negativo: -inf, -INF, -1.#INF
            value.startsWith("-") -> {
                if (isDouble) {
                    CodeBlock.of("%T.NEGATIVE_INFINITY", DOUBLE)
                } else {
                    CodeBlock.of("%T.NEGATIVE_INFINITY", FLOAT)
                }
            }

            else -> {
                if (isDouble) {
                    CodeBlock.of("%T.POSITIVE_INFINITY", DOUBLE)
                } else {
                    CodeBlock.of("%T.POSITIVE_INFINITY", FLOAT)
                }
            }
        }
    }

    private fun parseNaNConstant(type: String, kotlinType: TypeName?): CodeBlock = CodeBlock.of(
        "%T.NaN",
        kotlinType ?: if (type == "double") DOUBLE else FLOAT,
    )

    // Enum from numeric value
    context(context: Context)
    private fun parseEnumFromValue(value: Long, godotType: String): CodeBlock {
        // godotType = "enum::Key" o "enum::BaseMaterial3D.Flags"
        val enumTypeStr = godotType.removePrefix("enum::")
        val (className, enumName) = if (enumTypeStr.contains(".")) {
            enumTypeStr.substringBeforeLast(".") to enumTypeStr.substringAfterLast(".")
        } else {
            null to enumTypeStr
        }

        // resolveConstant returns the first-declared alias when multiple constants share this value.
        // For default values this is always correct: all aliases collapse to the same Long at runtime,
        // so emitting any of them produces identical behavior.  The first-declared alias is used
        // because it matches Godot's canonical documentation ordering.
        val constantName = context.resolveEnumConstant(
            parentClass = className,
            enumName = enumName,
            value = value,
        ) ?: run {
            logger.warning { "Enum constant not found: $enumTypeStr = $value, using raw value" }
            return CodeBlock.of("%LL", value)
        }

        val enumTypeName = typeResolver.resolve(godotType)
        return CodeBlock.of("%T.%L", enumTypeName, constantName)
    }

    // Variant from raw value
    context(context: Context)
    private fun parseVariantFromValue(value: String, godotType: String): CodeBlock {
        // Godot envía: { "type": "Variant", "default_value": "0" }
        // Necesitamos wrappear en la subclase correcta de Variant
        check(godotType == "Variant") { "Expected Variant type, got $godotType" }

        val variantClass = context.classNameForOrDefault("Variant")

        // Intentar inferir el tipo del variant desde godotType o meta
        // Por ahora, asumir INT si es entero, FLOAT si tiene decimal

        val hasDecimal = value.contains('.') || value.contains('e', ignoreCase = true)

        return CodeBlock.of("%T(%L)", variantClass, if (hasDecimal) value.toDouble() else value.toLong())
    }

    // Numeric Literals (primitivos puros)
    private fun parseNumericLiteral(value: String, kotlinType: TypeName): CodeBlock = when (kotlinType) {
        U_BYTE, U_SHORT, U_INT -> CodeBlock.of("%Lu", value.toUInt())

        U_LONG -> CodeBlock.of("%LuL", value.toULong())

        // Floating point
        FLOAT -> CodeBlock.of("%Lf", value.toFloat())

        DOUBLE -> CodeBlock.of("%L", value.toDouble())

        LONG -> CodeBlock.of("%LL", value.toLong())

        BYTE, SHORT, INT -> CodeBlock.of("%L", value.toInt())

        // Fallback
        else -> {
            logger.warning { "Unknown numeric type $kotlinType for value '$value', assuming Int" }
            CodeBlock.of(value)
        }
    }

    // NodePath Literals
    context(context: Context)
    private fun parseNodePathLiteral(value: String): CodeBlock {
        // ^"root/child" → NodePath("root/child")
        // ^"" → NodePath("")

        val nodePathClass = context.classNameForOrDefault("NodePath")
        val stringLiteral = value.removePrefix("^") // Quitar el ^, dejar solo "path"

        return CodeBlock.of("%T(%L)", nodePathClass, stringLiteral)
    }

    // StringName Literals
    context(context: Context)
    private fun parseStringNameLiteral(value: String): CodeBlock {
        // &"Master" → StringName("Master")
        // &"" → StringName("")

        val stringNameClass = context.classNameForOrDefault("StringName")
        val stringLiteral = value.removePrefix("&") // Quitar el &, dejar solo "text"

        return CodeBlock.of("%T(%L)", stringNameClass, stringLiteral)
    }

    // String Literals
    context(context: Context)
    private fun parseStringLiteral(value: String): CodeBlock {
        // "text" → GodotString("text") si el tipo es GodotString
        val stringClass = context.classNameForOrDefault("String", "GodotString")
        return CodeBlock.of("%T(%L)", stringClass, value)
    }

    // TypedArray Literals
    private fun isTypedArrayLiteral(value: String): Boolean {
        // Array[Type]([...]) o Array[Type](datos)
        return value.matches(TYPED_ARRAY_REGEX)
    }

    @Suppress("UNUSED_PARAMETER")
    context(context: Context)
    private fun parseTypedArrayLiteral(value: String): CodeBlock {
        // Array[RID]([]) → Array()
        // Array[StringName](["a", "b"]) → Array() // Ignoramos los valores por ahora

        // TODO: En el futuro, cuando tengas typed arrays:
        // Array[RID]([]) → TypedArray<RID>()

        val arrayClass = context.classNameForOrDefault("Array", "GodotArray", typedClass = true)

        // Por ahora, siempre crear array vacío
        // En el futuro podrías parsear el contenido entre paréntesis
        return CodeBlock.of("%T()", arrayClass)
    }

    /*
    // TODO TypedArray Literals con contenido
    context(context: Context)
    private fun parseTypedArrayLiteral(value: String): CodeBlock {
        // Array[RID]([item1, item2]) → Array()

        val typeMatch = Regex("""Array\[([\w:]+)]\((.*)\)""").find(value)
            ?: return CodeBlock.of("%T()", context.classNameForOrDefault("Array"))

        val elementType = typeMatch.groupValues[1] // "RID"
        val content = typeMatch.groupValues[2]     // "[item1, item2]" o "[]"

        val arrayClass = context.classNameForOrDefault("Array", "GodotArray")

        // Si está vacío, constructor vacío
        if (content.trim() == "[]") {
            return CodeBlock.of("%T()", arrayClass)
        }

        // TODO: Parsear contenido si necesario en el futuro
        // Por ahora, siempre vacío es seguro
        return CodeBlock.of("%T()", arrayClass)
    }
     */

    // Arrays
    private fun isEmptyArray(value: String): Boolean = value == "[]" ||
        value == "Array[]" ||
        value.matches(EMPTY_ARRAY_REGEX)

    context(context: Context)
    private fun parseEmptyArray(value: String): CodeBlock {
        // Detectar tipo específico si está presente
        val arrayType = when {
            value.startsWith("PackedStringArray") -> "PackedStringArray"
            value.startsWith("PackedByteArray") -> "PackedByteArray"
            value.startsWith("PackedInt32Array") -> "PackedInt32Array"
            value.startsWith("PackedInt64Array") -> "PackedInt64Array"
            value.startsWith("PackedFloat32Array") -> "PackedFloat32Array"
            value.startsWith("PackedFloat64Array") -> "PackedFloat64Array"
            value.startsWith("PackedVector2Array") -> "PackedVector2Array"
            value.startsWith("PackedVector3Array") -> "PackedVector3Array"
            value.startsWith("PackedColorArray") -> "PackedColorArray"
            else -> "Array"
        }

        val arrayClass = context.classNameForOrDefault(arrayType, typedClass = true)
        return CodeBlock.of("%T()", arrayClass)
    }

    // Constructor Calls
    private fun isConstructorCall(value: String): Boolean = value.matches(CLASS_CONSTRUCTOR_REGEX)

    // Constructor Calls (mejorado con constructor resolution)
    // TODO add indent and format output
    context(context: Context)
    private fun parseConstructorCall(value: String): CodeBlock {
        val className = value.substringBefore('(')
        val argsString = value.substringAfter('(').substringBeforeLast(')')

        val kotlinClass = context.classNameForOrDefault(className)

        if (argsString.isEmpty()) {
            return CodeBlock.of("%T()", kotlinClass)
        }

        val rawArgs = splitConstructorArguments(argsString)

        // ── SPECIAL MAPPING ───────────────────────────────
        val mappedArgs = applySpecialConstructorMapping(className, rawArgs) ?: rawArgs

        // ── CONSTRUCTOR RESOLUTION ───────────────────────
        val constructor = context.resolveConstructor(className, mappedArgs)

        if (constructor == null) {
            logger.warning { "Constructor not found for $className with ${mappedArgs.size} args" }
            return parseConstructorWithRawArgs(kotlinClass, mappedArgs)
        }

        if (constructor.usesKotlinStringBridge && mappedArgs.size == 1) {
            return CodeBlock.of("%T(%L)", kotlinClass, mappedArgs.single())
        }

        check(mappedArgs.size == constructor.arguments.size) {
            "Invalid number of arguments for $className: ${mappedArgs.size} != ${constructor.arguments.size}"
        }

        val convertedArgs = mappedArgs.zip(constructor.arguments) { rawArg, expectedParam ->
            convertConstructorArgument(rawArg.trim(), expectedParam)
        }

        check(convertedArgs.size == constructor.arguments.size) {
            "Failed to convert arguments for $className: ${convertedArgs.size} != ${constructor.arguments.size}"
        }

        return buildFormattedConstructor(kotlinClass, convertedArgs)
    }

    private fun applySpecialConstructorMapping(className: String, rawArgs: List<String>): List<String>? {
        val mapping = SPECIAL_CONSTRUCTOR_MAPPINGS[className] ?: return null
        if (rawArgs.size != mapping.rawArgCount) return null

        return rawArgs
            .chunked(mapping.groupingSize)
            .map { group ->
                "${mapping.groupType}(${group.joinToString(", ")})"
            }
    }

    context(ctx: Context)
    private fun convertConstructorArgument(value: String, expectedParam: MethodArg): CodeBlock {
        val expectedType = typeResolver.resolve(expectedParam)
        return parseDefaultValue(value, expectedType, expectedParam.meta ?: expectedParam.type)
            ?: run {
                // Fallback al valor crudo si parseDefaultValue devuelve null
                logger.warning {
                    "Unable to parse default value '$value' for ${expectedParam.name}: ${expectedParam.type} on constructor args, using raw value"
                }

                CodeBlock.of("%L", value)
            }
    }

    // Fallback cuando no encontramos el constructor
    context(context: Context)
    private fun parseConstructorWithRawArgs(kotlinClass: ClassName, rawArgs: List<String>): CodeBlock {
        val convertedArgs = rawArgs.map { arg ->
            when {
                isNumericLiteral(arg) -> parseNumericValue(arg, null, "")
                isConstructorCall(arg) -> parseConstructorCall(arg)
                else -> CodeBlock.of("%L", arg)
            }
        }
        return buildFormattedConstructor(kotlinClass, convertedArgs)
    }

    // Helper: split respetando paréntesis anidados
    private fun splitConstructorArguments(argsString: String): List<String> {
        val args = mutableListOf<String>()
        var current = StringBuilder()
        var depth = 0

        for (char in argsString) {
            when (char) {
                '(' -> {
                    depth++
                    current.append(char)
                }

                ')' -> {
                    depth--
                    current.append(char)
                }

                ',' -> {
                    if (depth == 0) {
                        args.add(current.toString().trim())
                        current = StringBuilder()
                    } else {
                        current.append(char)
                    }
                }

                else -> current.append(char)
            }
        }

        if (current.isNotEmpty()) {
            args.add(current.toString().trim())
        }

        return args
    }

    // Enum Constants
    private fun isEnumConstant(value: String): Boolean = value.matches(ENUM_CONSTANT_REGEX)

    context(context: Context)
    private fun parseEnumConstant(value: String, godotType: String): CodeBlock {
        // SIDE_LEFT → Side.LEFT
        // HORIZONTAL → Orientation.HORIZONTAL

        val cleanType = godotType.removePrefix("enum::").removePrefix("bitfield::")

        // Determinar si es enum global o nested
        val (className, enumName) = if (cleanType.contains('.')) {
            cleanType.substringBeforeLast('.') to cleanType.substringAfterLast('.')
        } else {
            null to cleanType
        }

        // Buscar en EnumConstantResolver
        val allConstants = context.getConstantEnumNamesFor(className, enumName)

        // Buscar el constant por nombre original
        val constantName = allConstants.find { it == value }
            ?: run {
                logger.warning { "Enum constant '$value' not found in $className.$enumName" }
                return CodeBlock.of(value) // Fallback: usar valor raw
            }

        // Resolver el TypeName del enum
        val enumTypeName = context.classNameForOrDefault(
            godotName = if (className != null) "$className.$enumName" else enumName,
        )

        return CodeBlock.of("%T.%L", enumTypeName, constantName)
    }

    /**
     * Esta es la clave: Usamos joinToCode con separadores que incluyen el newline
     * pero NO usamos addStatement dentro de la recursión para evitar cerrar el scope.
     */
    private fun buildFormattedConstructor(kotlinClass: TypeName, args: List<CodeBlock>): CodeBlock {
        if (args.size <= 4 && args.all { it.toString().length < 40 }) {
            return CodeBlock.of("%T(%L)", kotlinClass, args.joinToCode(", "))
        }
        return CodeBlock
            .builder()
            .add("%T(\n", kotlinClass)
            .indent()
            .add(args.joinToCode(separator = ",\n", suffix = ",\n"))
            .unindent()
            .add(")")
            .build()
    }
}
