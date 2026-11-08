package io.github.kingg22.kogot.compiler.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtElement

/**
 * Diagnostics reported by the kogot compiler plugin. `FirErrors` is not part of the plugin API, so kogot owns them.
 *
 * Factories are built directly instead of through the `error0`..`error4` DSL: the DSL is bounded by
 * `com.intellij.psi.PsiElement`, which `kotlin-compiler-plugin-api` does not ship.
 */
object KogotDiagnostics : KtDiagnosticsContainer() {
    /**
     * "Hello kogot": emitted for every call whose `@MustBeVariant` contract was validated.
     *
     * INFO severity on purpose: the DSL only offers warnings and errors, and a warning would break
     * builds using `allWarningsAsErrors`.
     */
    val MUST_BE_VARIANT_CHECKED: KtDiagnosticFactory1<String> = KtDiagnosticFactory1(
        "MUST_BE_VARIANT_CHECKED",
        Severity.INFO,
        SourceElementPositioningStrategies.DEFAULT,
        KtElement::class,
        KogotDiagnosticRenderers,
    )

    /** A type that can never be converted to a `Variant` flows into a `@MustBeVariant` position. Args: type, position. */
    val NOT_VARIANT_COMPATIBLE: KtDiagnosticFactory2<ConeKotlinType, String> = KtDiagnosticFactory2(
        "NOT_VARIANT_COMPATIBLE",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        KtElement::class,
        KogotDiagnosticRenderers,
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KogotDiagnosticRenderers
}

private object KogotDiagnosticRenderers : BaseDiagnosticRendererFactory() {
    @Suppress("ktlint:standard:property-naming") // Overrides the platform's `MAP`.
    override val MAP by KtDiagnosticFactoryToRendererMap("Kogot") { map ->
        map.put(
            KogotDiagnostics.MUST_BE_VARIANT_CHECKED,
            "Hello kogot: validated the @MustBeVariant contract of ''{0}''.",
            CommonRenderers.STRING,
        )
        map.put(
            KogotDiagnostics.NOT_VARIANT_COMPATIBLE,
            "Type ''{0}'' is not Variant compatible, but {1} requires a @MustBeVariant type.",
            FirDiagnosticRenderers.RENDER_TYPE,
            CommonRenderers.STRING,
        )
    }
}
