package io.github.kingg22.kogot.compiler.fir.checkers

import io.github.kingg22.kogot.compiler.diagnostics.KogotDiagnostics
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedValueParameterSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.typeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

/**
 * Checks a value flowing into a `@MustBeVariant` position (an argument, a lambda result, an initializer, an
 * assignment, a `return`): every branch it may take is checked separately.
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkVariantValue(value: FirExpression, position: String, fallbackSource: AbstractKtSourceElement?) {
    value.resultLeaves().forEach { leaf ->
        checkVariantType(leaf.resolvedType, leaf.source ?: fallbackSource, position, origin = leaf)
    }
}

/**
 * Checks [value] against a [declaredType] that may carry the contract directly (`@MustBeVariant Any?`) or in a function
 * type (`() -> @MustBeVariant Any?`). Returns whether the declared type carried any contract.
 */
@IgnorableReturnValue
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkAgainstDeclaredType(
    declaredType: ConeKotlinType,
    value: FirExpression,
    position: String,
    source: AbstractKtSourceElement?,
): Boolean {
    var checked = false
    if (declaredType.customAnnotations.hasMustBeVariant(context.session)) {
        checked = true
        checkVariantValue(value, position, source)
    }
    if (checkFunctionTypeArguments(declaredType, value, position, source)) checked = true
    return checked
}

/**
 * For a function type (`(@MustBeVariant P) -> @MustBeVariant R`), checks each annotated type argument against the
 * passed function's matching one. Returns whether any position carried a contract.
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkFunctionTypeArguments(
    declaredType: ConeKotlinType,
    value: FirExpression,
    position: String,
    source: AbstractKtSourceElement?,
): Boolean {
    val session = context.session
    val declared = declaredType.fullyExpandedType().lowerBoundIfFlexible()
    val actual = value.resolvedType.fullyExpandedType().lowerBoundIfFlexible()
    if (declared !is ConeClassLikeType || !declared.isSomeFunctionType(session)) return false
    if (actual !is ConeClassLikeType || actual.typeArguments.size != declared.typeArguments.size) return false

    var checked = false
    val lastIndex = declared.typeArguments.lastIndex
    declared.typeArguments.forEachIndexed { index, declaredArgument ->
        val declaredArgumentType = declaredArgument.type ?: return@forEachIndexed
        if (!declaredArgumentType.customAnnotations.hasMustBeVariant(session)) return@forEachIndexed
        // `(@MustBeVariant P1) -> R` with P1 a type parameter of the callee: validated through its type argument.
        if (declaredArgumentType.lowerBoundIfFlexible() is ConeTypeParameterType) return@forEachIndexed
        checked = true
        if (index == lastIndex && value is FirAnonymousFunctionExpression) {
            // A lambda's return type follows the expected one (e.g. `Any?`), not its body: check what it returns.
            value.anonymousFunction.returnedExpressions().forEach { returned ->
                checkVariantValue(returned, "the result of $position", source)
            }
            return@forEachIndexed
        }
        val actualArgument = actual.typeArguments[index].type ?: return@forEachIndexed
        val part = if (index == lastIndex) "the result" else "parameter ${index + 1}"
        checkVariantType(actualArgument, value.source ?: source, "$part of $position")
    }
    return checked
}

/**
 * Reports a type that can never cross the Variant boundary as an error, and one whose concrete type is only known at
 * runtime as a warning that says how to make it checkable. [origin] is the value expression, if any.
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkVariantType(
    type: ConeKotlinType,
    source: AbstractKtSourceElement?,
    position: String,
    origin: FirExpression? = null,
) {
    if (source == null) return
    when (type.variantCompatibility(context.session)) {
        VariantCompatibility.COMPATIBLE -> Unit

        VariantCompatibility.INCOMPATIBLE ->
            reporter.reportOn(source, KogotDiagnostics.NOT_VARIANT_COMPATIBLE, type, position)

        VariantCompatibility.UNKNOWN ->
            reporter.reportOn(
                source,
                KogotDiagnostics.MUST_BE_VARIANT_UNKNOWN_TYPE,
                type,
                position,
                unknownTypeHint(type, origin),
            )
    }
}

/** Annotating where the open type comes from moves the check to its callers, which removes the warning. */
private fun unknownTypeHint(type: ConeKotlinType, origin: FirExpression?): String {
    val parameter = (origin as? FirQualifiedAccessExpression)?.calleeReference?.toResolvedValueParameterSymbol()
    if (parameter != null && parameter.containingDeclarationSymbol !is FirAnonymousFunctionSymbol) {
        return "Annotate the type of parameter '${parameter.name}' with @MustBeVariant to check its callers instead."
    }
    val typeParameter = (type.lowerBoundIfFlexible() as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol
    if (typeParameter != null) {
        return "Annotate type parameter '${typeParameter.name}' with @MustBeVariant to check its callers instead."
    }
    return "Use a concrete type, or suppress with @Suppress(\"MUST_BE_VARIANT_UNKNOWN_TYPE\") " +
        "if the runtime check is intended."
}

internal fun List<FirAnnotation>.hasMustBeVariant(session: FirSession): Boolean =
    hasAnnotation(KogotClassIds.MUST_BE_VARIANT, session)

/** Values this lambda returns: explicit `return@label` and the implicit return of its last expression. */
internal fun FirAnonymousFunction.returnedExpressions(): List<FirExpression> {
    val function = this
    val returned = mutableListOf<FirExpression>()
    body?.accept(
        object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitReturnExpression(returnExpression: FirReturnExpression) {
                // Nested lambdas return to themselves; only keep returns that target this function.
                if (returnExpression.target.labeledElement == function) returned += returnExpression.result
                returnExpression.acceptChildren(this)
            }
        },
    )
    return returned
}

/**
 * The expressions that may become this expression's value: `if`/`when`, `try`/`catch` and `?:` contribute every
 * branch, since their own type is only the branches' common supertype (`Any` for `if (c) 1 else Regex("")`).
 */
internal fun FirExpression.resultLeaves(): List<FirExpression> = when (this) {
    is FirWhenExpression -> branches.flatMap { it.result.resultLeaves() }
    is FirTryExpression -> (listOf(tryBlock) + catches.map { it.block }).flatMap { it.resultLeaves() }
    is FirElvisExpression -> lhs.resultLeaves() + rhs.resultLeaves()
    is FirBlock -> (statements.lastOrNull() as? FirExpression)?.resultLeaves().orEmpty()
    is FirFunctionCall -> inlineLambdaResults() ?: listOf(this)
    else -> listOf(this)
}

/**
 * `run { if (c) 1 else Regex("") }`: an inline function returning its lambda's result (`run`, `let`, `with`...) has
 * the lambda's own leaves, not their common supertype. Null when this call is not one of them.
 */
private fun FirFunctionCall.inlineLambdaResults(): List<FirExpression>? {
    val callee = calleeReference.toResolvedFunctionSymbol() ?: return null
    if (!callee.isInline) return null
    val returnType = callee.resolvedReturnType.lowerBoundIfFlexible() as? ConeTypeParameterType ?: return null
    val lambda = resolvedArgumentMapping?.entries?.firstOrNull { (_, parameter) ->
        val parameterType = parameter.returnTypeRef.coneType.lowerBoundIfFlexible() as? ConeClassLikeType
        val resultType = parameterType?.typeArguments?.lastOrNull()?.type?.lowerBoundIfFlexible()
        resultType is ConeTypeParameterType && resultType.lookupTag == returnType.lookupTag
    }?.key as? FirAnonymousFunctionExpression ?: return null
    return lambda.anonymousFunction.returnedExpressions().flatMap { it.resultLeaves() }
}
