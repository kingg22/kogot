package io.github.kingg22.kogot.compiler.fir.checkers

import io.github.kingg22.kogot.compiler.diagnostics.KogotDiagnostics
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.type

/**
 * Validates calls to APIs declaring a `@MustBeVariant` contract, typically the generated Godot API. The same rules
 * apply to values sent to and received from Godot:
 * - type parameters: `fun <@MustBeVariant T> f()` checks the inferred or explicit type argument;
 * - value parameters: `fun f(x: @MustBeVariant Any?)` checks the type of the passed argument;
 * - function-typed parameters: `fun f(lambda: () -> @MustBeVariant Any?)` checks the passed lambda's own types.
 */
object MustBeVariantCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val callee = expression.calleeReference.toResolvedFunctionSymbol() ?: return
        val session = context.session
        var validated = false

        callee.typeParameterSymbols.forEachIndexed { index, typeParameter ->
            if (!typeParameter.hasAnnotation(KogotClassIds.MUST_BE_VARIANT, session)) return@forEachIndexed
            val typeArgument = expression.typeArguments.getOrNull(index) ?: return@forEachIndexed
            val type = typeArgument.toConeTypeProjection().type ?: return@forEachIndexed
            validated = true
            checkVariantType(type, typeArgument.source ?: expression.source, "type parameter '${typeParameter.name}'")
        }

        expression.resolvedArgumentMapping?.forEach { (argument, parameter) ->
            val declaredType = parameter.returnTypeRef.coneType
            val source = argument.source ?: expression.source
            if (checkAgainstDeclaredType(declaredType, argument, "parameter '${parameter.name}'", source)) {
                validated =
                    true
            }
        }

        if (validated) {
            val source = expression.calleeReference.source ?: expression.source ?: return
            reporter.reportOn(source, KogotDiagnostics.MUST_BE_VARIANT_CHECKED, callee.name.asString())
        }
    }
}
