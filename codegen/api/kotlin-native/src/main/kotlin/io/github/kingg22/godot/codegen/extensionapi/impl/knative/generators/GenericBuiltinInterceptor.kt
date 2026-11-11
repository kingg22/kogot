package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver
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
class GenericBuiltinInterceptor(private val typeResolver: TypeResolver) {

    /** Detecta si una clase builtin debe ser genérica. */
    fun requiresGenerics(builtinClass: BuiltinClass): Boolean = when (builtinClass.name) {
        "Array" -> true
        "Dictionary" -> true
        else -> false
    }

    /** Obtiene la configuración de genéricos para una clase. */
    context(_: Context)
    fun getGenericConfig(builtinClass: BuiltinClass): GenericConfig? = when (builtinClass.name) {
        "Array" -> ArrayGenericConfig(builtinClass, typeResolver)
        "Dictionary" -> DictionaryGenericConfig()
        else -> PACKED_ELEMENTS[builtinClass.name]?.let(::PackedListConfig)
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

    /** `Packed*Array` as `AbstractMutableList<E>` + `RandomAccess`: not generic, the element type is fixed. */
    private class PackedListConfig(private val element: String) : GenericConfig {
        context(context: Context)
        override val listElementType: TypeName get() = packedElementType(element)

        context(context: Context)
        override val superclass: TypeName
            get() = ClassName("kotlin.collections", "AbstractMutableList").parameterizedBy(listElementType)

        // Abstract members of AbstractMutableList that the engine methods implement; `size` is bridged by hand.
        override val viewMethods: Map<String, String> = mapOf("is_empty" to "isEmpty", "clear" to "clear")
    }

    private companion object {
        /** `Packed*Array` name → the element type its `get` returns. */
        val PACKED_ELEMENTS = mapOf(
            "PackedByteArray" to "Long",
            "PackedInt32Array" to "Long",
            "PackedInt64Array" to "Long",
            "PackedFloat32Array" to "Double",
            "PackedFloat64Array" to "Double",
            "PackedStringArray" to "String",
            "PackedVector2Array" to "Vector2",
            "PackedVector3Array" to "Vector3",
            "PackedColorArray" to "Color",
            "PackedVector4Array" to "Vector4",
        )
    }

    private class ArrayGenericConfig(private val builtinClass: BuiltinClass, private val typeResolver: TypeResolver) :
        GenericConfig {
        override val typeVariableNames: List<String> = listOf("T")

        context(context: Context)
        override val superclass: TypeName get() = collectionBase("GodotList", "T")

        context(context: Context)
        override val codecParameters: List<ParameterSpec> get() = listOf(codecParameter("codec", "T"))

        override val viewMethods: Map<String, String> = mapOf(
            "size" to "size",
            "is_empty" to "isEmpty",
            "clear" to "clear",
            "get" to "getVariant",
            "set" to "setVariant",
            "insert" to "insert",
            "remove_at" to "removeAt",
        )

        context(context: Context)
        override val untypedAlias: Pair<String, ParameterizedTypeName>
            get() {
                val godotArrayClass = context.classNameForOrDefault("Array", "GodotArray")
                val variantClass = context.classNameForOrDefault("Variant")
                val untypedArray = godotArrayClass.parameterizedBy(variantClass)
                return "VariantArray" to untypedArray
            }

        // NO transformar tipos de parámetros - Array<T> internamente usa Variant
        // Los factory methods (ofInt, ofLong, etc.) se generan aparte
    }

    private class DictionaryGenericConfig : GenericConfig {
        override val typeVariableNames: List<String> = listOf("K", "V")

        context(context: Context)
        override val superclass: TypeName get() = collectionBase("GodotMap", "K", "V")

        context(context: Context)
        override val codecParameters: List<ParameterSpec>
            get() = listOf(codecParameter("keyCodec", "K"), codecParameter("valueCodec", "V"))

        override val viewMethods: Map<String, String> = mapOf(
            "size" to "size",
            "is_empty" to "isEmpty",
            "clear" to "clear",
            "has" to "has",
            "erase" to "erase",
            "get" to "getVariant",
            "set" to "setVariant",
            "keys" to "keys",
        )

        context(context: Context)
        override val untypedAlias: Pair<String, ParameterizedTypeName>
            get() {
                val godotDictClass = context.classNameForOrDefault("Dictionary", typedClass = true)
                val variantClass = context.classNameForOrDefault("Variant")
                val untypedDict = godotDictClass.parameterizedBy(variantClass, variantClass)
                return "VariantDictionary" to untypedDict
            }

        // NO transformar tipos de parámetros - Dictionary<K, V> internamente usa Variant
        // Los factory methods (ofIntInt, etc.) se generan aparte
    }
}

/**
 * `@MustBeVariant [name]`: the contract the compiler plugin enforces where the container is built or written down.
 *
 * KotlinPoet only emits TypeVariableName annotations at use sites, but the compiler plugin reads them from the
 * declaration (`class GodotArray<@MustBeVariant T>`). Prefixing the name emits it in the declaration; no import is
 * needed because MustBeVariant lives in the same package as the generated builtins.
 */
internal fun contractedTypeVariable(name: String): TypeVariableName = TypeVariableName("@MustBeVariant $name")

/** A parameter named [name] of type `ElementCodec<[element]>`. */
context(context: Context)
private fun codecParameter(name: String, element: String): ParameterSpec {
    val codec = ClassName(context.classNameForOrDefault("Variant").packageName, "ElementCodec")
    return ParameterSpec.builder(name, codec.parameterizedBy(TypeVariableName(element))).build()
}

/** The hand-written collection view [name] over [elements], in the builtin package. */
context(context: Context)
private fun collectionBase(name: String, vararg elements: String): TypeName =
    ClassName(context.classNameForOrDefault("Variant").packageName, name)
        .parameterizedBy(elements.map { TypeVariableName(it) })

context(context: Context)
private fun packedElementType(name: String): TypeName = when (name) {
    "Long" -> LONG
    "Double" -> DOUBLE
    "String" -> STRING
    else -> context.classNameForOrDefault(name)
}
