package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.builtin.internal.NoConversion
import io.github.kingg22.godot.api.builtin.internal.anyToVariant
import io.github.kingg22.godot.api.builtin.internal.convertValue
import io.github.kingg22.godot.api.builtin.internal.valueMismatch
import io.github.kingg22.godot.api.builtin.internal.value
import kotlin.reflect.KClass
import kotlin.reflect.KType

// Element codecs built at runtime from a `KType` (`typeOf<T>()`), so a collection nested in another one, or read
// from a `Variant`, keeps its element type instead of falling back to `Variant`.

/** The codec for [type], recursively for `GodotArray<X>` and `Dictionary<K, V>`; anything unknown stays a [Variant]. */
@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun codecOf(type: KType): ElementCodec<Any?> {
    val kClass = type.classifier as? KClass<*> ?: return ElementCodec.VARIANT as ElementCodec<Any?>
    val nullable = type.isMarkedNullable
    return when (kClass) {
        Variant::class -> ElementCodec.VARIANT as ElementCodec<Any?>

        GodotArray::class -> {
            val element = elementCodecOf(type, 0)
            ElementCodec(
                reader = { variant ->
                    variant.use {
                        val source = it.toArray()
                        GodotArray(source, element).also { _ -> source.close() }.checkedFromEngine()
                    }
                },
                writer = { anyToVariant(it) },
                godotType = Variant.Type.ARRAY,
            )
        }

        Dictionary::class -> {
            val key = elementCodecOf(type, 0)
            val value = elementCodecOf(type, 1)
            ElementCodec(
                reader = { variant ->
                    variant.use {
                        val source = it.toDictionary()
                        Dictionary(source, key, value).also { _ -> source.close() }.checkedFromEngine()
                    }
                },
                writer = { anyToVariant(it) },
                godotType = Variant.Type.DICTIONARY,
            )
        }

        else -> ElementCodec(
            reader = { variant -> variant.use { it.getValueDynamic(kClass, nullable) } },
            writer = { anyToVariant(it) },
            godotType = godotTypeOf(kClass, nullable),
        )
    }
}

@Suppress("UNCHECKED_CAST")
private fun elementCodecOf(type: KType, index: Int): ElementCodec<Any?> {
    val argument = type.arguments.getOrNull(index)?.type ?: return ElementCodec.VARIANT as ElementCodec<Any?>
    return codecOf(argument)
}

/** [getValueOrNull] for a type known only at runtime: the same conversions, keyed on [kClass]. */
internal fun Variant.getValueDynamic(kClass: KClass<*>, nullable: Boolean): Any? {
    val value = this.value
    val converted = convertValue(value, kClass, nullable)
    if (converted !== NoConversion) return converted

    val description = value.toString() // before closing: the wrapper was built just for this message
    (value as? AutoCloseable)?.close()
    throw valueMismatch(kClass, value, description)
}

/**
 * Reads this variant as the collection [type] (`GodotArray<X>` or `Dictionary<K, V>`) with the right element codecs.
 * Null when it is not that kind of collection, or it is typed differently in the engine.
 */
@PublishedApi
internal fun Variant.toCollectionOrNull(type: KType): Any? {
    val codec = codecOf(type)
    if (getType() != codec.godotType) return null
    return try {
        codec.read(Variant(rawPtr)) // reads a copy: the codec closes what it reads
    } catch (_: IllegalStateException) {
        null
    }
}
