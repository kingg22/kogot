package io.github.kingg22.kogot.compiler.fir.checkers

import io.github.kingg22.kogot.compiler.diagnostics.KogotDiagnostics
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.customAnnotations

/*
 * `@MustBeVariant` on a type (a property, local variable or function return type) is a contract on every value
 * stored there, not only on call arguments. These checkers validate values where they enter such a type.
 */

/**
 * `val x: @MustBeVariant Any? = value` (or `List<@MustBeVariant Any?>`, `() -> @MustBeVariant Any?`...): member,
 * top-level and local.
 */
object MustBeVariantPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val initializer = declaration.initializer ?: return
        // `val x = listOf<@MustBeVariant Any?>(...)`: an inferred type is the initializer's own, checked at its call.
        if (declaration.returnTypeRef.isInferred()) return
        val position = "property '${declaration.name}'"
        checkAgainstDeclaredType(declaration.returnTypeRef.coneType, initializer, position, declaration.source)
    }
}

/** `fun f(x: @MustBeVariant Any? = value)`: callers that omit the argument pass the default, so it is checked here. */
object MustBeVariantDefaultValueChecker : FirDeclarationChecker<FirValueParameter>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirValueParameter) {
        val defaultValue = declaration.defaultValue ?: return
        val position = "the default value of parameter '${declaration.name}'"
        checkAgainstDeclaredType(declaration.returnTypeRef.coneType, defaultValue, position, declaration.source)
    }
}

/** `x = value` where `x` is declared with a `@MustBeVariant` type. */
object MustBeVariantAssignmentChecker : FirVariableAssignmentChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirVariableAssignment) {
        val variable = (expression.lValue as? FirQualifiedAccessExpression)?.calleeReference?.toResolvedVariableSymbol()
            ?: return
        val position = "variable '${variable.name}'"
        checkAgainstDeclaredType(variable.resolvedReturnType, expression.rValue, position, expression.source)
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
        // A getter's type ref is always a copy: whether it was written is known from its property.
        val typeRef = when (function) {
            is FirPropertyAccessor -> function.propertySymbol.resolvedReturnTypeRef
            else -> function.returnTypeRef
        }
        if (typeRef.isInferred()) return
        val position = "the result of '${function.symbol.callableId.callableName}'"
        checkAgainstDeclaredType(function.returnTypeRef.coneType, expression.result, position, expression.source)
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
                function.valueParameterSymbols.any {
                    val type = it.resolvedReturnType
                    type.customAnnotations.hasMustBeVariant(session) || type.hasContract(session)
                }
        if (!hasContract) return
        val source = expression.source ?: return
        reporter.reportOn(source, KogotDiagnostics.MUST_BE_VARIANT_UNCHECKED_REFERENCE, function.name.asString())
    }
}

/** A type written by nobody: inferred from an initializer or expression body. */
private fun FirTypeRef.isInferred(): Boolean = source?.kind == KtFakeSourceElementKind.ImplicitTypeRef
