package io.github.kingg22.godot.codegen.utils

fun <K, V> Map<K, V?>.filterValuesNotNull(): Map<K, V> {
    @Suppress("UNCHECKED_CAST")
    return filterValues { it != null } as Map<K, V>
}

inline fun <T> withExceptionContext(metadata: () -> String, block: () -> T): T = try {
    block()
} catch (e: Exception) {
    throw RuntimeException(metadata(), e)
}
