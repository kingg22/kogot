package io.github.kingg22.godot.codegen.runtime.impl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.godot.codegen.impl.snakeCaseToPascalCase
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import io.github.kingg22.godot.codegen.models.extensioninterface.Interface
import io.github.kingg22.godot.codegen.models.extensioninterface.Types
import io.github.kingg22.godot.codegen.models.extensioninterface.ValueType
import io.github.kingg22.godot.codegen.services.SimpleTypeResolver
import io.github.kingg22.godot.codegen.types.*

class RuntimeTypeResolver(interfaceModel: GDExtensionInterface, packageName: String) : SimpleTypeResolver {
    private val ffiPackage = "$packageName.internal.ffi"
    private val typesByName = interfaceModel.types.associateBy(Types::name)

    fun resolve(returnValue: ValueType): TypeName = resolve(returnValue.type)

    override fun resolve(rawGodotType: String): TypeName {
        val (baseType, pointerCount) = ParsedType(rawGodotType)
        return when {
            pointerCount == 0 -> resolveDirectType(baseType)
            baseType == "void" -> COPAQUE_POINTER.copy(nullable = true)
            else -> C_POINTER.parameterizedBy(resolvePointerTarget(baseType)).copy(nullable = true)
        }
    }

    fun resolveInterfaceAlias(iface: Interface): ClassName = ClassName(
        ffiPackage,
        iface.legacyTypeName ?: "GDExtensionInterface${iface.name.snakeCaseToPascalCase()}",
    )

    private fun resolveDirectType(baseType: String): TypeName {
        val ffiType = typesByName[baseType]
        if (ffiType != null) {
            val typeName = ClassName(ffiPackage, baseType)
            return when (ffiType) {
                is Types.FunctionType,
                is Types.HandleType,
                -> typeName.copy(nullable = true)

                else -> typeName
            }
        }

        return when (baseType) {
            "void" -> UNIT
            "char" -> BYTE_VAR
            "int8_t", "int8" -> BYTE_VAR
            "char16_t" -> ClassName(ffiPackage, "char16_t")
            "char32_t" -> ClassName(ffiPackage, "char32_t")
            "short", "int16_t", "int16" -> INT16_T
            "int", "int32_t", "int32" -> INT32_T
            "long long", "int64_t", "int64" -> INT64_T
            "uint8_t" -> UINT8_T
            "uint16_t" -> UINT16_T
            "uint32_t" -> UINT32_T
            "uint64_t" -> UINT64_T
            "size_t" -> SIZE_T
            "float" -> POSIX_FLOAT
            "double" -> POSIX_DOUBLE
            "wchar_t" -> POSIX_WCHAR_T
            else -> error("Unsupported runtime type: '$baseType'")
        }
    }

    private fun resolvePointerTarget(baseType: String): TypeName {
        val ffiType = typesByName[baseType]
        if (ffiType != null) {
            return when (ffiType) {
                is Types.StructType -> ClassName(ffiPackage, baseType)

                is Types.AliasType ->
                    if (resolvesToStruct(ffiType.type)) {
                        ClassName(ffiPackage, baseType)
                    } else {
                        ClassName(ffiPackage, "${baseType}Var")
                    }

                else -> ClassName(ffiPackage, "${baseType}Var")
            }
        }

        return when (baseType) {
            "char", "int8_t", "int8" -> BYTE_VAR
            "char16_t" -> ClassName(ffiPackage, "char16_tVar")
            "char32_t" -> ClassName(ffiPackage, "char32_tVar")
            "short", "int16_t", "int16" -> SHORT_VAR
            "int", "int32_t", "int32" -> INT_VAR
            "long long", "int64_t", "int64" -> LONG_VAR
            "uint8_t" -> U_BYTE_VAR
            "uint16_t" -> U_SHORT_VAR
            "uint32_t" -> U_INT_VAR
            "uint64_t" -> U_LONG_VAR
            "float" -> FLOAT_VAR
            "double" -> DOUBLE_VAR
            "wchar_t" -> ClassName("platform.posix", "wchar_tVar")
            else -> error("Unsupported runtime pointer target: '$baseType'")
        }
    }

    private fun resolvesToStruct(typeName: String): Boolean {
        val (normalized) = ParsedType(typeName)
        return when (val type = typesByName[normalized]) {
            is Types.StructType -> true
            is Types.AliasType -> resolvesToStruct(type.type)
            else -> false
        }
    }

    // Change to data class when Flexible constructor is available https://youtrack.jetbrains.com/issue/KT-81919
    private class ParsedType {
        val baseType: String
        val pointerCount: Int

        constructor(rawType: String) {
            var normalized = rawType.trim()
            normalized = normalized.removePrefix("const ").trim()

            var pointerCount = 0
            while (normalized.endsWith("*")) {
                pointerCount++
                normalized = normalized.dropLast(1).trimEnd()
            }

            baseType = normalized.removePrefix("const ").trim()
            this.pointerCount = pointerCount
        }

        operator fun component1() = baseType
        operator fun component2() = pointerCount
    }
}
