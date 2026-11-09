package io.github.kingg22.kogot.compiler.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
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

    /**
     * The value's static type (`Any?`, an unbounded `T`, an interface...) may or may not be Variant compatible: it is
     * only checked at runtime. Args: type, position, how to fix it. Suppressible by name.
     */
    val MUST_BE_VARIANT_UNKNOWN_TYPE: KtDiagnosticFactory3<ConeKotlinType, String, String> = KtDiagnosticFactory3(
        "MUST_BE_VARIANT_UNKNOWN_TYPE",
        Severity.WARNING,
        SourceElementPositioningStrategies.DEFAULT,
        KtElement::class,
        KogotDiagnosticRenderers,
    )

    /** A reference (`::anyToVariant`) to a function with a `@MustBeVariant` contract: calls through it are unchecked. */
    val MUST_BE_VARIANT_UNCHECKED_REFERENCE: KtDiagnosticFactory1<String> = KtDiagnosticFactory1(
        "MUST_BE_VARIANT_UNCHECKED_REFERENCE",
        Severity.WARNING,
        SourceElementPositioningStrategies.DEFAULT,
        KtElement::class,
        KogotDiagnosticRenderers,
    )

    /** A property delegated to `signal(...)` without `@RegisterSignal`: Godot never learns the signal. Args: name. */
    val SIGNAL_NOT_REGISTERED: KtDiagnosticFactory1<String> = KtDiagnosticFactory1(
        "SIGNAL_NOT_REGISTERED",
        Severity.ERROR,
        SourceElementPositioningStrategies.DECLARATION_NAME,
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
        map.put(
            KogotDiagnostics.MUST_BE_VARIANT_UNKNOWN_TYPE,
            "The value for {1} has the open type ''{0}'': whether it is Variant compatible " +
                "is only known at runtime. {2}",
            FirDiagnosticRenderers.RENDER_TYPE,
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        map.put(
            KogotDiagnostics.MUST_BE_VARIANT_UNCHECKED_REFERENCE,
            "Calls through a reference to ''{0}'' bypass its @MustBeVariant contract " +
                "and are only checked at runtime. " +
                "Call it directly, or suppress with @Suppress(\"MUST_BE_VARIANT_UNCHECKED_REFERENCE\").",
            CommonRenderers.STRING,
        )
        map.put(
            KogotDiagnostics.SIGNAL_NOT_REGISTERED,
            "Signal ''{0}'' is not registered: annotate it with @RegisterSignal, " +
                "or Godot rejects every emit and connect.",
            CommonRenderers.STRING,
        )
    }
}
