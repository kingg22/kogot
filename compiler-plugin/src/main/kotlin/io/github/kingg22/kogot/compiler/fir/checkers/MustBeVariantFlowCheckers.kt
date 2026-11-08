package io.github.kingg22.kogot.compiler.fir.checkers

import io.github.kingg22.kogot.compiler.diagnostics.KogotDiagnostics
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirTypeChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.type

/*
 * `@MustBeVariant` on a type (a property, local variable or function return type) is a contract on every value
 * stored there, not only on call arguments. These checkers validate values where they enter such a type.
 */

/** `val x: @MustBeVariant Any? = value`: the initializer, for member, top-level and local properties alike. */
object MustBeVariantPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val initializer = declaration.initializer ?: return
        if (!declaration.returnTypeRef.coneType.customAnnotations.hasMustBeVariant(context.session)) return
        checkVariantValue(initializer, "property '${declaration.name}'", declaration.source)
    }
}

/** `x = value` where `x` is declared with a `@MustBeVariant` type. */
object MustBeVariantAssignmentChecker : FirVariableAssignmentChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirVariableAssignment) {
        val variable = (expression.lValue as? FirQualifiedAccessExpression)?.calleeReference?.toResolvedVariableSymbol()
            ?: return
        if (!variable.resolvedReturnType.customAnnotations.hasMustBeVariant(context.session)) return
        checkVariantValue(expression.rValue, "variable '${variable.name}'", expression.source)
    }
}

/**
 * `return value` (and expression bodies) from a function or getter whose return type is `@MustBeVariant`. Lambdas are
 * skipped: their result is checked at the call that receives them (see [MustBeVariantCallChecker]).
 */
object MustBeVariantReturnChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression) {
        val function = expression.target.labeledElement
        if (function is FirAnonymousFunction) return
        if (!function.returnTypeRef.coneType.customAnnotations.hasMustBeVariant(context.session)) return
        checkVariantValue(
            expression.result,
            "the result of '${function.symbol.callableId.callableName}'",
            expression.source,
        )
    }
}

/** `::anyToVariant`: calls through a reference cannot be checked, so referencing a contract is reported. */
object MustBeVariantReferenceChecker : FirCallableReferenceAccessChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCallableReferenceAccess) {
        val function = expression.calleeReference.toResolvedFunctionSymbol() ?: return
        val session = context.session
        val hasContract =
            function.typeParameterSymbols.any { it.hasAnnotation(KogotClassIds.MUST_BE_VARIANT, session) } ||
                function.valueParameterSymbols.any { it.resolvedReturnType.customAnnotations.hasMustBeVariant(session) }
        if (!hasContract) return
        val source = expression.source ?: return
        reporter.reportOn(source, KogotDiagnostics.MUST_BE_VARIANT_UNCHECKED_REFERENCE, function.name.asString())
    }
}

/**
 * `List<@MustBeVariant Any?>`: only function types (`() -> @MustBeVariant Any?`) and Godot typed containers give a
 * type argument a meaning; anywhere else the annotation silently does nothing, so it is reported.
 */
object MustBeVariantTypeArgumentChecker : FirTypeChecker<FirResolvedTypeRef>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        val session = context.session
        val type = typeRef.coneType.lowerBoundIfFlexible() as? ConeClassLikeType ?: return
        if (type.isSomeFunctionType(session) || type.classId in KogotClassIds.TYPED_CONTAINERS) return
        if (type.typeArguments.none { it.type?.customAnnotations?.hasMustBeVariant(session) == true }) return
        val source = typeRef.source ?: return
        reporter.reportOn(source, KogotDiagnostics.MUST_BE_VARIANT_NO_EFFECT)
    }
}
