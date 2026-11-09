package io.github.kingg22.kogot.compiler.fir.checkers

import io.github.kingg22.kogot.compiler.diagnostics.KogotDiagnostics
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.types.valueParameterName
import org.jetbrains.kotlin.fir.types.valueParameterTypesWithoutReceivers
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val SIGNAL_DELEGATE = CallableId(FqName("io.github.kingg22.godot.api.signal"), Name.identifier("signal"))
private val REGISTER_SIGNAL =
    ClassId(FqName("io.github.kingg22.godot.api.annotations"), Name.identifier("RegisterSignal"))

/**
 * `signal<(value: Long) -> Unit>()`: every parameter of the function type must be Variant compatible, the same rule
 * the generated `emit`/`connect` apply. Checked at the declaration, so a signal nobody emits yet is caught too.
 */
object SignalDeclarationCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (!expression.isSignalDelegateCall()) return
        // `signal()` without type arguments declares a parameterless signal.
        val typeArgument = expression.typeArguments.singleOrNull() ?: return
        val session = context.session
        val function = typeArgument.toConeTypeProjection().type
            ?.fullyExpandedType()
            ?.lowerBoundIfFlexible() as? ConeClassLikeType
            ?: return
        // A non-function type argument is already rejected by `signal`'s `Function<Unit>` bound.
        if (!function.isSomeFunctionType(session)) return

        val source = typeArgument.source ?: expression.source
        function.valueParameterTypesWithoutReceivers(session).forEachIndexed { index, parameter ->
            val name = parameter.valueParameterName(session)?.asString() ?: "#${index + 1}"
            checkVariantType(parameter, source, "signal parameter '$name'")
        }
        val callSource = expression.calleeReference.source ?: return
        reporter.reportOn(callSource, KogotDiagnostics.MUST_BE_VARIANT_CHECKED, SIGNAL_DELEGATE.callableName.asString())
    }
}

/**
 * A property delegated to `signal(...)` must be registered with `@RegisterSignal`: otherwise Godot does not know the
 * signal and rejects every `emit` and `connect` at runtime.
 */
object SignalRegistrationPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val delegate = declaration.delegate ?: return
        if (!delegate.isSignalDelegateCall()) return
        if (declaration.hasAnnotation(REGISTER_SIGNAL, context.session)) return
        reporter.reportOn(declaration.source, KogotDiagnostics.SIGNAL_NOT_REGISTERED, declaration.name.asString())
    }
}

private fun FirExpression.isSignalDelegateCall(): Boolean =
    (this as? FirFunctionCall)?.calleeReference?.toResolvedFunctionSymbol()?.callableId == SIGNAL_DELEGATE
