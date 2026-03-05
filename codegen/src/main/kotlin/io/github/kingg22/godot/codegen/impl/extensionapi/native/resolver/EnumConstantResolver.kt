package io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver

import io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi

class EnumConstantResolver(private val enumsByParent: Map<String, Map<String, Map<Long, String>>>) {
    init {
        println("Enum Constant Resolver started with ${enumsByParent.keys.size} entries")
    }

    /**
     * Resuelve un valor numérico a su nombre de enum constant.
     *
     * @param parentClass Clase contenedora Godot name (ej. "BaseMaterial3D"), null si es global
     * @param enumName Nombre Godot del enum (ej. "Flags")
     * @param value Valor numérico (ej. 14)
     * @return Nombre Kotlin del constant (ej. "DISABLE_FOG") o null si no existe
     */
    fun resolveConstant(parentClass: String?, enumName: String, value: Long): String? {
        val parent = parentClass ?: GLOBAL
        return enumsByParent[parent]?.get(enumName)?.get(value)
    }

    /**
     * Obtiene todos los nombres de enum constantes de un enum.
     * @param parentClass Clase contenedora Godot name (ej. "BaseMaterial3D"), null si es global
     * @param enumName Nombre Godot del enum (ej. "Flags")
     * @return Map constant long y nombre Kotlin del constant (ej. 0 to "DISABLE_FOG")
     */
    fun getAllConstantsWithValue(parentClass: String?, enumName: String): Map<Long, String> {
        val parent = parentClass ?: GLOBAL
        return enumsByParent[parent]?.get(enumName) ?: emptyMap()
    }

    fun getAllConstantsNames(parentClass: String?, enumName: String): List<String> {
        val parent = parentClass ?: GLOBAL
        return enumsByParent[parent]?.get(enumName)?.values?.toList() ?: emptyList()
    }

    companion object {
        private const val GLOBAL = "__GLOBAL__"

        fun empty() = EnumConstantResolver(emptyMap())

        fun build(api: ExtensionApi): EnumConstantResolver {
            val map = mutableMapOf<String, MutableMap<String, MutableMap<Long, String>>>()

            fun processEnum(className: String?, enum: EnumDescriptor) {
                val targetKey = className ?: GLOBAL
                val enumMap = map.getOrPut(targetKey) { mutableMapOf() }
                    .getOrPut(enum.name) { mutableMapOf() }

                val constants = EnumeratorShortener.shortenEnumeratorNames(
                    className,
                    enum.name,
                    enum.values.map { it.name },
                )

                enum.values.zip(constants).forEach { (constant, name) ->
                    enumMap[constant.value] = name
                }
            }

            api.globalEnums.forEach { processEnum(null, it) }

            (api.builtinClasses + api.classes).forEach { cls ->
                cls.enums.forEach { processEnum(cls.name, it) }
            }

            return EnumConstantResolver(map)
        }
    }
}
