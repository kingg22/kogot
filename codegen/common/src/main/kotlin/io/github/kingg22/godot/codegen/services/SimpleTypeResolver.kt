package io.github.kingg22.godot.codegen.services

import com.squareup.kotlinpoet.TypeName

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
interface SimpleTypeResolver {
    /**
     * Resolves a [raw Godot type string][rawGodotType] (e.g. `"int"`, `"Node"`, `"typedarray::Vector2"`)
     * to a KotlinPoet [TypeName] suitable for the target backend.
     */
    fun resolve(rawGodotType: String): TypeName
}
