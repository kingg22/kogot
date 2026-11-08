package io.github.kingg22.kogot.compiler.fir.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isSubclassOf
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.typeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.classId
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

    /** Final types accepted as-is by `anyToVariantOrNull` (kept in sync with `ConvertToVariant.kt`). */
    val VARIANT_TYPES: Set<ClassId> = setOf(
        StandardClassIds.Nothing, // `null` literal -> NIL
        StandardClassIds.Unit,
        StandardClassIds.Boolean,
        StandardClassIds.Int,
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
        // A type parameter that already carries the contract propagates it to its callers.
        is ConeTypeParameterType ->
            if (type.lookupTag.typeParameterSymbol.hasAnnotation(KogotClassIds.MUST_BE_VARIANT, session)) {
                VariantCompatibility.COMPATIBLE
            } else {
                VariantCompatibility.UNKNOWN
            }

        is ConeClassLikeType -> classCompatibility(type, session)

        // Intersection, captured and error types: leave them to the runtime check.
        else -> VariantCompatibility.UNKNOWN
    }
}

private fun classCompatibility(type: ConeClassLikeType, session: FirSession): VariantCompatibility {
    if (type.classId in KogotClassIds.VARIANT_TYPES) return VariantCompatibility.COMPATIBLE
    val symbol = type.toRegularClassSymbol(session) ?: return VariantCompatibility.UNKNOWN
    if (symbol.isGodotObject(session)) return VariantCompatibility.COMPATIBLE
    if (symbol.isFinal || symbol.classKind == ClassKind.ENUM_CLASS) return VariantCompatibility.INCOMPATIBLE

    // Open type: compatible values may still hide behind it (e.g. `Number` holding an `Int`).
    val lookupTag = symbol.toLookupTag()
    val mayHoldVariant = (KogotClassIds.VARIANT_TYPES + KogotClassIds.GODOT_OBJECT).any { classId ->
        classId.toLookupTag().toRegularClassSymbol(session)
            ?.isSubclassOf(lookupTag, session, isStrict = false, lookupInterfaces = true) == true
    }
    return if (mayHoldVariant) VariantCompatibility.UNKNOWN else VariantCompatibility.INCOMPATIBLE
}

private fun FirRegularClassSymbol.isGodotObject(session: FirSession): Boolean =
    isSubclassOf(KogotClassIds.GODOT_OBJECT.toLookupTag(), session, isStrict = false, lookupInterfaces = false)
