package io.github.kingg22.godot.codegen.extensionapi

import com.squareup.kotlinpoet.TypeName
import io.github.kingg22.godot.codegen.models.extensionapi.TypeMetaHolder

/**
 * Un decorador para [TypeResolver] que añade una capa de caché
 * para evitar recalcular la resolución de tipos ya resueltos.
 */
class CachedTypeResolver(private val delegate: TypeResolver) : TypeResolver {
    private val cache = LinkedHashMap<String, TypeName>(2048)

    // La clave aquí es crear una clave única compuesta si es necesario.
    // Si hay meta, incluimos la meta en la clave.
    @Suppress("NOTHING_TO_INLINE")
    private inline fun computeKey(godotType: String, metaType: String?): String = "$godotType:$metaType"

    // computeIfAbsent realiza la lógica: si existe devuelve el valor,
    // si no, ejecuta el bloque, guarda el resultado y lo devuelve.
    context(ctx: Context)
    override fun resolve(godotType: String, metaType: String?): TypeName = cache.getOrPut(
        computeKey(godotType, metaType),
    ) { delegate.resolve(godotType, metaType) }

    context(ctx: Context)
    override fun resolve(holder: TypeMetaHolder): TypeName = cache.getOrPut(computeKey(holder.type, holder.meta)) {
        delegate.resolve(holder)
    }
}
