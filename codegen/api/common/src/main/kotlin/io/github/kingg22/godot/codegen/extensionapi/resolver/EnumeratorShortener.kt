package io.github.kingg22.godot.codegen.extensionapi.resolver

object EnumeratorShortener {

    /**
     * Shortens a list of Godot enumerator names by stripping their common prefix.
     *
     * ## Contract (invariant)
     * The returned list **always** has the same size as [enumerators].  The shortener never drops,
     * merges, or reorders constants — it only strips prefixes.  Callers (e.g. [EnumConstantResolver])
     * rely on position-based correspondence between the input and output lists.
     *
     * ## Algorithm
     * 1. If a hardcoded mapping exists for `(godotClassName, godotEnumName)`, apply it.
     * 2. Otherwise, derive the longest common underscore-delimited prefix shared by **all** names
     *    and strip it, falling back to the original name if stripping would produce an invalid
     *    Kotlin identifier (one that starts with a digit).
     * 3. The `_MAX` / `_COUNT` sentinel convention: if the **last** enumerator ends with `_MAX` or
     *    `_COUNT` *and* its stripped form would be just `"MAX"` or `"COUNT"` after applying the
     *    common prefix, we keep that result.
     *
     * @param godotClassName Class owner (`null` for global enums).
     * @param godotEnumName  Short enum name (already mapped/renamed).
     * @param enumerators    Raw Godot enumerator names (e.g. `["KEY_NONE", "KEY_ESCAPE"]`).
     * @return Shortened names in the same order as [enumerators], same size.
     */
    fun shortenEnumeratorNames(
        godotClassName: String?,
        godotEnumName: String,
        enumerators: List<String>,
    ): List<String> {
        // 1. Hardcoded exceptions
        val hardcodedPrefixes = reduceHardcodedPrefix(godotClassName, godotEnumName)
        if (hardcodedPrefixes != null) {
            return enumerators.map { tryStripPrefixes(it, hardcodedPrefixes) }
        }

        if (enumerators.size <= 1) {
            return enumerators
        }

        // 2. Find the longest common underscore-delimited prefix using a heuristic
        val original = enumerators[0]
        var (longestPrefix, pos) = enumeratorPrefix(original, original.length) ?: return enumerators

        // Narrow the prefix by comparing with every other enumerator
        for (i in 1 until enumerators.size) {
            val e = enumerators[i]
            while (!e.startsWith(longestPrefix)) {
                val nextPrefix = enumeratorPrefix(original, pos - 1)
                if (nextPrefix != null) {
                    longestPrefix = nextPrefix.first
                    pos = nextPrefix.second
                } else {
                    pos = 0
                    break
                }
            }
            if (pos == 0) break
        }

        // 3. Apply trimming with final validations
        return enumerators.map { e ->
            if (pos == 0) return@map e

            var localPos = pos
            // If the result would start with a digit, back up to the previous segment
            // (e.g. SOURCE_3D → "3D" is invalid; keep "SOURCE_3D" instead)
            while (startsWithInvalidChar(e.substring(localPos))) {
                if (localPos <= 0) break
                val prevUnderscore = e.lastIndexOf('_', localPos - 2)
                localPos = if (prevUnderscore != -1) prevUnderscore + 1 else 0
            }

            val shortened = e.substring(localPos)

            // Sentinel convention: the last enumerator is often a MAX/COUNT guard whose
            // stripped form is naturally "MAX" or "COUNT" — keep it.  We intentionally do NOT
            // force any name to "MAX" here; the correct short name is whatever stripping produces.
            // Previously this block unconditionally returned "MAX" for *any* last element ending
            // in "_MAX" or "_COUNT", which corrupted names like "FEED_CBCR_IMAGE" → "MAX".
            shortened
        }
    }

    private fun reduceHardcodedPrefix(className: String?, enumName: String): Array<String>? = when {
        className == null && enumName == "Key" -> arrayOf("KEY_")

        className == null && enumName == "Error" -> arrayOf("ERR_")

        (className == "RenderingServer" || className == "Mesh") &&
            enumName == "ArrayFormat" -> arrayOf("ARRAY_FORMAT_", "ARRAY_")

        className == "AudioServer" && enumName == "SpeakerMode" -> arrayOf("SPEAKER_MODE_", "SPEAKER_")

        className == "ENetConnection" && enumName == "HostStatistic" -> arrayOf("HOST_")

        className == null && enumName == "MethodFlags" -> arrayOf("METHOD_FLAG_", "METHOD_FLAGS_")

        className == null && enumName == "KeyModifierMask" -> arrayOf("KEY_MASK_", "KEY_")

        className == "RenderingDevice" && enumName == "StorageBufferUsage" -> arrayOf("STORAGE_BUFFER_USAGE_")

        enumName == "PathfindingAlgorithm" -> arrayOf("PATHFINDING_ALGORITHM_")

        else -> null
    }

    private fun tryStripPrefixes(enumerator: String, prefixes: Array<String>): String {
        for (prefix in prefixes) {
            val stripped = enumerator.removePrefix(prefix)
            // Only accept the stripped form if the prefix was actually present AND the result
            // is a valid Kotlin identifier (does not start with a digit).
            if (stripped !== enumerator && !startsWithInvalidChar(stripped)) {
                return stripped
            }
        }
        return enumerator
    }

    /** Returns the longest underscore-terminated prefix of [s] up to [maxPos]. */
    private fun enumeratorPrefix(s: String, maxPos: Int): Pair<String, Int>? {
        if (maxPos <= 0) return null
        val sub = s.substring(0, maxPos)
        val lastUnderscore = sub.lastIndexOf('_')
        if (lastUnderscore == -1) return null

        val pos = lastUnderscore + 1
        return s.substring(0, pos) to pos
    }

    // Kotlin identifiers cannot start with a digit
    private fun startsWithInvalidChar(s: String): Boolean {
        if (s.isEmpty()) return true
        return s[0].isDigit()
    }
}
