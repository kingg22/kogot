package io.github.kingg22.godot.codegen.extensionapi

import com.squareup.kotlinpoet.TypeName
import io.github.kingg22.godot.codegen.models.extensionapi.TypeMetaHolder
import io.github.kingg22.godot.codegen.services.SimpleTypeResolver

interface TypeResolver : SimpleTypeResolver {
    @Deprecated("Use resolve(godotType, metaType) instead", level = DeprecationLevel.HIDDEN)
    override fun resolve(rawGodotType: String): TypeName =
        TODO("This should not be called, use resolve(godotType, metaType) with Context instead.")

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
        if (holder.meta == null || holder.isRequired()) return resolve(holder.type, null)
        return runCatching { resolve(holder.meta!!) }
            .onFailure {
                println(
                    "ERROR: failed to resolve type with meta: ${holder.type} (${holder.meta}).\n${it.stackTraceToString()}",
                )
            }
            .getOrDefault(resolve(holder.type))
    }
}
