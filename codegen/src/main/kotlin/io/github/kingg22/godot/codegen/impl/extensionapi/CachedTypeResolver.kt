package io.github.kingg22.godot.codegen.impl.extensionapi

import com.squareup.kotlinpoet.TypeName

/**
 * Un decorador para [TypeResolver] que añade una capa de caché
 * para evitar recalcular la resolución de tipos ya resueltos.
 */
class CachedTypeResolver(private val delegate: TypeResolver) : TypeResolver {
    private val cache = LinkedHashMap<String, TypeName>(2048)

    // computeIfAbsent realiza la lógica: si existe devuelve el valor,
    // si no, ejecuta el bloque, guarda el resultado y lo devuelve.
    context(ctx: Context)
    override fun resolve(godotType: String, metaType: String?): TypeName {
        // La clave aquí es crear una clave única compuesta si es necesario.
        // Si hay meta, incluimos la meta en la clave.
        val cacheKey = if (metaType != null) "$godotType:$metaType" else godotType
        return cache.getOrPut(cacheKey) { delegate.resolve(godotType, metaType) }
    }
}
