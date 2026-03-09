package io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver

import io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi

/**
 * Resolves enum constant names (after prefix-shortening) for a given parent class and enum name.
 *
 * ## Storage model
 * Internally the resolver stores constants as an **ordered list of (value, shortName) pairs** rather
 * than a map keyed by value.  Godot enums occasionally contain duplicate numeric values (e.g.
 * `CameraServer.FeedImage` where `FEED_RGBA_IMAGE`, `FEED_YCBCR_IMAGE`, and `FEED_Y_IMAGE` all equal 0).
 * A `Map<Long, String>` would silently overwrite earlier entries for the same value, producing fewer
 * constants than the original enum had — causing the security guard in `NativeEnumGenerator` to throw.
 *
 * The list preserves insertion order and duplicate values so that the 1-to-1 correspondence between
 * [EnumDescriptor.values] and the list returned by [getAllConstantsList] is always maintained.
 */
class EnumConstantResolver(
    // parent → enumName → ordered list of (numericValue, shortName)
    private val enumsByParent: Map<String, Map<String, List<Pair<Long, String>>>>,
) {
    init {
        println("INFO: Enum Constant Resolver started with ${enumsByParent.keys.size} entries")
    }

    /**
     * Resolves a numeric value to the **first** matching constant name.
     *
     * When multiple constants share the same value (aliases), the one declared first in the JSON is
     * returned — consistent with how Godot itself documents the primary name.
     *
     * @param parentClass Godot owner class name (e.g. `"BaseMaterial3D"`), `null` for global enums.
     * @param enumName    Short Godot enum name (e.g. `"Flags"`).
     * @param value       Numeric value to look up.
     * @return The shortened Kotlin constant name, or `null` if not found.
     */
    fun resolveConstant(parentClass: String?, enumName: String, value: Long): String? {
        val parent = parentClass ?: GLOBAL
        return enumsByParent[parent]?.get(enumName)?.firstOrNull { it.first == value }?.second
    }

    /**
     * Returns every constant name in declaration order, **including duplicates**.
     *
     * The returned list has exactly the same size as [EnumDescriptor.values], so it can be safely
     * zipped with that list position-by-position.
     */
    fun getAllConstantsNames(parentClass: String?, enumName: String): List<String> {
        val parent = parentClass ?: GLOBAL
        return enumsByParent[parent]?.get(enumName)?.map { it.second } ?: emptyList()
    }

    companion object {
        private const val GLOBAL = "__GLOBAL__"

        fun empty() = EnumConstantResolver(emptyMap())

        fun build(api: ExtensionApi): EnumConstantResolver {
            // parent → enumName → ordered list of (numericValue, shortName)
            val map = mutableMapOf<String, MutableMap<String, MutableList<Pair<Long, String>>>>()

            fun processEnum(className: String?, enum: EnumDescriptor) {
                val targetKey = className ?: GLOBAL
                val enumName = enum.shortName
                val enumList = map.getOrPut(targetKey) { mutableMapOf() }
                    .getOrPut(enumName) { mutableListOf() }

                val constants = EnumeratorShortener.shortenEnumeratorNames(
                    className,
                    enumName,
                    enum.values.map { it.name },
                )

                // Security guard: the shortener must return exactly one name per input enumerator.
                // A size mismatch here means the shortener dropped or merged constants, which would
                // later cause NativeEnumGenerator to fail with a misleading error.
                check(enum.values.size == constants.size) {
                    "EnumeratorShortener returned ${constants.size} names for enum " +
                        "'${className?.let { "$it." } ?: ""}$enumName' which has ${enum.values.size} values. " +
                        "The shortener must never drop constants.\n" +
                        "Input:  [${enum.values.joinToString { it.name }}]\n" +
                        "Output: [${constants.joinToString()}]"
                }

                // Use a list — NOT a map — so duplicate numeric values are preserved in order.
                enum.values.zip(constants) { constant, name ->
                    enumList.add(constant.value to name)
                }
            }

            api.globalEnums.forEach { enum ->
                val parentClass = enum.ownerName
                processEnum(parentClass, enum)
            }

            (api.builtinClasses + api.classes).forEach { cls ->
                cls.enums.forEach { processEnum(cls.name, it) }
            }

            return EnumConstantResolver(map)
        }
    }
}
