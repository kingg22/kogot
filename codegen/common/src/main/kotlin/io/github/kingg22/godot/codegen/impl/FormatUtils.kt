package io.github.kingg22.godot.codegen.impl

/**
 * Produces a safe Kotlin identifier for a **method/property/parameter** name.
 *
 * - Replaces illegal characters with `_`
 * - Prepends `_` if starts with a digit
 * - Converts snake_case → camelCase
 * - Backtick-escapes Kotlin keywords
 */
fun safeIdentifier(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return "_"
    val sanitized = trimmed.replace(NAME_REGEX, "_")
    val fixed = if (sanitized.first().isDigit()) {
        println("WARNING: Sanitizing identifier: $name, start with digit: $sanitized")
        "_$sanitized"
    } else {
        sanitized
    }
    return fixed.snakeCaseToCamelCase()
}

/**
 * Produces a safe Kotlin identifier for a **type name** (class, enum constant).
 *
 * Same rules as [safeIdentifier] but appends `_` instead of backtick-escaping,
 * because type names in KotlinPoet don't support backtick escaping.
 */
fun sanitizeTypeName(name: String): String {
    val trimmed = name.trim()
    check(trimmed.isNotBlank()) { "Type name cannot be blank, '$name' given" }
    val sanitized = trimmed.replace(NAME_REGEX, "_")
    val fixed = if (sanitized.first().isDigit()) {
        println("WARNING: Sanitizing type name: $name, start with digit: $sanitized")
        "_$sanitized"
    } else {
        sanitized
    }
    return if (isKotlinKeyword(fixed)) {
        println("WARNING: Sanitizing type name: $name, is Kotlin keyword: $fixed")
        "${fixed}_"
    } else {
        fixed
    }
}

fun String.renameGodotClass(getTypedClass: Boolean = false): String = when {
    // 1. Comparación de longitud y contenido sin crear objetos nuevos.
    // Usamos ignoreCase = true para evitar el .lowercase()

    this.equals("Object", ignoreCase = true) -> "GodotObject"

    this.equals("Error", ignoreCase = true) -> "GodotError"

    this.equals("String", ignoreCase = true) -> "GodotString"

    this.equals("Array", ignoreCase = true) -> if (!getTypedClass) "VariantArray" else "GodotArray"

    this.equals("Dictionary", ignoreCase = true) -> if (!getTypedClass) "VariantDictionary" else "Dictionary"

    this.equals("Range", ignoreCase = true) -> "GodotRange"

    // 2. Chequeo de "All Upper Case" manualmente
    isAllUpperCase(this) -> this.lowercase().replaceFirstChar { it.uppercaseChar() }

    else -> this
}

fun String.renameAllUpperCaseToCamelCase() = if (isAllUpperCase(this)) {
    this.lowercase().replaceFirstChar { it.uppercaseChar() }
} else {
    this
}

/** Verifica si todos los caracteres son mayúsculas sin crear iteradores. */
private fun isAllUpperCase(str: String): Boolean {
    if (str.isEmpty()) return false
    for (i in str.indices) {
        if (!str[i].isUpperCase()) return false
    }
    return true
}

fun String.snakeCaseToCamelCase(): String {
    val prefix = takeWhile { it == '_' }
    val core = drop(prefix.length)
    return buildString {
        append(prefix)
        var upperNext = false
        for (c in core) {
            when {
                c == '_' -> upperNext = true

                upperNext -> {
                    append(c.uppercaseChar())
                    upperNext = false
                }

                else -> append(c)
            }
        }
    }
}

fun String.snakeCaseToPascalCase(): String = split('_')
    .filter { it.isNotBlank() }
    .joinToString("") { token ->
        token.lowercase().replaceFirstChar(Char::uppercaseChar)
    }

/**
 * Extensión de String para convertir formatos CamelCase o PascalCase
 * a SCREAMING_SNAKE_CASE.
 */
fun String.toScreamingSnakeCase(): String {
    if (this.isBlank()) return ""

    val tmp = this.trim()
        // 1. Insertar guion bajo entre minúscula/número y una mayúscula
        // Ejemplo: user1Login -> user1_Login
        .replace(WORDS_REGEX, "$1_$2")
        // 2. Insertar guion bajo entre una letra y un número (si se desea que el número sea palabra aparte)
        // Ejemplo: User1 -> USER_1
        // .replace(WORDS_2_REGEX, "$1_$2")
        // 3. Manejar acrónimos: insertar guion bajo antes de la última mayúscula de una serie
        // Ejemplo: HTTPResponse -> HTTP_Response
        .replace(ABC_REGEX, "$1_$2")
        // 4. Convertir todo a mayúsculas
        .uppercase()
        // 5. Limpiar posibles guiones bajos duplicados (por si el input ya tenía algunos)
        .replace(UNDERSCORES_REGEX, "_")
    if (tmp.endsWith("2_D")) return tmp.dropLast(3) + "2D"
    if (tmp.endsWith("3_D")) return tmp.dropLast(3) + "3D"
    return tmp
}

private fun String.fixed2d3d(): String = when {
    this.endsWith("2d") -> this.dropLast(2) + "2D"
    this.endsWith("3d") -> this.dropLast(2) + "3D"
    else -> this
}

fun String.screamingToPascalCase(): String = this
    .substringAfter("TYPE_")
    .split("_")
    .joinToString("") { part ->
        val str = part.lowercase().replaceFirstChar { it.uppercase() }
        when {
            str.endsWith("2d") -> str.dropLast(2) + "2D"
            str.endsWith("3d") -> str.dropLast(2) + "3D"
            else -> str
        }
    }.fixed2d3d()

// ── Kotlin keyword list ───────────────────────────────────────────────────────

fun isKotlinKeyword(name: String): Boolean = name in KOTLIN_KEYWORDS

fun checkAndNormalizeTypeName(rawType: String): String {
    val type = rawType.trim()
    if (type.any { it in ILLEGAL_CHARACTERS_TO_ESCAPE }) {
        error(
            "FATAL: Type '$rawType' contains " +
                ILLEGAL_CHARACTERS_TO_ESCAPE.intersect(rawType.toSet()).joinToString("") +
                ", which is not supported by KotlinPoet.",
        )
    }
    check(!type.isBlank()) { "Type name cannot be blank, '$rawType' given" }
    return type
}

private val NAME_REGEX = Regex("[^A-Za-z0-9_]")
private val WORDS_REGEX = "([a-z0-9])([A-Z])".toRegex()
private val WORDS_2_REGEX = "([a-zA-Z])([0-9])".toRegex()
private val ABC_REGEX = "([A-Z])([A-Z][a-z])".toRegex()
private val UNDERSCORES_REGEX = "__+".toRegex()

// https://github.com/JetBrains/kotlin/blob/master/compiler/frontend.java/src/org/jetbrains/kotlin/resolve/jvm/checkers/JvmSimpleNameBacktickChecker.kt
// '.', special handle
private val ILLEGAL_CHARACTERS_TO_ESCAPE = setOf(';', '[', ']', '/', '<', '>', ':', '\\')

// https://kotlinlang.org/docs/keyword-reference.html
private val KOTLIN_KEYWORDS = setOf(
    // Hard keywords
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
    "if", "in", "interface", "is", "null", "object", "package", "return",
    "super", "this", "throw", "true", "try", "typealias", "typeof", "val",
    "var", "when", "while",
    // Soft keywords
    "by", "catch", "constructor", "delegate", "dynamic", "field", "file",
    "finally", "get", "import", "init", "param", "property", "receiver",
    "set", "setparam", "where",
    // Modifier keywords
    "actual", "abstract", "annotation", "companion", "const", "crossinline",
    "data", "enum", "expect", "external", "final", "infix", "inline", "inner",
    "internal", "lateinit", "noinline", "open", "operator", "out", "override",
    "private", "protected", "public", "reified", "sealed", "suspend",
    "tailrec", "value", "vararg",
    // Legacy / still problematic
    "header", "impl", "yield",
)
