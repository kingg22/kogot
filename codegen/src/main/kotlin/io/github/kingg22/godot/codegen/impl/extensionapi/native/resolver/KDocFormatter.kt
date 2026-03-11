package io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver

import io.github.kingg22.godot.codegen.impl.extensionapi.PackageRegistry
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.impl.snakeCaseToCamelCase

private val cache = LinkedHashMap<String, String>(2048)
private val SPECIAL_SECTION_REGEX = Regex("""\*\*(Note|Warning|Deprecated|See also|Example)s?:\*\*""")
private val TOO_MANY_BREAK_LINES_REGEX = Regex("\n{3,}")

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

        return cache.getOrPut(description) {
            // 1. Pre-procesamiento de escapes y limpieza inicial
            val prepared = description.trim()
                .replace("*/", "*\\/")
                .replace("/*", "/\\*")
                .replace("[br]", "\n")

            // 2. Parser de una sola pasada para tags inline y bloques
            val transformed = transformTags(prepared)

            // 3. Formateo de secciones y wrapping
            val withSections = SPECIAL_SECTION_REGEX.replace(transformed) { "\n\n${it.value}" }

            // 4. Limpieza de saltos de lineas
            val normalized = withSections.replace(TOO_MANY_BREAK_LINES_REGEX, "\n\n")

            wrapLongLines(normalized)
        }
    }

    context(_: PackageRegistry)
    private fun transformTags(text: String): StringBuilder {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            if (text[i] == '[' && i + 1 < text.length) {
                // Caso especial: URL requiere lookahead para encontrar el cierre [/url]
                if (text.startsWith("[url=", i)) {
                    val endTagIdx = text.indexOf("[/url]", i)
                    if (endTagIdx != -1) {
                        val fullUrlBlock = text.substring(i, endTagIdx + 6)
                        if (processUrlTag(fullUrlBlock, sb)) {
                            i = endTagIdx + 6
                            continue
                        }
                    }
                }

                val closeIdx = text.indexOf(']', i)
                if (closeIdx != -1) {
                    val tagContent = text.substring(i + 1, closeIdx)
                    if (processTag(tagContent, sb)) {
                        i = closeIdx + 1
                        continue
                    }
                }
            }
            sb.append(text[i])
            i++
        }
        return sb
    }

    /**
     * Procesa específicamente el bloque [url=link]texto[/url]
     */
    private fun processUrlTag(fullBlock: String, sb: StringBuilder): Boolean {
        // Formato: [url=https://...]Texto[/url]
        val urlStart = fullBlock.indexOf('=') + 1
        val urlEnd = fullBlock.indexOf(']', urlStart)
        val textEnd = fullBlock.indexOf("[/url]")

        if (urlStart in 1..<urlEnd && textEnd > urlEnd) {
            val url = fullBlock.substring(urlStart, urlEnd).replace("\n", "").trim()
            val linkText = fullBlock.substring(urlEnd + 1, textEnd).trim()
            sb.append("[").append(linkText).append("](").append(url).append(")")
            return true
        }
        return false
    }

    /**
     * Procesa tags y escribe directamente en el StringBuilder.
     * @return true si el tag fue reconocido y procesado.
     */
    context(packageRegistry: PackageRegistry)
    private fun processTag(tag: String, sb: StringBuilder): Boolean = when {
        // Estilos simples
        tag == "b" || tag == "/b" -> {
            sb.append("**")
            true
        }

        tag == "i" || tag == "/i" -> {
            sb.append('*')
            true
        }

        // Bloques de código
        tag.startsWith("gdscript") -> {
            sb.ensureNewLine()
            sb.append("```gdscript")
            true
        }

        tag == "/gdscript" -> {
            sb.ensureNewLine()
            sb.append("```\n")
            true
        }

        tag.startsWith("csharp") -> {
            sb.ensureNewLine()
            sb.append("```csharp")
            true
        }

        tag == "/csharp" -> {
            sb.ensureNewLine()
            sb.append("```\n")
            true
        }

        tag == "codeblocks" || tag == "/codeblocks" -> {
            // ignorar completamente, manejado por otros casos
            true
        }

        tag.startsWith("codeblock") || tag.startsWith("codeblocks") -> {
            val lang = tag.substringAfter("lang=", "").substringBefore(" ")
            sb.ensureNewLine()
            sb.append("```$lang\n")
            true
        }

        tag == "/codeblock" -> {
            sb.ensureNewLine()
            sb.append("```\n")
            true
        }

        tag.startsWith("code") || tag == "/code" -> {
            sb.append('`')
            true
        }

        // Links y referencias
        tag.startsWith("param ") -> {
            val p = tag.substring(6)
            sb.append("[${if (p.startsWith("@")) p else safeIdentifier(p)}]")
            true
        }

        tag.startsWith("method ") || tag.startsWith("member ") -> {
            val isMethod = tag.startsWith("method")
            val path = tag.substring(if (isMethod) 7 else 7)
            val dotIdx = path.indexOf('.')
            if (dotIdx != -1) {
                val className = path.substring(0, dotIdx)
                val memberName = safeIdentifier(path.substring(dotIdx + 1))
                val kClass = packageRegistry.classNameForOrNull(className)?.canonicalName
                if (kClass != null) {
                    sb.append("[$className.$memberName][$kClass.$memberName]")
                } else {
                    sb.append("[$memberName]")
                }
            } else {
                sb.append("[${safeIdentifier(path)}]")
            }
            true
        }

        // Tipos y Enums
        tag.startsWith("enum ") -> {
            sb.append("[${tag.substring(5)}]")
            true
        }

        tag.startsWith("constant ") -> {
            sb.append("[${tag.substring(9)}]")
            true
        }

        // Primitivos
        tag == "int" || tag == "float" || tag == "bool" -> {
            val kType = when (tag) {
                "int" -> "Int"
                "float" -> "Float"
                else -> "Boolean"
            }
            sb.append("[${tag.replaceFirstChar { it.uppercase() }}][kotlin.$kType]")
            true
        }

        // Clases [ClassName]
        tag.isNotEmpty() && (tag[0].isUpperCase() || tag.startsWith("@")) -> {
            val kName = packageRegistry.classNameForOrNull(tag)?.canonicalName
            if (kName != null && kName.contains('.')) {
                sb.append("[${kName.substringAfterLast('.')}][$kName]")
            } else {
                sb.append("[${tag.snakeCaseToCamelCase().replaceFirstChar { it.uppercase() }}]")
            }
            true
        }

        else -> false
    }

    private fun wrapLongLines(text: String): String {
        val result = StringBuilder(text.length)
        var lineStart = 0
        var insideCode = false
        val limit = 116

        while (lineStart < text.length) {
            val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
            val line = text.substring(lineStart, lineEnd)

            if (line.contains("```")) insideCode = !insideCode

            if (insideCode || line.length <= limit) {
                result.append(line)
            } else {
                var currentLine = line
                while (currentLine.length > limit) {
                    val breakAt = currentLine.lastIndexOf(' ', limit).let { if (it == -1) limit else it }
                    result.append(currentLine.substring(0, breakAt)).ensureNewLine()
                    currentLine = currentLine.substring(if (breakAt < currentLine.length) breakAt + 1 else breakAt)
                }
                result.append(currentLine)
            }

            if (lineEnd < text.length) result.append('\n')
            lineStart = lineEnd + 1
        }
        return result.toString().trim()
    }

    private fun StringBuilder.ensureNewLine() = apply {
        if (isNotEmpty() && last() != '\n') append('\n')
    }
}
