package io.github.kingg22.kogot.processor.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Nullability
import io.github.kingg22.kogot.processor.model.AnnotationInfo
import io.github.kingg22.kogot.processor.model.ClassInfo
import io.github.kingg22.kogot.processor.model.FunctionInfo
import io.github.kingg22.kogot.processor.model.ParameterInfo
import io.github.kingg22.kogot.processor.model.PropertyInfo
import io.github.kingg22.kogot.processor.model.SignalDeclaration
import io.github.kingg22.kogot.processor.model.SignalParameterInfo
import io.github.kingg22.kogot.processor.model.TypeInfo
import io.github.kingg22.kogot.processor.model.VirtualMethodOverride

private const val UNKNOWN = "<unknown>"
private const val GODOT_VIRTUAL_METHOD_FQN = "io.github.kingg22.godot.api.internal.GodotVirtualMethod"
private const val TYPED_SIGNAL_FQN = "io.github.kingg22.godot.api.signal.TypedSignal"
private const val BUILTIN_SIGNAL_FQN = "io.github.kingg22.godot.api.builtin.Signal"
private const val GODOT_OBJECT_FQN = "io.github.kingg22.godot.api.core.GodotObject"
private const val GODOT_CLASS_FQN = "io.github.kingg22.godot.api.internal.GodotClass"
private const val GODOT_ANNOTATION_FQN = "io.github.kingg22.godot.api.annotations.Godot"

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

    val filePath = this.containingFile?.filePath ?: UNKNOWN
    val line = this.getLineNumber()

    val overriddenVirtualMethods = this.getDeclaredFunctions()
        .filter { Modifier.OVERRIDE in it.modifiers }
        .mapNotNull { it.findVirtualMethodOverride() }
        .toList()

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
        overriddenVirtualMethods = overriddenVirtualMethods,
    )
}

/**
 * Walks the override chain via [KSFunctionDeclaration.findOverridee] up to the engine-generated root
 * declaration annotated `@GodotVirtualMethod`, if any. Annotations aren't inherited across `override`,
 * so the user's own declaration never carries it directly — only the original `api/generated` stub does.
 */
private fun KSFunctionDeclaration.findVirtualMethodOverride(): VirtualMethodOverride? {
    var current: KSFunctionDeclaration? = this
    while (current != null) {
        val marker = current.annotations.firstOrNull { it.isGodotVirtualMethod() }
        if (marker != null) {
            val godotName = marker.arguments.firstOrNull { it.name?.asString() == "godotName" }?.value as? String
            val owner = current.parentDeclaration as? KSClassDeclaration
            if (godotName != null && owner != null) {
                return VirtualMethodOverride(
                    godotName = godotName,
                    enginePackageName = owner.packageName.asString(),
                    engineClassShortName = owner.simpleName.asString(),
                )
            }
            return null
        }
        current = current.findOverridee() as? KSFunctionDeclaration
    }
    return null
}

private fun KSAnnotation.isGodotVirtualMethod(): Boolean {
    val decl = this.annotationType.resolve().declaration as? KSClassDeclaration ?: return false
    return decl.qualifiedName?.asString() == GODOT_VIRTUAL_METHOD_FQN
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
    val qualifiedName = (decl as? KSClassDeclaration)?.qualifiedName?.asString() ?: UNKNOWN
    val shortName = (decl as? KSClassDeclaration)?.simpleName?.asString() ?: qualifiedName

    val arguments = this.arguments.associate { arg ->
        val name = arg.name?.asString() ?: UNKNOWN

        val value = when (val v = arg.value) {
            is KSType -> {
                val typeDecl = v.declaration
                if (typeDecl is KSClassDeclaration) {
                    typeDecl.qualifiedName?.asString() ?: UNKNOWN
                } else {
                    typeDecl.toString()
                }
            }

            // Enum-typed annotation arguments (e.g. Variant.Type.INT) resolve to the enum entry's
            // own KSClassDeclaration, not a KSType wrapping it.
            is KSClassDeclaration -> v.qualifiedName?.asString() ?: v.simpleName.asString()

            is KSAnnotation -> v.toAnnotationInfo()

            is List<*> -> v.mapNotNull { element ->
                when (element) {
                    is KSAnnotation -> element.toAnnotationInfo()

                    is KSType -> {
                        val typeDecl = element.declaration
                        if (typeDecl is KSClassDeclaration) {
                            typeDecl.qualifiedName?.asString()
                        } else {
                            element.toString()
                        }
                    }

                    is KSClassDeclaration -> element.qualifiedName?.asString() ?: element.simpleName.asString()

                    else -> element?.toString()
                }
            }

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
    val qualifiedName = declQName?.asString() ?: UNKNOWN

    return TypeInfo(
        qualifiedName = qualifiedName,
        shortName = declaration.simpleName.asString(),
        isNullable = this.nullability == Nullability.NULLABLE,
        isPrimitive = false,
    )
}

/**
 * Extension to convert KSPropertyDeclaration to PropertyInfo.
 */
fun KSPropertyDeclaration.toPropertyInfo(): PropertyInfo {
    // Delegated properties without an explicit type (`by signal<...>()`) resolve to the delegate's value type.
    val resolvedType = type.resolve()
    val typeInfo = resolvedType.takeUnless { it.isError }?.toTypeInfo() ?: TypeInfo(UNKNOWN, UNKNOWN)
    val annotations = this.annotations.map { it.toAnnotationInfo() }.toList()
    val modifiers = this.modifiers.map { it.name }.toSet()

    return PropertyInfo(
        name = simpleName.asString(),
        type = typeInfo,
        isMutable = this.isMutable,
        hasDefaultValue = false,
        annotations = annotations,
        modifiers = modifiers,
        lineNumber = getLineNumber(),
        signal = resolvedType.toSignalDeclaration(),
    )
}

/**
 * Reads the signal a property type declares: `TypedSignal<(value: Long) -> Unit>` gives its parameters with the
 * names Kotlin keeps as `@ParameterName` on each type argument; the builtin `Signal` gives [SignalDeclaration.Builtin].
 */
private fun KSType.toSignalDeclaration(): SignalDeclaration? {
    if (isError) return null
    return when (declaration.qualifiedName?.asString()) {
        BUILTIN_SIGNAL_FQN -> SignalDeclaration.Builtin

        TYPED_SIGNAL_FQN -> {
            val function = arguments.singleOrNull()?.type?.resolve()?.expandTypeAliases()
            when {
                function == null || function.isError -> SignalDeclaration.Invalid("its type argument is unresolved")

                !function.isFunctionType -> SignalDeclaration.Invalid(
                    "'${function.declaration.simpleName.asString()}' is not a function type",
                )

                // The last type argument of FunctionN is the return type (Unit, enforced by TypedSignal's bound).
                else -> SignalDeclaration.Typed(function.arguments.dropLast(1).map { it.toSignalParameter() })
            }
        }

        else -> null
    }
}

private fun KSTypeArgument.toSignalParameter(): SignalParameterInfo {
    val reference = type
    val resolved = reference?.resolve()?.takeUnless { it.isError }?.expandTypeAliases()
    // KSP2 exposes @ParameterName on the reference and on the resolved type; either is enough.
    val name = (reference?.annotations.orEmpty() + resolved?.annotations.orEmpty())
        .firstOrNull { it.shortName.asString() == "ParameterName" }
        ?.arguments
        ?.firstOrNull()
        ?.value as? String
    return SignalParameterInfo(
        name = name.orEmpty(),
        type = resolved?.toTypeInfo() ?: TypeInfo(UNKNOWN, UNKNOWN),
        godotClassName = resolved?.takeIf { it.isGodotObject() }?.let { it.registeredGodotClassName().orEmpty() },
    )
}

/**
 * The class Godot knows this type as: the nearest class in its hierarchy registered with Godot, a user `@Godot` class
 * by its simple name or an engine class by its `@GodotClass` name. A Kotlin subclass that is not registered is seen
 * by Godot as that ancestor.
 */
private fun KSType.registeredGodotClassName(): String? {
    var current = declaration as? KSClassDeclaration
    while (current != null) {
        for (annotation in current.annotations) {
            when (annotation.annotationType.resolve().declaration.qualifiedName?.asString()) {
                GODOT_ANNOTATION_FQN -> return current.simpleName.asString()
                GODOT_CLASS_FQN -> return annotation.arguments.firstOrNull()?.value as? String
            }
        }
        current = current.superTypes
            .map { it.resolve().declaration }
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.classKind == ClassKind.CLASS }
    }
    return null
}

/** Follows (possibly nested) type aliases: `typealias Hit = (damage: Long) -> Unit` resolves to the function type. */
private tailrec fun KSType.expandTypeAliases(): KSType {
    val alias = declaration as? KSTypeAlias ?: return this
    val expanded = alias.type.resolve()
    return if (isMarkedNullable) expanded.makeNullable().expandTypeAliases() else expanded.expandTypeAliases()
}

private fun KSType.isGodotObject(): Boolean {
    val classDeclaration = declaration as? KSClassDeclaration ?: return false
    return classDeclaration.qualifiedName?.asString() == GODOT_OBJECT_FQN ||
        classDeclaration.getAllSuperTypes().any { it.declaration.qualifiedName?.asString() == GODOT_OBJECT_FQN }
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
            name = param.name?.asString() ?: UNKNOWN,
            type = param.type.toTypeInfo() ?: TypeInfo(UNKNOWN, UNKNOWN),
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
