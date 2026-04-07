package io.github.kingg22.kogot.processor.bridge

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import io.github.kingg22.kogot.analysis.context.AnalysisContext
import io.github.kingg22.kogot.analysis.models.AnnotationInfo
import io.github.kingg22.kogot.analysis.models.ClassInfo
import io.github.kingg22.kogot.analysis.models.FunctionInfo
import io.github.kingg22.kogot.analysis.models.ParameterInfo
import io.github.kingg22.kogot.analysis.models.PropertyInfo
import io.github.kingg22.kogot.analysis.models.TypeInfo

/**
 * KSP-specific implementation of AnalysisContext.
 * Bridges KSP symbols to the backend-agnostic analysis layer.
 */
class KspAnalysisContext(private val resolver: Resolver) : AnalysisContext {
    override val backend: String = "ksp"

    // Simple cache for resolved classes
    private val classCache = mutableMapOf<String, ClassInfo?>()

    override fun resolveClass(qualifiedName: String): ClassInfo? {
        classCache[qualifiedName]?.let { return it }

        val ksClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString(qualifiedName))
            ?: return null.also { classCache[qualifiedName] = null }

        return ksClass.toClassInfo().also { classCache[qualifiedName] = it }
    }

    override fun getAllClasses(): List<ClassInfo> = emptyList()

    override fun isGodotBuiltinType(qualifiedName: String): Boolean =
        qualifiedName.startsWith("io.github.kingg22.godot.api.builtin.")

    override fun isValidExportType(qualifiedName: String): Boolean {
        if (qualifiedName in GodotBuiltinTypeNames.ALL) return true
        if (isGodotBuiltinType(qualifiedName)) return true
        return false
    }

    private object GodotBuiltinTypeNames {
        val ALL = listOf(
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Float",
            "kotlin.Double",
            "kotlin.Boolean",
            "kotlin.String",
            "kotlin.Byte",
            "kotlin.Short",
        )
    }
}

/**
 * Extension to convert KSClassDeclaration to ClassInfo.
 */
fun KSClassDeclaration.toClassInfo(): ClassInfo {
    val packageName = packageName.asString()
    val qName = this.qualifiedName
    val qualifiedName = qName?.asString() ?: "$packageName.${simpleName.asString()}"

    val annotations = this.annotations.map { it.toAnnotationInfo() }.toList()
    val modifiers = this.modifiers.map { it.name }.toSet()

    val supertypes = this.superTypes.mapNotNull { it.toTypeInfo() }.toList()

    val properties = this.getAllProperties().map { it.toPropertyInfo() }.toList()
    val functions = this.getAllFunctions().map { it.toFunctionInfo() }.toList()

    val filePath = this.containingFile?.filePath ?: "<unknown>"
    val line = this.getLineNumber()

    return ClassInfo(
        qualifiedName = qualifiedName,
        shortName = simpleName.asString(),
        packageName = packageName,
        supertypes = supertypes,
        properties = properties,
        functions = functions,
        annotations = annotations,
        modifiers = modifiers,
        filePath = filePath,
        lineNumber = line,
    )
}

/**
 * Gets line number from a KSNode.
 */
private fun KSNode.getLineNumber(): Int {
    val loc = this.location as? FileLocation ?: return 0
    return loc.lineNumber
}

/**
 * Extension to convert KSAnnotation to AnnotationInfo.
 */
fun KSAnnotation.toAnnotationInfo(): AnnotationInfo {
    val annotationType = annotationType.resolve()
    val decl = annotationType.declaration
    val qualifiedName = (decl as? KSClassDeclaration)?.qualifiedName?.asString() ?: "<unknown>"
    val shortName = (decl as? KSClassDeclaration)?.simpleName?.asString() ?: qualifiedName

    val arguments = this.arguments.associate { arg ->
        val name = arg.name?.asString() ?: "<unknown>"

        val value = when (val v = arg.value) {
            is KSType -> {
                val typeDecl = v.declaration
                if (typeDecl is KSClassDeclaration) {
                    typeDecl.qualifiedName?.asString() ?: "unknown"
                } else {
                    "unknown"
                }
            }

            is KSAnnotation -> v.toAnnotationInfo()

            is List<*> -> v.toString()

            else -> v
        }
        name to value
    }

    @Suppress("UNCHECKED_CAST")
    return AnnotationInfo(
        qualifiedName = qualifiedName,
        shortName = shortName,
        arguments = arguments.filterValues { it != null } as Map<String, Any>,
    )
}

/**
 * Extension to convert KSTypeReference to TypeInfo.
 */
fun KSTypeReference.toTypeInfo(): TypeInfo? {
    val type = this.resolve().takeUnless { it.isError }
    return type?.toTypeInfo()
}

/**
 * Extension to convert KSType to TypeInfo.
 */
fun KSType.toTypeInfo(): TypeInfo {
    val declaration = this.declaration
    val declQName = declaration.qualifiedName
    val qualifiedName = declQName?.asString() ?: "<unknown>"

    return TypeInfo(
        qualifiedName = qualifiedName,
        shortName = declaration.simpleName.asString(),
        isNullable = this.nullability == com.google.devtools.ksp.symbol.Nullability.NULLABLE,
        isPrimitive = false,
    )
}

/**
 * Extension to convert KSPropertyDeclaration to PropertyInfo.
 */
fun KSPropertyDeclaration.toPropertyInfo(): PropertyInfo {
    val typeInfo = type.toTypeInfo() ?: TypeInfo("<unknown>", "<unknown>")
    val annotations = this.annotations.map { it.toAnnotationInfo() }.toList()
    val modifiers = this.modifiers.map { it.name }.toSet()

    return PropertyInfo(
        name = simpleName.asString(),
        type = typeInfo,
        isMutable = this.isMutable,
        hasDefaultValue = false,
        annotations = annotations,
        modifiers = modifiers,
    )
}

/**
 * Extension to convert KSFunctionDeclaration to FunctionInfo.
 */
fun KSFunctionDeclaration.toFunctionInfo(): FunctionInfo {
    val returnTypeInfo = returnType?.toTypeInfo()
    val annotations = this.annotations.map { it.toAnnotationInfo() }.toList()
    val modifiers = this.modifiers.map { it.name }.toSet()

    val parameters = this.parameters.map { param ->
        ParameterInfo(
            name = param.name?.asString() ?: "<param>",
            type = param.type.toTypeInfo() ?: TypeInfo("<unknown>", "<unknown>"),
            hasDefaultValue = param.hasDefault,
        )
    }

    val simpleNameStr = simpleName.asString()

    return FunctionInfo(
        name = simpleNameStr,
        returnType = returnTypeInfo,
        parameters = parameters,
        annotations = annotations,
        modifiers = modifiers,
        isConstructor = isConstructor(),
    )
}
