package io.github.kingg22.godot.codegen.extensionapi

import io.github.kingg22.godot.codegen.extensionapi.impl.experimental.ExperimentalTypes4_6_1

class ExperimentalTypesRegistry(private val registry: Map<String, ExperimentalInfo>) {
    constructor(block: Builder.() -> Unit) : this(Builder().apply(block).registry())

    /**
     * Valida si una clase o un miembro específico es experimental.
     *
     * Uso: `isExperimental("NavigationAgent3D")` o `isExperimental("AudioStream", "generate_sample")`
     */
    fun isExperimental(className: String, memberName: String? = null): Boolean {
        val key = if (memberName == null) className else "$className.$memberName"
        return registry.containsKey(key) || (memberName != null && registry.containsKey(className))
    }

    fun getReason(className: String, memberName: String? = null): String? {
        val key = if (memberName == null) className else "$className.$memberName"
        return registry[key]?.reason ?: registry[className]?.reason
    }

    /** Representa el tipo de elemento de la API de Godot. */
    enum class GodotElementType {
        CLASS,
        METHOD,
        PROPERTY,
        CONSTANT,
        SIGNAL,
        ENUM,
    }

    /** Información sobre el estado experimental de un componente. */
    data class ExperimentalInfo(
        val type: GodotElementType,
        val className: String,
        val memberName: String? = null,
        val reason: String = "",
    )

    class Builder {
        private val registry = mutableMapOf<String, ExperimentalInfo>()

        fun addClass(name: String, reason: String = "") {
            registry[name] = ExperimentalInfo(GodotElementType.CLASS, name, null, reason)
        }

        fun addMember(
            className: String,
            memberName: String,
            type: GodotElementType,
            reason: String = "",
            getterName: String? = null,
            setterName: String? = null,
        ) {
            registry["$className.$memberName"] = ExperimentalInfo(type, className, memberName, reason)
            if (getterName != null) {
                registry["$className.$getterName"] =
                    ExperimentalInfo(GodotElementType.METHOD, className, getterName, reason)
            }
            if (setterName != null) {
                registry["$className.$setterName"] = ExperimentalInfo(GodotElementType.METHOD, className, setterName)
            }
        }

        fun registry(): Map<String, ExperimentalInfo> = registry

        fun build() = ExperimentalTypesRegistry(registry)
    }

    companion object {
        val empty = ExperimentalTypesRegistry(emptyMap())
        val v4_6_1 = ExperimentalTypes4_6_1
    }
}
