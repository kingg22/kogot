package io.github.kingg22.godot.codegen.impl.extensionapi

import com.squareup.kotlinpoet.TypeName
import io.github.kingg22.godot.codegen.models.extensionapi.TypeMetaHolder

/**
 * Un decorador para [TypeResolver] que añade una capa de caché
 * para evitar recalcular la resolución de tipos ya resueltos.
 */
class CachedTypeResolver(private val delegate: TypeResolver) : TypeResolver {
    private val cache = LinkedHashMap<String, TypeName>(2048)

    // computeIfAbsent realiza la lógica: si existe devuelve el valor,
    // si no, ejecuta el bloque, guarda el resultado y lo devuelve.
    context(_: Context)
    override fun resolve(godotType: String): TypeName = cache.getOrPut(godotType) { delegate.resolve(godotType) }

    context(_: Context)
    override fun resolve(holder: TypeMetaHolder): TypeName {
        // La clave aquí es crear una clave única compuesta si es necesario.
        // Si el holder tiene meta, incluimos la meta en la clave.
        val cacheKey = if (holder.meta != null) "${holder.type}:${holder.meta}" else holder.type
        return cache.getOrPut(cacheKey) { delegate.resolve(holder) }
    }
}
