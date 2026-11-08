package io.github.kingg22.kogot.compiler.fir.checkers

import io.github.kingg22.kogot.compiler.diagnostics.KogotDiagnostics
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

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
            validate(
                type,
                typeArgument.source ?: expression.source,
                "type parameter '${typeParameter.name}'",
            )
        }

        expression.resolvedArgumentMapping?.forEach { (argument, parameter) ->
            val declaredType = parameter.returnTypeRef.coneType
            val source = argument.source ?: expression.source
            if (declaredType.customAnnotations.hasMustBeVariant(session)) {
                validated = true
                validate(argument.resolvedType, source, "parameter '${parameter.name}'")
            }
            if (validateFunctionTypeArguments(declaredType, argument, source, parameter.name.asString())) {
                validated = true
            }
        }

        if (validated) {
            val source = expression.calleeReference.source ?: expression.source ?: return
            reporter.reportOn(source, KogotDiagnostics.MUST_BE_VARIANT_CHECKED, callee.name.asString())
        }
    }

    /**
     * For a function-typed parameter (`(@MustBeVariant P) -> @MustBeVariant R`), checks each annotated type
     * argument against the passed function's matching one. Returns whether any position carried a contract.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun validateFunctionTypeArguments(
        declaredType: ConeKotlinType,
        argument: FirExpression,
        source: AbstractKtSourceElement?,
        parameterName: String,
    ): Boolean {
        val session = context.session
        val declared = declaredType.fullyExpandedType().lowerBoundIfFlexible()
        val actual = argument.resolvedType.fullyExpandedType().lowerBoundIfFlexible()
        if (declared !is ConeClassLikeType || !declared.isSomeFunctionType(session)) return false
        if (actual !is ConeClassLikeType || actual.typeArguments.size != declared.typeArguments.size) return false

        var validated = false
        val lastIndex = declared.typeArguments.lastIndex
        declared.typeArguments.forEachIndexed { index, declaredArgument ->
            val declaredArgumentType = declaredArgument.type ?: return@forEachIndexed
            if (!declaredArgumentType.customAnnotations.hasMustBeVariant(session)) return@forEachIndexed
            // `(@MustBeVariant P1) -> R` with P1 a type parameter: already validated through its type argument.
            if (declaredArgumentType.lowerBoundIfFlexible() is ConeTypeParameterType) return@forEachIndexed
            validated = true
            if (index == lastIndex && argument is FirAnonymousFunctionExpression) {
                // A lambda's return type follows the expected one (e.g. `Any?`), not its body: check what it returns.
                argument.anonymousFunction.returnedExpressions().forEach { returned ->
                    validate(
                        returned.resolvedType,
                        returned.source ?: source,
                        "the result of '$parameterName'",
                    )
                }
                return@forEachIndexed
            }
            val actualArgument = actual.typeArguments[index].type ?: return@forEachIndexed
            val position = if (index == lastIndex) "the result" else "parameter ${index + 1}"
            validate(actualArgument, source, "$position of '$parameterName'")
        }
        return validated
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun validate(type: ConeKotlinType, source: AbstractKtSourceElement?, position: String) {
        if (source == null) return
        if (type.variantCompatibility(context.session) == VariantCompatibility.INCOMPATIBLE) {
            reporter.reportOn(source, KogotDiagnostics.NOT_VARIANT_COMPATIBLE, type, position)
        }
    }
}

/** Values this lambda returns: explicit `return@label` and the implicit return of its last expression. */
private fun FirAnonymousFunction.returnedExpressions(): List<FirExpression> {
    val function = this
    val returned = mutableListOf<FirExpression>()
    body?.accept(
        object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitReturnExpression(returnExpression: FirReturnExpression) {
                // Nested lambdas return to themselves; only keep returns that target this function.
                if (returnExpression.target.labeledElement ==
                    function
                ) {
                    returned += returnExpression.result.resultLeaves()
                }
                returnExpression.acceptChildren(this)
            }
        },
    )
    return returned
}

private fun List<FirAnnotation>.hasMustBeVariant(session: FirSession): Boolean =
    hasAnnotation(KogotClassIds.MUST_BE_VARIANT, session)

/**
 * The expressions that may become this expression's value: `if`/`when`, `try`/`catch` and `?:` contribute every
 * branch, since their own type is only the branches' common supertype (`Any` for `if (c) 1 else Regex("")`).
 */
private fun FirExpression.resultLeaves(): List<FirExpression> = when (this) {
    is FirWhenExpression -> branches.flatMap { it.result.resultLeaves() }
    is FirTryExpression -> (listOf(tryBlock) + catches.map { it.block }).flatMap { it.resultLeaves() }
    is FirElvisExpression -> lhs.resultLeaves() + rhs.resultLeaves()
    is FirBlock -> (statements.lastOrNull() as? FirExpression)?.resultLeaves().orEmpty()
    else -> listOf(this)
}
