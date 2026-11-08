package io.github.kingg22.kogot.compiler.fir.checkers

import io.github.kingg22.kogot.compiler.diagnostics.KogotDiagnostics
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.typeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.types.varargElementType

/**
 * Validates every call whose parameters carry a `@MustBeVariant` contract: the generated Godot API, user functions,
 * and generic APIs instantiated with an annotated type (`MutableList<@MustBeVariant Any?>.add`, `listOf<...>`).
 * The same rules apply to values sent to and received from Godot:
 * - type parameters: `fun <@MustBeVariant T> f()` checks the inferred or explicit type argument;
 * - value parameters and extension receivers: `fun f(x: @MustBeVariant Any?)` checks the passed argument, including
 *   annotated type arguments (`List<@MustBeVariant Any?>`) and function types (`() -> @MustBeVariant Any?`);
 * - substituted parameters: `fun <T> f(x: T)` called with `T = @MustBeVariant Any?` checks `x` too.
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
        if (checkCallArguments(expression)) validated = true

        if (validated) {
            val source = expression.calleeReference.source ?: expression.source ?: return
            reporter.reportOn(source, KogotDiagnostics.MUST_BE_VARIANT_CHECKED, callee.name.asString())
        }
    }
}

/**
 * Checks the receiver and arguments of [call] against its parameter types, with the callee's type parameters replaced
 * by the call's type arguments, or by [bindings] where a contract is known for them (see [bindContracts]).
 * Returns whether any parameter carried a contract.
 */
@IgnorableReturnValue
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkCallArguments(
    call: FirFunctionCall,
    bindings: Map<FirTypeParameterSymbol, ConeKotlinType> = emptyMap(),
): Boolean {
    val callee = call.calleeReference.toResolvedFunctionSymbol() ?: return false
    val session = context.session
    val receiverType = callee.resolvedReceiverType
    val extensionReceiver = call.extensionReceiver

    val substitution = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
    callee.typeParameterSymbols.forEachIndexed { index, typeParameter ->
        // Annotated type parameters stay as they are: their type argument is checked by the call checker.
        if (typeParameter.hasAnnotation(KogotClassIds.MUST_BE_VARIANT, session)) return@forEachIndexed
        val type = call.typeArguments.getOrNull(index)?.toConeTypeProjection()?.type ?: return@forEachIndexed
        substitution[typeParameter] = type
    }
    // `list += x` with `list: MutableList<@MustBeVariant Any?>`: `plusAssign`'s `T` is inferred from `x`, but the
    // receiver binds it to the annotated element type.
    if (receiverType != null && extensionReceiver != null) {
        bindContracts(receiverType, extensionReceiver.resolvedType, substitution.keys, session, substitution)
    }
    substitution.putAll(bindings)
    val substitutor = substitutorByMap(substitution, session)

    var checked = false
    fun checkArgument(declaredType: ConeKotlinType, argument: FirExpression, position: String) {
        // A type annotated as a whole (`@MustBeVariant T`) is the contract itself, whatever `T` becomes.
        val type = if (declaredType.customAnnotations.hasMustBeVariant(session)) {
            declaredType
        } else {
            substitutor.substituteOrSelf(declaredType)
        }
        if (checkAgainstDeclaredType(type, argument, position, argument.source ?: call.source)) checked = true
    }

    if (receiverType != null &&
        extensionReceiver != null
    ) {
        checkArgument(receiverType, extensionReceiver, "the receiver")
    }
    call.resolvedArgumentMapping?.forEach { (argument, parameter) ->
        val declaredType = parameter.returnTypeRef.coneType
        val position = "parameter '${parameter.name}'"
        if (argument !is FirVarargArgumentsExpression) return@forEach checkArgument(declaredType, argument, position)
        argument.arguments.forEach { element ->
            if (element is FirSpreadArgumentExpression) {
                checkArgument(declaredType, element.expression, position)
            } else {
                checkArgument(declaredType.varargElementType(), element, position)
            }
        }
    }
    return checked
}

/**
 * Binds the [typeParameters] that [pattern] (e.g. `MutableCollection<in T>`) places where [actual] carries a contract
 * (`MutableList<@MustBeVariant Any?>` binds `T` to `@MustBeVariant Any?`), adding them to [into].
 */
internal fun bindContracts(
    pattern: ConeKotlinType,
    actual: ConeKotlinType,
    typeParameters: Set<FirTypeParameterSymbol>,
    session: FirSession,
    into: MutableMap<FirTypeParameterSymbol, ConeKotlinType>,
) {
    when (val type = pattern.fullyExpandedType(session).lowerBoundIfFlexible()) {
        is ConeTypeParameterType -> {
            val symbol = type.lookupTag.typeParameterSymbol
            if (symbol in typeParameters && actual.hasContract(session)) into[symbol] = actual
        }

        is ConeClassLikeType -> {
            // Either side may be the supertype: a receiver `MutableList<..>` matches the pattern
            // `MutableCollection<in T>`, while an expected `Collection<..>` matches `listOf`'s result `List<T>`.
            val actualClass = actual.fullyExpandedType(session).lowerBoundIfFlexible() as? ConeClassLikeType ?: return
            val (matchedPattern, matchedActual) = actual.asSupertype(type.lookupTag, session)?.let { type to it }
                ?: type.asSupertype(actualClass.lookupTag, session)?.let { it to actualClass }
                ?: return
            if (matchedPattern.typeArguments.size != matchedActual.typeArguments.size) return
            matchedPattern.typeArguments.zip(matchedActual.typeArguments).forEach { (patternArgument, actualArgument) ->
                val patternType = patternArgument.type ?: return@forEach
                val actualType = actualArgument.type ?: return@forEach
                bindContracts(patternType, actualType, typeParameters, session, into)
            }
        }

        else -> Unit
    }
}
