package io.github.kingg22.godot.codegen.impl.extensionapi.knative.generators

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass

/**
 * Interceptor que modifica la generación de clases builtin genéricas.
 *
 * Actúa como decorator sobre NativeBuiltinClassGenerator, interceptando
 * la construcción de clases que requieren type parameters:
 * - Array → Array<T>
 * - Dictionary → Dictionary<K, V> (futuro)
 *
 * ## Responsabilidades:
 * 1. Detectar clases genéricas (Array, Dictionary)
 * 2. Añadir TypeVariables al TypeSpec
 * 3. Modificar tipos de retorno/parámetros para usar type variables
 * 4. Generar typealiases para versiones untyped
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
        else -> null
    }

    /** Configuración de genéricos para una clase builtin. */
    interface GenericConfig {
        /** Type variables a añadir a la clase (ej: T, K, V) */
        context(context: Context)
        val typeVariables: List<TypeVariableName>
            get() = emptyList()

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
    }

    private class ArrayGenericConfig(private val builtinClass: BuiltinClass, private val typeResolver: TypeResolver) :
        GenericConfig {
        context(_: Context)
        private val typeT get() = TypeVariableName.invoke("T")

        context(context: Context)
        override val typeVariables: List<TypeVariableName> get() = listOf(typeT)

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
        context(_: Context)
        private val typeKeys get() = TypeVariableName.invoke("K")

        context(_: Context)
        private val typeValues get() = TypeVariableName.invoke("V")

        context(context: Context)
        override val typeVariables: List<TypeVariableName> get() = listOf(typeKeys, typeValues)

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
