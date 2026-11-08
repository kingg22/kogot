package io.github.kingg22.kogot.compiler.fir.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isSubclassOf
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.typeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isAny
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

internal object KogotClassIds {
    private val BUILTIN_PACKAGE = FqName("io.github.kingg22.godot.api.builtin")
    private val CORE_PACKAGE = FqName("io.github.kingg22.godot.api.core")

    val MUST_BE_VARIANT = builtin("MustBeVariant")
    val GODOT_OBJECT = ClassId(CORE_PACKAGE, Name.identifier("GodotObject"))

    /**
     * Final types convertible to and from a Variant, the same rules both ways (kept in sync with `anyToVariantOrNull`
     * in `ConvertToVariant.kt` and `getValueOrNull` in `VariantConverter.kt`; see the `MustBeVariant` KDoc).
     */
    val VARIANT_TYPES: Set<ClassId> = setOf(
        StandardClassIds.Nothing, // `null` literal -> NIL
        StandardClassIds.Unit,
        StandardClassIds.Boolean,
        StandardClassIds.Short, // INT, widened / narrowed
        StandardClassIds.Int, // INT, widened / narrowed
        StandardClassIds.Long,
        StandardClassIds.Float,
        StandardClassIds.Double,
        StandardClassIds.String,
    ) + listOf(
        "Variant", "GodotString", "StringName", "NodePath",
        "Vector2", "Vector2i", "Rect2", "Rect2i", "Vector3", "Vector3i", "Vector4", "Vector4i",
        "Transform2D", "Transform3D", "Plane", "Quaternion", "Aabb", "Basis", "Projection", "Color",
        "Rid", "Callable", "Signal", "Dictionary", "GodotArray",
        "PackedByteArray", "PackedInt32Array", "PackedInt64Array", "PackedFloat32Array", "PackedFloat64Array",
        "PackedStringArray", "PackedVector2Array", "PackedVector3Array", "PackedVector4Array", "PackedColorArray",
    ).map(::builtin)

    private fun builtin(name: String) = ClassId(BUILTIN_PACKAGE, Name.identifier(name))
}

internal enum class VariantCompatibility {
    /** Statically known to convert to a `Variant`. */
    COMPATIBLE,

    /** Open type (`Any`, unconstrained `T`, an interface...): only the runtime slow path can tell. */
    UNKNOWN,

    /** No value of this type can ever convert to a `Variant`. */
    INCOMPATIBLE,
}

internal fun ConeKotlinType.variantCompatibility(session: FirSession): VariantCompatibility {
    val type = fullyExpandedType(session).lowerBoundIfFlexible()
    if (type.isAny || type.isNullableAny) return VariantCompatibility.UNKNOWN

    return when (type) {
        is ConeTypeParameterType -> typeParameterCompatibility(type.lookupTag.typeParameterSymbol, session)

        is ConeClassLikeType -> classCompatibility(type, session)

        // Smart casts (`Any & Byte`): the value is all of them at once.
        is ConeIntersectionType -> combineAll(type.intersectedTypes.map { it.variantCompatibility(session) })

        // Captured and error types: leave them to the runtime check.
        else -> VariantCompatibility.UNKNOWN
    }
}

/**
 * A type parameter that already carries the contract propagates it to its callers; otherwise its upper bounds decide
 * (all must hold at once): `T : Node2D` is compatible, `T : Regex` is not, an unbounded `T` is left to the runtime.
 */
private fun typeParameterCompatibility(
    typeParameter: FirTypeParameterSymbol,
    session: FirSession,
): VariantCompatibility {
    if (typeParameter.hasAnnotation(KogotClassIds.MUST_BE_VARIANT, session)) return VariantCompatibility.COMPATIBLE
    return combineAll(typeParameter.resolvedBounds.map { it.coneType.variantCompatibility(session) })
}

/** Compatibility of a value that is all of these types at once (type parameter bounds, intersection types). */
private fun combineAll(parts: Collection<VariantCompatibility>): VariantCompatibility = when {
    VariantCompatibility.INCOMPATIBLE in parts -> VariantCompatibility.INCOMPATIBLE
    VariantCompatibility.COMPATIBLE in parts -> VariantCompatibility.COMPATIBLE
    else -> VariantCompatibility.UNKNOWN
}

private fun classCompatibility(type: ConeClassLikeType, session: FirSession): VariantCompatibility {
    if (type.classId in KogotClassIds.VARIANT_TYPES) return VariantCompatibility.COMPATIBLE
    val symbol = type.toRegularClassSymbol(session) ?: return VariantCompatibility.UNKNOWN
    if (symbol.isGodotObject(session)) return VariantCompatibility.COMPATIBLE
    // Final classes (including enums and objects) cannot hide a Variant-compatible subtype.
    if (symbol.isFinal) return VariantCompatibility.INCOMPATIBLE

    // A non-stdlib interface may be implemented by a GodotObject subclass (`class Enemy : Node2D(), Damageable`).
    // Classes can't be: Kotlin has single class inheritance, so an unrelated open class never holds a GodotObject.
    if (symbol.classKind == ClassKind.INTERFACE &&
        !symbol.classId.packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
    ) {
        return VariantCompatibility.UNKNOWN
    }

    // Other open types: compatible values may still hide behind them (e.g. `Number` holding an `Int`).
    val lookupTag = symbol.toLookupTag()
    val mayHoldVariant = (KogotClassIds.VARIANT_TYPES + KogotClassIds.GODOT_OBJECT).any { classId ->
        classId.toLookupTag().toRegularClassSymbol(session)
            ?.isSubclassOf(lookupTag, session, isStrict = false, lookupInterfaces = true) == true
    }
    return if (mayHoldVariant) VariantCompatibility.UNKNOWN else VariantCompatibility.INCOMPATIBLE
}

private fun FirRegularClassSymbol.isGodotObject(session: FirSession): Boolean =
    isSubclassOf(KogotClassIds.GODOT_OBJECT.toLookupTag(), session, isStrict = false, lookupInterfaces = false)
