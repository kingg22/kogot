package io.github.kingg22.kogot.compiler.fir

import io.github.kingg22.kogot.compiler.diagnostics.KogotDiagnostics
import io.github.kingg22.kogot.compiler.fir.checkers.MustBeVariantAssignmentChecker
import io.github.kingg22.kogot.compiler.fir.checkers.MustBeVariantCallChecker
import io.github.kingg22.kogot.compiler.fir.checkers.MustBeVariantPropertyChecker
import io.github.kingg22.kogot.compiler.fir.checkers.MustBeVariantReferenceChecker
import io.github.kingg22.kogot.compiler.fir.checkers.MustBeVariantReturnChecker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/** Single FIR entry point: every kogot frontend extension and diagnostic container is registered here. */
class KogotFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::KogotFirCheckersExtension
        registerDiagnosticContainers(KogotDiagnostics)
    }
}

/** Additional FIR checkers contributed by kogot. */
class KogotFirCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(MustBeVariantCallChecker)
        override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> =
            setOf(MustBeVariantAssignmentChecker)
        override val returnExpressionCheckers: Set<FirReturnExpressionChecker> = setOf(MustBeVariantReturnChecker)
        override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> =
            setOf(MustBeVariantReferenceChecker)
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker> = setOf(MustBeVariantPropertyChecker)
    }
}
