package io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver

import io.github.kingg22.godot.codegen.impl.extensionapi.PackageRegistry
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.impl.snakeCaseToCamelCase

/**
 * Converts Godot BBCode documentation to KDoc format.
 *
 * Godot uses BBCode tags in descriptions (e.g., [code], [param], [Node]).
 * This formatter converts them to proper KDoc/Markdown syntax.
 *
 * ## Supported conversions:
 * - `[code]...[/code]` → `` `...` ``
 * - `[codeblock]...[/codeblock]` → ` ```kotlin ... ``` `
 * - `[b]...[/b]` → `**...**`
 * - `[i]...[/i]` → `*...*`
 * - `[param name]` → `[name]`
 * - `[method method_name]` → `[methodName]`
 * - `[member property_name]` → `[propertyName]`
 * - `[signal signal_name]` → `[signalName]`
 * - `[ClassName]` → `[ClassName]`
 * - `[enum EnumName]` → `[EnumName]`
 * - `[constant CONSTANT_NAME]` → `[CONSTANT_NAME]`
 * - `[url=https://...]text[/url]` → `[text](https://...)`
 * - `[br]` → newline
 *
 * ## Example:
 * ```kotlin
 * // Godot:
 * "Returns [code]true[/code] if [param node] is a [Node2D]."
 *
 * // KDoc:
 * "Returns `true` if [node] is a [Node2D]."
 * ```
 */
object KDocFormatter {
    /** Formatea un valor de retorno para KDoc @return tag. */
    context(_: PackageRegistry)
    fun formatReturn(description: String): String? {
        val formattedDesc = format(description) ?: return null
        return "@return $formattedDesc"
    }

    /**
     * Formats Godot BBCode description into KDoc-compatible text.
     *
     * @param description Raw BBCode text from extension_api.json
     * @return Formatted KDoc string
     */
    context(_: PackageRegistry)
    fun format(description: String): String? {
        if (description.isBlank()) return null

        var result = description.trim()

        // 0. Wrap long lines (before any transformations)
        result = wrapLongLines(result)

        // 1. Escape existing block comment delimiters to prevent breaking KDoc
        result = escapeCommentDelimiters(result)

        // 2. Convert codeblocks first (multiline) - ANTES de inline code
        result = convertCodeblocks(result)

        // 3. Convert inline code
        result = convertInlineCode(result)

        // 4. Convert text formatting (bold, italic)
        result = convertTextFormatting(result)

        // 5. Add double newlines for special sections (Note, Warning, etc.)
        result = formatSpecialSections(result)

        // 6. Convert links (urls, classes, methods, params, etc.)
        result = convertLinks(result)

        // 7. Convert line breaks
        result = convertLineBreaks(result)

        // 8. Clean up whitespace
        result = cleanupWhitespace(result)

        return result
    }

    // STEP 0: Wrap long lines
    private const val MAX_LINE_LENGTH = 120
    private const val KDOC_INDENT = " * " // KDoc prefix per line
    private const val EFFECTIVE_MAX_LENGTH = MAX_LINE_LENGTH - KDOC_INDENT.length // ~116 chars

    /**
     * Wraps long lines to fit within KDoc max line length.
     *
     * Respects:
     * - Existing line breaks
     * - Codeblocks (don't wrap inside)
     * - URLs (don't break)
     * - BBCode tags (don't break mid-tag)
     */
    private fun wrapLongLines(text: String): String {
        val lines = text.lines()
        val wrappedLines = mutableListOf<String>()

        var insideCodeblock = false

        for (line in lines) {
            // Detect codeblock boundaries
            if (line.contains(Regex("""\[codeblocks?(\s+lang=.*)?]""")) ||
                line.contains("[gdscript]") ||
                line.contains("[csharp]")
            ) {
                insideCodeblock = true
            }
            if (line.contains(Regex("""\[/codeblocks?]""")) ||
                line.contains("[/gdscript]") ||
                line.contains("[/csharp]")
            ) {
                insideCodeblock = false
                wrappedLines.add(line)
                continue
            }

            // Don't wrap inside codeblocks
            if (insideCodeblock) {
                wrappedLines.add(line)
                continue
            }

            // Don't wrap if line is already short enough
            if (line.length <= EFFECTIVE_MAX_LENGTH) {
                wrappedLines.add(line)
                continue
            }

            // Wrap the long line
            wrappedLines.addAll(wrapSingleLine(line))
        }

        return wrappedLines.joinToString("\n")
    }

    /** Wraps a single long line into multiple lines. */
    private fun wrapSingleLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var remaining = line

        while (remaining.length > EFFECTIVE_MAX_LENGTH) {
            val breakPoint = findBreakPoint(remaining, EFFECTIVE_MAX_LENGTH)

            if (breakPoint <= 0) {
                // Can't find a good break point, force break at max length
                result.add(remaining.substring(0, EFFECTIVE_MAX_LENGTH))
                remaining = remaining.substring(EFFECTIVE_MAX_LENGTH)
            } else {
                result.add(remaining.substring(0, breakPoint).trimEnd())
                remaining = remaining.substring(breakPoint).trimStart()
            }
        }

        if (remaining.isNotEmpty()) {
            result.add(remaining)
        }

        return result
    }

    /**
     * Finds the best position to break a line.
     *
     * Priority:
     * 1. After the sentence ends (. ! ?)
     * 2. After comma or semicolon
     * 3. At last space before maxLength
     * 4. Don't break inside BBCode tags
     * 5. Don't break URLs
     */
    private fun findBreakPoint(text: String, maxLength: Int): Int {
        val searchRange = text.substring(0, minOf(maxLength, text.length))

        // Don't break inside BBCode tags [...] or [/...]
        val openBracket = searchRange.lastIndexOf('[')
        val closeBracket = searchRange.lastIndexOf(']')
        val insideTag = openBracket > closeBracket

        if (insideTag) {
            // Break before the tag
            return openBracket
        }

        // Don't break URLs (including multiline URLs)
        val urlPattern = Regex("""https?://[^\s\]]+""")
        val urlMatch = urlPattern.find(searchRange)
        if (urlMatch != null && urlMatch.range.last > maxLength - 20) {
            // URL near end, break before it
            return urlMatch.range.first
        }

        // Priority 1: After sentence end
        val sentenceEnd = searchRange.lastIndexOfAny(charArrayOf('.', '!', '?'))
        if (sentenceEnd > maxLength / 2) { // Only if it's past halfway
            // Make sure it's actually sentence end, not part of something else
            if (sentenceEnd < searchRange.length - 1) {
                val nextChar = searchRange[sentenceEnd + 1]
                if (nextChar.isWhitespace()) {
                    return sentenceEnd + 1
                }
            }
        }

        // Priority 2: After comma or semicolon
        val punctuation = searchRange.lastIndexOfAny(charArrayOf(',', ';'))
        if (punctuation > maxLength / 2) {
            return punctuation + 1
        }

        // Priority 3: Last space before maxLength
        val lastSpace = searchRange.lastIndexOf(' ')
        if (lastSpace > 0) {
            return lastSpace
        }

        // Priority 4: Last space anywhere
        val anySpace = text.indexOf(' ', maxLength)
        if (anySpace > 0) {
            return anySpace
        }

        // Can't find a break point
        return -1
    }

    // STEP 1: Escape comment delimiters
    private fun escapeCommentDelimiters(text: String): String {
        return text
            .replace("*/", "*\\/") // Prevent closing KDoc block
            .replace("/*", "/\\*") // Prevent opening nested comment
    }

    // STEP 2: Codeblocks
    // Primero detectar codeblocks sin lenguaje específico para evitar que los demás regex los capturen
    private val CODEBLOCK_REGEX = """\[codeblocks?(?:\s+lang=(\w+))?](.*?)\[/codeblocks?]""".toRegex(
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE),
    )
    private val GDSCRIPT_CODEBLOCK_REGEX = """\[gdscript](.*?)\[/gdscript]""".toRegex(
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE),
    )
    private val CSHARP_CODEBLOCK_REGEX = """\[csharp](.*?)\[/csharp]""".toRegex(
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE),
    )

    private fun convertCodeblocks(text: String): String {
        var result = text

        // Primero convertir los específicos (gdscript y csharp)
        // para evitar que el regex general los capture

        // [gdscript] → ```gdscript
        result = GDSCRIPT_CODEBLOCK_REGEX.replace(result) { match ->
            val code = match.groupValues[1].trim()
            "\n```gdscript\n$code\n```\n"
        }

        // [csharp] → ```csharp
        result = CSHARP_CODEBLOCK_REGEX.replace(result) { match ->
            val code = match.groupValues[1].trim()
            "\n```csharp\n$code\n```\n"
        }

        // [codeblock] o [codeblocks lang=xxx] → ```lang
        result = CODEBLOCK_REGEX.replace(result) { match ->
            val lang = match.groupValues.getOrNull(1)?.trim() ?: ""
            val code = match.groupValues[2].trim()
            "\n```$lang\n$code\n```\n"
        }

        return result
    }

    // STEP 3: Inline code
    private val INLINE_CODE_REGEX = """\[code(?:\s+skip-lint)?](.*?)\[/code]""".toRegex(
        RegexOption.DOT_MATCHES_ALL,
    )

    private fun convertInlineCode(text: String): String = INLINE_CODE_REGEX.replace(text) { match ->
        val code = match.groupValues[1].trim()
        "`$code`"
    }

    // STEP 4: Text formatting
    private val BOLD_REGEX = """\[b](.*?)\[/b]""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val ITALIC_REGEX = """\[i](.*?)\[/i]""".toRegex(RegexOption.DOT_MATCHES_ALL)

    private fun convertTextFormatting(text: String): String {
        var result = text

        // [b]...[/b] → **...**
        result = BOLD_REGEX.replace(result) { match ->
            "**${match.groupValues[1]}**"
        }

        // [i]...[/i] → *...*
        result = ITALIC_REGEX.replace(result) { match ->
            "*${match.groupValues[1]}*"
        }

        return result
    }

    // STEP 5: Format special sections (Note, Warning, See also, etc.)
    private val SPECIAL_SECTION_REGEX = """\*\*(Note|Warning|Deprecated|See also|Example)s?:\*\*""".toRegex()

    private fun formatSpecialSections(text: String): String {
        // Add double newline before special sections for better readability
        return SPECIAL_SECTION_REGEX.replace(text) { match ->
            "\n\n${match.value}"
        }
    }

    // STEP 6: Links
    // URL que puede estar en múltiples líneas
    private val URL_REGEX = """\[url=((?:https?://)?[^]]+)](.*?)\[/url]""".toRegex(
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE),
    )

    // Soportar @ prefix para referencias a clases globales como @GDScript
    private val PARAM_REGEX = """\[param ([a-zA-Z_@][a-zA-Z0-9_]*)]""".toRegex()

    // Soportar método con clase: [method ClassName.method_name] o [method @GDScript.method_name]
    private val METHOD_WITH_CLASS_REGEX = """\[method (@?[A-Z][a-zA-Z0-9]*\.)?([a-zA-Z_][a-zA-Z0-9_]*)]""".toRegex()

    // Soportar member con clase: [member ClassName.property_name]
    private val MEMBER_WITH_CLASS_REGEX = """\[member (@?[A-Z][a-zA-Z0-9]*\.)?([a-zA-Z_][a-zA-Z0-9_]*)]""".toRegex()

    private val SIGNAL_REGEX = """\[signal ([a-zA-Z_][a-zA-Z0-9_]*)]""".toRegex()
    private val CONSTANT_REGEX = """\[constant ([A-Z_][A-Z0-9_]*)]""".toRegex()
    private val ENUM_REGEX = """\[enum ([A-Z][a-zA-Z0-9]*)]""".toRegex()

    // Class references: [ClassName] or [ClassName.NestedClass] or [@GDScript]
    private val CLASS_REGEX = """\[(@?[A-Z][a-zA-Z0-9]*(?:\.[A-Z][a-zA-Z0-9]*)*)]""".toRegex()

    // Primitivos de Godot que no deben generar links incorrectos
    private val PRIMITIVE_REGEX = """\[(int|float|bool|nil)]""".toRegex()

    context(packageRegistry: PackageRegistry)
    private fun convertLinks(text: String): String {
        var result = text

        // [url=...]...[/url] → [text](url)
        // Normalizar URLs que están en múltiples líneas
        result = URL_REGEX.replace(result) { match ->
            val url = match.groupValues[1].replace("\n", "").trim()
            val linkText = match.groupValues[2].trim()
            "[$linkText]($url)"
        }

        // [param name] → [name]
        result = PARAM_REGEX.replace(result) { match ->
            val paramName = match.groupValues[1]
            // Si empieza con @, dejarlo tal cual
            if (paramName.startsWith("@")) {
                "[$paramName]"
            } else {
                val kotlinName = safeIdentifier(paramName)
                "[$kotlinName]"
            }
        }

        // [method ClassName.method_name] o [method @GDScript.method_name] → [methodName]
        result = METHOD_WITH_CLASS_REGEX.replace(result) { match ->
            val className = match.groupValues[1].removeSuffix(".")
            val kotlinClassName = className
                .takeIf { it.isNotEmpty() }
                ?.let { packageRegistry.classNameForOrNull(className) }?.canonicalName
            val methodName = match.groupValues[2]
            val kotlinName = safeIdentifier(methodName)

            if (kotlinClassName != null) {
                // Incluir la clase en la documentación
                "[$className.$kotlinName][$kotlinClassName.$kotlinName]\n"
            } else {
                "[$kotlinName]"
            }
        }

        // [member ClassName.property_name] → [propertyName]
        result = MEMBER_WITH_CLASS_REGEX.replace(result) { match ->
            val className = match.groupValues[1].removeSuffix(".")
            val kotlinClassName = className
                .takeIf { it.isNotEmpty() }
                ?.let { packageRegistry.classNameForOrNull(className) }
            val memberName = match.groupValues[2]
            val kotlinPropertyName = safeIdentifier(memberName)

            if (kotlinClassName != null) {
                "[$className.$kotlinPropertyName][${kotlinClassName.canonicalName}.$kotlinPropertyName]\n"
            } else {
                "[$kotlinPropertyName]"
            }
        }

        // [signal signal_name] → [signalName]
        result = SIGNAL_REGEX.replace(result) { match ->
            val signalName = match.groupValues[1]
            val kotlinName = safeIdentifier(signalName)
            "[$kotlinName]"
        }

        // [constant CONSTANT_NAME] → [CONSTANT_NAME]
        result = CONSTANT_REGEX.replace(result) { match ->
            val constantName = match.groupValues[1]
            "[$constantName]"
        }

        // [enum EnumName] → [EnumName]
        result = ENUM_REGEX.replace(result) { match ->
            val enumName = match.groupValues[1]
            "[$enumName]"
        }

        // Primitivos: [int], [float], [bool], [nil] → sin link
        result = PRIMITIVE_REGEX.replace(result) { match ->
            val primitiveName = match.groupValues[1]
            val kotlinName = when (primitiveName.lowercase()) {
                "int" -> "kotlin.Int"
                "float" -> "kotlin.Float"
                "bool" -> "kotlin.Boolean"
                "nil" -> "null"
                else -> primitiveName
            }
            "[${primitiveName.replaceFirstChar(Char::uppercaseChar)}][$kotlinName]"
        }

        // [ClassName] or [ClassName.NestedClass] or [@GDScript] → [ClassName]
        result = CLASS_REGEX.replace(result) { match ->
            val className = match.groupValues[1]

            // Si empieza con @, es una clase global, dejar sin modificar
            if (className.startsWith("@")) {
                "[$className]"
            } else {
                val kotlinName = packageRegistry.classNameForOrNull(className)?.canonicalName
                    ?: className.snakeCaseToCamelCase().replaceFirstChar(Char::uppercaseChar)

                if (kotlinName.contains('.')) {
                    // Show a simple name and full qualified link
                    val simpleName = kotlinName.substringAfterLast('.')
                    "[$simpleName][$kotlinName]\n"
                } else {
                    "[$kotlinName]"
                }
            }
        }

        return result
    }

    // STEP 7: Line breaks
    private fun convertLineBreaks(text: String): String = text.replace("[br]", "\n")

    // STEP 8: Cleanup
    private fun cleanupWhitespace(text: String): String = text
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .replace(Regex("\n{4,}"), "\n\n")
        .trim()
}
