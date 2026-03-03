package io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver

object NativeStructureParser {
    data class NativeStructureField(
        val name: String,
        val type: String,
        val arraySize: Int? = null,
        val defaultValue: String? = null,
    ) {
        init {
            check(name.isNotBlank()) { "Name cannot be empty" }
            check(type.isNotBlank()) { "Type cannot be empty" }
            check(defaultValue == null || defaultValue.isNotBlank() || defaultValue != "ERROR") {
                "Default value cannot be empty or 'ERROR'"
            }
            check(arraySize == null || arraySize > 0) { "Array size must be positive" }
        }
    }

    /**
     * Parsea el string de formato de Godot (estilo C) a una lista de campos.
     * Ejemplo: "Vector2 position; float data[4]; Object* owner"
     */
    fun parseFormat(input: String): List<NativeStructureField> {
        return input.trim()
            .split(";")
            .filter { it.trim().isNotBlank() }
            .mapNotNull { rawVar ->
                val parts = rawVar.trim().split(Regex("\\s+"), limit = 3)
                if (parts.size < 2) return@mapNotNull null

                var fieldType = parts[0].trim()
                var fieldName = parts[1].trim()
                var defaultValue: String? = null

                // 1. Manejo de punteros: Si el nombre empieza con '*', moverlo al tipo
                if (fieldName.startsWith('*')) {
                    fieldName = fieldName.removePrefix("*").trim()
                    fieldType += "*"
                }

                // 2. Limpieza de valores por defecto (ej. "int a = 0") [cite: 79]
                if (fieldName.contains("=")) {
                    fieldName = fieldName.substringBefore("=").trim()
                    defaultValue = fieldName.substringAfter("=", "ERROR").trim()
                }

                if (parts.size > 2) defaultValue = parts[2].removePrefix("=").trim()

                // 3. Extracción de tamaño de arrays (ej. "float values[16]") [cite: 81, 82]
                var arraySize: Int? = null
                if (fieldName.contains('[') && fieldName.endsWith(']')) {
                    val sizeStr = fieldName.substringAfter('[').substringBefore(']')
                    arraySize = sizeStr.toIntOrNull()
                    fieldName = fieldName.substringBefore('[').trim()
                }

                NativeStructureField(
                    name = fieldName,
                    type = normalizeType(fieldType),
                    arraySize = arraySize,
                    defaultValue = defaultValue,
                )
            }
    }

    /**
     * Normaliza tipos de enums escopados (ej. TextServer::Direction)
     * al formato esperado por el TypeResolver.
     */
    private fun normalizeType(type: String): String = if (type.contains("::")) {
        "enum::${type.replace("::", ".")}"
    } else {
        type
    }
}
