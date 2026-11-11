package io.github.kingg22.kogot.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.type

/**
 * Validates the type arguments of every type written in the code against the `@MustBeVariant` contract of the type
 * parameters they fill: `lateinit var items: GodotArray<Regex>`, `fun f(items: GodotArray<Regex>)`,
 * `fun f(): GodotArray<Regex>`, supertypes, casts and nested arguments alike. The calls that build such a value
 * (`GodotArray<Regex>()`) are checked by [MustBeVariantCallChecker]; this covers the places a type is only written.
 *
 * Each type argument is reported where it is written (`Regex` in `GodotArray<Regex>`). Star projections are Godot's
 * untyped containers and hold anything, and type aliases are checked where they are declared.
 */
object MustBeVariantTypeRefChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        val session = context.session
        val type = typeRef.coneType.lowerBoundIfFlexible() as? ConeClassLikeType ?: return
        if (type.typeArguments.isEmpty()) return
        // A type alias has no contract of its own: its expansion is checked where the alias is declared.
        val typeParameters = type.lookupTag.toClassSymbol(session)?.typeParameterSymbols ?: return
        val written = (typeRef.delegatedTypeRef as? FirUserTypeRef)?.qualifier?.lastOrNull()
            ?.typeArgumentList?.typeArguments

        type.typeArguments.forEachIndexed { index, argument ->
            val typeParameter = typeParameters.getOrNull(index) ?: return@forEachIndexed
            if (!typeParameter.hasAnnotation(KogotClassIds.MUST_BE_VARIANT, session)) return@forEachIndexed
            val argumentType = argument.type ?: return@forEachIndexed
            val writtenArgument = written?.getOrNull(index)
            // A written container reports its own arguments (`Byte` in `GodotArray<GodotArray<Byte>>`).
            if (writtenArgument != null && argumentType.carriesOwnContract(session)) return@forEachIndexed
            val source = writtenArgument?.source ?: typeRef.source
            checkVariantType(argumentType, source, "type parameter '${typeParameter.name}'")
        }
    }
}

/**
 * Whether this is a generic type with `@MustBeVariant` type parameters of its own (`GodotArray<Byte>`). When such a type
 * is written out, its own type reference reports the bad argument, so the type around it must not report it again.
 */
internal fun ConeKotlinType.carriesOwnContract(session: FirSession): Boolean {
    val type = lowerBoundIfFlexible() as? ConeClassLikeType ?: return false
    if (type.typeArguments.isEmpty()) return false
    val typeParameters = type.lookupTag.toClassSymbol(session)?.typeParameterSymbols ?: return false
    return typeParameters.any { it.hasAnnotation(KogotClassIds.MUST_BE_VARIANT, session) }
}
