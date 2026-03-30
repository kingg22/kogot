package io.github.kingg22.godot.codegen.impl.extensionapi.knative.resolver

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
 * [EnumDescriptor.values] and the list returned by [getAllConstantsNames] is always maintained.
 *
 * ## Alias semantics (duplicate numeric values)
 * Godot C enums may declare multiple names for the same integer value — these are **intentional aliases**,
 * not errors.  Examples:
 * - `CameraServer.FeedImage`: `FEED_RGBA_IMAGE = 0`, `FEED_YCBCR_IMAGE = 0`, `FEED_Y_IMAGE = 0`
 *   (three names for "the first available feed format depending on camera driver").
 *
 * In Kotlin enum classes every entry is a distinct object even if two entries share the same `value: Long`.
 * This is fine and intentional — it models the C source faithfully.  The consequence for code generation is:
 *
 * - **Default values**
 *   ([io.github.kingg22.godot.codegen.impl.extensionapi.knative.generators.DefaultValueGenerator.parseEnumFromValue])
 *   when Godot JSON says `"default_value": "0"` for type
 *   `enum::CameraServer.FeedImage`, *any* alias with value 0 produces correct runtime behaviour because
 *   they are all the same integer.  We emit the **first-declared** alias (canonical Godot order) via
 *   [resolveConstant].
 *
 * - **Indexed property indices**
 *   ([io.github.kingg22.godot.codegen.impl.extensionapi.knative.generators.NativeEngineClassGenerator.resolveIndexedPropertyConstant]):
 *   the `index` field of a Godot
 *   `ClassProperty` is a raw integer passed verbatim to the getter/setter.  When it maps to an enum
 *   with aliases, the same "first-declared wins" rule is applied.  Use [resolveConstantUnambiguous]
 *   in that call-site to get a warning logged whenever an alias is silently selected, making future
 *   surprises visible during generation.
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
     * returned — consistent with how Godot itself documents the primary name.  This is the right
     * choice for **default value generation**: all aliases are runtime-equivalent, so emitting the
     * first-declared one is both correct and stable across Godot releases.
     *
     * If you need to know whether the resolution was ambiguous (multiple aliases), use
     * [resolveConstantUnambiguous] instead.
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
     * Resolves a numeric value to its constant name, logging a warning when multiple aliases share
     * that value.
     *
     * Use this instead of [resolveConstant] in call-sites where ambiguity is **semantically surprising**
     * — primarily [io.github.kingg22.godot.codegen.impl.extensionapi.knative.generators.NativeEngineClassGenerator.resolveIndexedPropertyConstant],
     * where the index is a raw integer passed to a
     * getter/setter and the choice of alias may affect readability or correctness of the generated code.
     *
     * The returned name is always the first-declared alias (same as [resolveConstant]); the warning
     * is purely informational for the developer running the generator.
     *
     * @param parentClass Godot owner class name, `null` for global enums.
     * @param enumName    Short Godot enum name.
     * @param value       Numeric value to look up.
     * @param context     Human-readable description of the call-site, included in the warning message.
     * @return The shortened Kotlin constant name, or `null` if not found.
     */
    fun resolveConstantUnambiguous(
        parentClass: String?,
        enumName: String,
        value: Long,
        context: String = "",
    ): String? {
        val parent = parentClass ?: GLOBAL
        val matches = enumsByParent[parent]?.get(enumName)?.filter { it.first == value } ?: return null
        if (matches.size > 1) {
            val qualified = "${parentClass?.let { "$it." } ?: ""}$enumName"
            val aliases = matches.joinToString { it.second }
            println(
                "INFO: Enum '$qualified' has ${matches.size} aliases for value $value: [$aliases]. " +
                    "Emitting first-declared '${matches.first().second}'." +
                    if (context.isNotBlank()) " Context: $context" else "",
            )
        }
        return matches.firstOrNull()?.second
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
                val enumValues = enum.values.zip(constants) { constant, name ->
                    constant.value to name
                }
                enumList.addAll(enumValues)
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
