package io.github.kingg22.godot.codegen.models.internal

enum class BuildConfiguration(val jsonName: String, val precision: String, val pointerBits: Int) {
    FLOAT_32("float_32", "single", 32),
    FLOAT_64("float_64", "single", 64),
    DOUBLE_32("double_32", "double", 32),
    DOUBLE_64("double_64", "double", 64),
    ;

    companion object {
        fun fromJsonName(value: String): BuildConfiguration = entries.firstOrNull { it.jsonName == value }
            ?: error("Unknown build configuration '$value'")

        fun defaultFor(precision: String, pointerBits: Int): BuildConfiguration = when (precision to pointerBits) {
            "single" to 32 -> FLOAT_32
            "single" to 64 -> FLOAT_64
            "double" to 32 -> DOUBLE_32
            "double" to 64 -> DOUBLE_64
            else -> error("Unsupported precision/pointerBits combination: $precision/$pointerBits")
        }
    }
}
