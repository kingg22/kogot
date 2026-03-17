package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

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
        "Dictionary" -> DictionaryGenericConfig(builtinClass, typeResolver)
        else -> null
    }

    /** Configuración de genéricos para una clase builtin. */
    interface GenericConfig {
        /** Type variables a añadir a la clase (ej: T, K, V) */
        val typeVariables: List<TypeVariableName>

        /** Typealias para versión untyped (ej: VariantArray = GodotArray<Variant>) */
        context(context: Context)
        val untypedAlias: Pair<String, ParameterizedTypeName>?

        /** Modifica el tipo de retorno de un método si usa type variables */
        context(context: Context)
        fun transformReturnType(method: BuiltinClass.BuiltinMethod, originalType: TypeName?): TypeName?

        /** Modifica el tipo de un parámetro si usa type variables */
        context(context: Context)
        fun transformParameterType(method: BuiltinClass.BuiltinMethod, argIndex: Int, originalType: TypeName): TypeName

        /** Modifica el tipo de retorno de un operator si usa type variables */
        context(context: Context)
        fun transformOperatorReturnType(operator: BuiltinClass.Operator, originalType: TypeName): TypeName
    }

    private class ArrayGenericConfig(private val builtinClass: BuiltinClass, private val typeResolver: TypeResolver) :
        GenericConfig {
        private val typeT = TypeVariableName("T")

        override val typeVariables: List<TypeVariableName> = listOf(typeT)

        context(context: Context)
        override val untypedAlias: Pair<String, ParameterizedTypeName>
            get() {
                val godotArrayClass = context.classNameForOrDefault("Array", "GodotArray")
                val variantClass = context.classNameForOrDefault("Variant")
                val untypedArray = godotArrayClass.parameterizedBy(variantClass)
                return "VariantArray" to untypedArray
            }

        context(context: Context)
        override fun transformReturnType(method: BuiltinClass.BuiltinMethod, originalType: TypeName?): TypeName? {
            // Si el método retorna el indexing_return_type, usar T
            if (originalType == null) return null

            val indexingType = builtinClass.indexingReturnType
            if (indexingType != null && method.name == "get") {
                val indexingTypeName = typeResolver.resolve(indexingType)
                if (originalType == indexingTypeName) {
                    return typeT
                }
            }

            // Métodos que retornan Array self-type → Array<T>
            if (method.returnType == "Array") {
                val godotArrayClass = context.classNameForOrDefault("Array", "GodotArray", typedClass = true)
                return godotArrayClass.parameterizedBy(typeT)
            }

            return originalType
        }

        context(context: Context)
        override fun transformParameterType(
            method: BuiltinClass.BuiltinMethod,
            argIndex: Int,
            originalType: TypeName,
        ): TypeName {
            // Si el parámetro es del indexing_return_type, usar T
            val indexingType = builtinClass.indexingReturnType
            if (indexingType != null && method.name == "set" && argIndex == 1) {
                val indexingTypeName = typeResolver.resolve(indexingType)
                if (originalType == indexingTypeName) {
                    return typeT
                }
            }

            // Parámetros que aceptan Array → Array<T>
            val arg = method.arguments.getOrNull(argIndex)
            if (arg?.type == "Array") {
                val godotArrayClass = context.classNameForOrDefault("Array", "GodotArray", typedClass = true)
                return godotArrayClass.parameterizedBy(typeT)
            }

            return originalType
        }

        context(context: Context)
        override fun transformOperatorReturnType(operator: BuiltinClass.Operator, originalType: TypeName): TypeName {
            // Operators que retornan Array self-type → Array<T>
            if (operator.returnType == "Array") {
                val godotArrayClass = context.classNameForOrDefault("Array", "GodotArray", typedClass = true)
                return godotArrayClass.parameterizedBy(typeT)
            }

            return originalType
        }
    }

    private class DictionaryGenericConfig(
        private val builtinClass: BuiltinClass,
        private val typeResolver: TypeResolver,
    ) : GenericConfig {
        private val typeKeys = TypeVariableName("K")
        private val typeValues = TypeVariableName("V")

        override val typeVariables: List<TypeVariableName> = listOf(typeKeys, typeValues)

        context(context: Context)
        override val untypedAlias: Pair<String, ParameterizedTypeName>
            get() {
                val godotDictClass = context.classNameForOrDefault("Dictionary", typedClass = true)
                val variantClass = context.classNameForOrDefault("Variant")
                val untypedDict = godotDictClass.parameterizedBy(variantClass, variantClass)
                return "VariantDictionary" to untypedDict
            }

        context(context: Context)
        override fun transformReturnType(method: BuiltinClass.BuiltinMethod, originalType: TypeName?): TypeName? {
            if (originalType == null) return null

            // Dictionary.get(key) → V (indexing_return_type)
            // Dictionary usa is_keyed = true, entonces get() retorna el value type
            val indexingType = builtinClass.indexingReturnType
            if (indexingType != null && method.name == "get") {
                val indexingTypeName = typeResolver.resolve(indexingType)
                if (originalType == indexingTypeName) {
                    return typeValues
                }
            }

            // Métodos que retornan Dictionary self-type → Dictionary<K, V>
            if (method.returnType == "Dictionary") {
                val godotDictClass = context.classNameForOrDefault("Dictionary", typedClass = true)
                return godotDictClass.parameterizedBy(typeKeys, typeValues)
            }

            return originalType
        }

        context(context: Context)
        override fun transformParameterType(
            method: BuiltinClass.BuiltinMethod,
            argIndex: Int,
            originalType: TypeName,
        ): TypeName {
            // Dictionary.get(key: Variant) → Dictionary.get(key: K)
            // Dictionary.set(key: Variant, value: Variant) → Dictionary.set(key: K, value: V)
            // Dictionary.has(key: Variant) → Dictionary.has(key: K)

            val arg = method.arguments.getOrNull(argIndex) ?: return originalType

            // Primer parámetro (key) → K
            if (argIndex == 0 && arg.type == "Variant") {
                when (method.name) {
                    "get", "set", "has", "erase", "get_or_add" -> return typeKeys
                }
            }

            // Segundo parámetro (value) de set → V
            if (argIndex == 1 && arg.type == "Variant" && method.name == "set") {
                return typeValues
            }

            // Parámetros que aceptan Dictionary → Dictionary<K, V>
            if (arg.type == "Dictionary") {
                val godotDictClass = context.classNameForOrDefault("Dictionary", typedClass = true)
                return godotDictClass.parameterizedBy(typeKeys, typeValues)
            }

            return originalType
        }

        context(context: Context)
        override fun transformOperatorReturnType(operator: BuiltinClass.Operator, originalType: TypeName): TypeName {
            // Operators que retornan Dictionary self-type → Dictionary<K, V>
            if (operator.returnType == "Dictionary") {
                val godotDictClass = context.classNameForOrDefault("Dictionary", typedClass = true)
                return godotDictClass.parameterizedBy(typeKeys, typeValues)
            }

            return originalType
        }
    }
}
