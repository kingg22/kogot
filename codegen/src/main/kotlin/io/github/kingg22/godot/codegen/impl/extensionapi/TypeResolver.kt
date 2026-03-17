package io.github.kingg22.godot.codegen.impl.extensionapi

import com.squareup.kotlinpoet.TypeName
import io.github.kingg22.godot.codegen.models.extensionapi.TypeMetaHolder

/**
 * Resolves a Godot type reference to the [TypeName] used in generated code for a specific backend.
 *
 * Each backend (Kotlin Native, Java FFM, Java JNI, …) provides its own implementation,
 * since the same Godot type may map to different platform types.
 *
 * Example mappings differ per backend:
 * ```
 * Godot "int*"  → Java FFM: MemorySegment(FFMUtils.C_INT) | Kotlin Native: CPointer<Long>
 * ```
 */
interface TypeResolver {

    /**
     * Resolves a raw Godot type string (e.g. `"int"`, `"Node"`, `"typedarray::Vector2"`)
     * to a KotlinPoet [TypeName] suitable for the target backend.
     */
    context(ctx: Context)
    fun resolve(godotType: String, metaType: String? = null): TypeName

    /**
     * Resolves a [TypeMetaHolder] (type + optional meta hint).
     *
     * The default implementation defers meta handling to subclasses:
     * if [TypeMetaHolder.isRequired] or meta is null, resolves the base type;
     * otherwise applies backend-specific meta mapping.
     */
    context(ctx: Context)
    fun resolve(holder: TypeMetaHolder): TypeName {
        if (holder.meta == null || holder.isRequired()) return resolve(holder.type)
        return runCatching { resolve(holder.meta!!) }
            .onFailure {
                println(
                    "ERROR: failed to resolve type with meta: ${holder.type} (${holder.meta}).\n${it.stackTraceToString()}",
                )
            }
            .getOrDefault(resolve(holder.type))
    }

    /**
     * Resolves a type string in the context of a builtin class member/constructor/method.
     *
     * "float" in builtin context = real_t (build-config-dependent), not GDScript float (always double).
     * Default implementation delegates to [resolve] — backends override when needed.
     */
    context(ctx: Context)
    fun resolveBuiltin(godotType: String, metaType: String? = null): TypeName = resolve(godotType, metaType)
}
