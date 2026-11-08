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
import org.jetbrains.kotlin.fir.resolve.createSubstitutionForSupertype
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.typeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
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
 * Checks [value] against a [declaredType] that may carry the contract directly (`@MustBeVariant Any?`) or in any of its
 * type arguments, at any depth: function types (`() -> @MustBeVariant Any?`), collections
 * (`List<@MustBeVariant Any?>`), or any other generic type. Returns whether the declared type carried any contract.
 */
@IgnorableReturnValue
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkAgainstDeclaredType(
    declaredType: ConeKotlinType,
    value: FirExpression,
    position: String,
    source: AbstractKtSourceElement?,
): Boolean {
    val session = context.session
    if (declaredType.customAnnotations.hasMustBeVariant(session)) {
        checkVariantValue(value, position, source)
        return true
    }
    val declared = declaredType.fullyExpandedType().lowerBoundIfFlexible() as? ConeClassLikeType ?: return false
    if (!declared.hasTypeArgumentContract(session)) return false

    if (value is FirAnonymousFunctionExpression && declared.isSomeFunctionType(session)) {
        checkLambda(declared, value, position, source)
    } else {
        value.resultLeaves().forEach { leaf ->
            val leafSource = leaf.source ?: source
            val type = (leaf as? FirFunctionCall)?.checkWithExpectedType(declared) ?: leaf.resolvedType
            checkTypeArguments(declared, type, leafSource, position)
        }
    }
    return true
}

/**
 * `val list: List<@MustBeVariant Any?> = listOf(1L, Regex(""))`: Kotlin infers `T` from the arguments (`Any`), not
 * from the annotated expected type, so the type parameters the result binds to a contract are checked through the
 * call's own arguments instead (`Regex("")` is reported, not the whole call). Returns the call's type with those
 * bindings, or null when none applies.
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun FirFunctionCall.checkWithExpectedType(expected: ConeClassLikeType): ConeKotlinType? {
    val callee = calleeReference.toResolvedFunctionSymbol() ?: return null
    val session = context.session
    val typeParameters = callee.typeParameterSymbols
        .filterNot { it.hasAnnotation(KogotClassIds.MUST_BE_VARIANT, session) }
        .toSet()
    val bindings = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
    bindContracts(callee.resolvedReturnType, expected, typeParameters, session, bindings)
    if (bindings.isEmpty()) return null
    checkCallArguments(this, bindings)
    return substitutorByMap(bindings, session).substituteOrSelf(callee.resolvedReturnType)
}

/** Whether this type carries the contract, as a whole or in a type argument at any depth. */
internal fun ConeKotlinType.hasContract(session: FirSession): Boolean {
    val type = fullyExpandedType(session).lowerBoundIfFlexible()
    return when {
        type.customAnnotations.hasMustBeVariant(session) -> type !is ConeTypeParameterType
        type is ConeClassLikeType -> type.hasTypeArgumentContract(session)
        else -> false
    }
}

/**
 * Whether a type argument of this type, at any depth, carries the contract. `@MustBeVariant P1` with `P1` a type
 * parameter of the callee is excluded: it is validated through the call's type argument instead.
 */
private fun ConeClassLikeType.hasTypeArgumentContract(session: FirSession): Boolean =
    typeArguments.any { it.type?.hasContract(session) == true }

/**
 * A lambda's types are its declared parameter types plus its result, which follows the expected type (e.g. `Any?`)
 * rather than its body: the values it returns are checked instead.
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkLambda(
    declared: ConeClassLikeType,
    lambda: FirAnonymousFunctionExpression,
    position: String,
    source: AbstractKtSourceElement?,
) {
    val session = context.session
    val result = declared.typeArguments.last().type?.lowerBoundIfFlexible()
    if (result != null && result !is ConeTypeParameterType) {
        val resultPosition = "the result of $position"
        if (result.customAnnotations.hasMustBeVariant(session)) {
            lambda.anonymousFunction.returnedExpressions().forEach { checkVariantValue(it, resultPosition, source) }
        } else if (result is ConeClassLikeType && result.hasTypeArgumentContract(session)) {
            lambda.anonymousFunction.returnedExpressions().forEach {
                checkAgainstDeclaredType(result, it, resultPosition, source)
            }
        }
    }
    // Parameters only: the lambda's result type argument is the expected one, already checked above.
    checkTypeArguments(declared, lambda.resolvedType, lambda.source ?: source, position, skipLast = true)
}

/**
 * Checks [actual]'s type arguments against [declared]'s annotated ones, reading [actual] as [declared]'s class first
 * (an `ArrayList<Regex>` passed as `List<@MustBeVariant Any?>` is checked as `List<Regex>`).
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkTypeArguments(
    declared: ConeClassLikeType,
    actual: ConeKotlinType,
    source: AbstractKtSourceElement?,
    position: String,
    skipLast: Boolean = false,
) {
    val session = context.session
    val projected = actual.asSupertype(declared.lookupTag, session) ?: return
    if (projected.typeArguments.size != declared.typeArguments.size) return
    val lastIndex = declared.typeArguments.lastIndex
    declared.typeArguments.forEachIndexed { index, declaredArgument ->
        if (skipLast && index == lastIndex) return@forEachIndexed
        val declaredType = declaredArgument.type?.fullyExpandedType()?.lowerBoundIfFlexible()
            ?: return@forEachIndexed
        if (declaredType is ConeTypeParameterType) return@forEachIndexed
        // `List<*>` holds anything.
        val actualType = projected.typeArguments[index].type ?: session.builtinTypes.nullableAnyType.coneType
        val part = declared.describeTypeArgument(index, session)
        when {
            declaredType.customAnnotations.hasMustBeVariant(session) ->
                checkVariantType(actualType, source, "$part of $position")

            declaredType is ConeClassLikeType && declaredType.hasTypeArgumentContract(session) ->
                checkTypeArguments(declaredType, actualType, source, "$part of $position")
        }
    }
}

/** `parameter 1` / `the result` for function types, `type argument 'E'` for any other generic type. */
private fun ConeClassLikeType.describeTypeArgument(index: Int, session: FirSession): String = when {
    isSomeFunctionType(session) -> if (index == typeArguments.lastIndex) "the result" else "parameter ${index + 1}"

    else -> {
        val name = lookupTag.toSymbol(session)?.typeParameterSymbols?.getOrNull(index)?.name
        if (name != null) "type argument '$name'" else "type argument ${index + 1}"
    }
}

/** This type seen as [target] through its supertypes (`ArrayList<Regex>` as `List<Regex>`), if it is one. */
internal fun ConeKotlinType.asSupertype(target: ConeClassLikeLookupTag, session: FirSession): ConeClassLikeType? {
    val type = fullyExpandedType(session).lowerBoundIfFlexible() as? ConeClassLikeType ?: return null
    if (type.lookupTag == target) return type
    val symbol = type.lookupTag.toClassSymbol(session) ?: return null
    val substitutor = createSubstitutionForSupertype(type, session)
    return symbol.resolvedSuperTypes.firstNotNullOfOrNull { supertype ->
        substitutor.substituteOrSelf(supertype).asSupertype(target, session)
    }
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
