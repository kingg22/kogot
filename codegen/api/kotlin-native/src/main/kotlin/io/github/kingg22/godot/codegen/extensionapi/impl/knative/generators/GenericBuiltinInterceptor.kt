package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.interceptors.ArrayGenericConfig
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.interceptors.DictionaryGenericConfig
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.interceptors.PackedListConfig
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass

/**
 * Interceptor que modifica la generación de clases builtin genéricas.
 *
 * Actúa como decorator sobre NativeBuiltinClassGenerator, interceptando
 * la construcción de clases que requieren type parameters:
 * - Array → GodotArray<@MustBeVariant T>, una `MutableList<T>` (vía `GodotList<T>`)
 * - Dictionary → Dictionary<@MustBeVariant K, @MustBeVariant V>, un `MutableMap<K, V>` (vía `GodotMap<K, V>`)
 *
 * ## Responsabilidades:
 * 1. Detectar clases genéricas (Array, Dictionary)
 * 2. Añadir TypeVariables (con su contrato `@MustBeVariant`) al TypeSpec
 * 3. Dar la clase base de la vista, los parámetros del codec de elementos y los métodos del JSON que la implementan
 * 4. Modificar tipos de retorno/parámetros para usar type variables
 * 5. Generar typealiases para versiones untyped
 */
class GenericBuiltinInterceptor {

    /** Detecta si una clase builtin debe ser genérica. */
    fun requiresGenerics(builtinClass: BuiltinClass): Boolean = when (builtinClass.name) {
        "Array" -> true
        "Dictionary" -> true
        else -> false
    }

    /** Obtiene la configuración de genéricos para una clase. */
    fun getGenericConfig(builtinClass: BuiltinClass): GenericConfig? = when (builtinClass.name) {
        "Array" -> ArrayGenericConfig()
        "Dictionary" -> DictionaryGenericConfig()
        else -> PackedListConfig.PACKED_ELEMENTS[builtinClass.name]?.let(::PackedListConfig)
    }

    /** Configuración de genéricos para una clase builtin. */
    interface GenericConfig {
        /** Nombres de los type variables de la clase (ej: `T`, o `K` y `V`). */
        val typeVariableNames: List<String>
            get() = emptyList()

        /** Type variables a añadir a la clase (ej: T, K, V), con su contrato `@MustBeVariant`. */
        val typeVariables: List<TypeVariableName>
            get() = typeVariableNames.map(::contractedTypeVariable)

        /** Typealias para versión untyped (ej: VariantArray = GodotArray<Variant>) */
        context(context: Context)
        val untypedAlias: Pair<String, ParameterizedTypeName>?
            get() = null

        /** Modifica el tipo de retorno de un método si usa type variables */
        context(context: Context)
        fun transformReturnType(method: BuiltinClass.BuiltinMethod, originalType: TypeName?): TypeName? = originalType

        /** Modifica el tipo de un parámetro si usa type variables */
        context(context: Context)
        fun transformParameterType(
            method: BuiltinClass.BuiltinMethod,
            argIndex: Int,
            originalType: TypeName,
        ): TypeName = originalType

        /** Modifica el tipo de retorno de un operator si usa type variables */
        context(context: Context)
        fun transformOperatorReturnType(operator: BuiltinClass.Operator, originalType: TypeName): TypeName =
            originalType

        /**
         * Clase base escrita a mano que da a la clase generada su vista de colección de Kotlin (`GodotList<T>`,
         * `GodotMap<K, V>`), o null si no tiene.
         */
        context(context: Context)
        val superclass: TypeName?
            get() = null

        /**
         * Parámetros del codec de elementos que reciben el constructor primario y cada constructor secundario, en el
         * orden en que la superclase los espera (`codec`, o `keyCodec` y `valueCodec`).
         */
        context(context: Context)
        val codecParameters: List<ParameterSpec>
            get() = emptyList()

        /**
         * Métodos del JSON que implementan los miembros abstractos de [superclass]: nombre del JSON → nombre en Kotlin.
         * Se generan con `override`, sin valores por defecto y sin `operator`.
         */
        val viewMethods: Map<String, String>
            get() = emptyMap()

        /**
         * Tipo Kotlin de los elementos si la clase es una `MutableList` de tamaño fijo en tipo (los `Packed*Array`):
         * el generador añade los miembros de `List` que puentean los métodos del JSON. Null si no lo es.
         */
        context(context: Context)
        val listElementType: TypeName?
            get() = null
    }

    companion object {
        /**
         * `@MustBeVariant [name]`: the contract the compiler plugin enforces where the container is built or written down.
         *
         * KotlinPoet only emits TypeVariableName annotations at use sites, but the compiler plugin reads them from the
         * declaration (`class GodotArray<@MustBeVariant T>`). Prefixing the name emits it in the declaration; no import is
         * needed because MustBeVariant lives in the same package as the generated builtins.
         */
        fun contractedTypeVariable(name: String): TypeVariableName = TypeVariableName("@MustBeVariant $name")
    }
}
