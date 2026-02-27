package io.github.kingg22.jextract.gradle.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

/**
 * DSL block for `--include-[function|constant|struct|union|typedef|var]` filters.
 *
 * When any filter is specified jextract omits all symbols not matched by at
 * least one filter.
 */
abstract class IncludesSpec @Inject constructor(objects: ObjectFactory) {
    /** `--include-function` — Function names to include. */
    val functions: ListProperty<String> = objects.listProperty(String::class.java)

    /** `--include-constant` — Constant names to include. */
    val constants: ListProperty<String> = objects.listProperty(String::class.java)

    /** `--include-struct` — Struct names to include. */
    val structs: ListProperty<String> = objects.listProperty(String::class.java)

    /** `--include-union` — Union names to include. */
    val unions: ListProperty<String> = objects.listProperty(String::class.java)

    /** `--include-typedef` — Typedef names to include. */
    val typedefs: ListProperty<String> = objects.listProperty(String::class.java)

    /** `--include-var` — Variable names to include. */
    val vars: ListProperty<String> = objects.listProperty(String::class.java)
}
