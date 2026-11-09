package io.github.kingg22.kogot.processor

import io.github.kingg22.kogot.processor.diagnostics.DiagnosticCode
import io.github.kingg22.kogot.processor.generators.kotlin.GodotBindingGenerator
import io.github.kingg22.kogot.processor.model.AnnotationInfo
import io.github.kingg22.kogot.processor.model.ClassInfo
import io.github.kingg22.kogot.processor.model.PropertyInfo
import io.github.kingg22.kogot.processor.model.SignalDeclaration
import io.github.kingg22.kogot.processor.model.SignalParameterInfo
import io.github.kingg22.kogot.processor.model.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** `@RegisterSignal` on typed signals: parameters inferred from the function type, and their validation. */
class SignalRegistrationTest {
    private val typedSignalType = TypeInfo("io.github.kingg22.godot.api.signal.TypedSignal", "TypedSignal")
    private val builtinSignalType = TypeInfo("io.github.kingg22.godot.api.builtin.Signal", "Signal")

    private fun registerSignal(name: String = "", params: List<AnnotationInfo> = emptyList()) = AnnotationInfo(
        qualifiedName = "io.github.kingg22.godot.api.annotations.RegisterSignal",
        shortName = "RegisterSignal",
        arguments = mapOf("name" to name, "params" to params),
    )

    private fun param(type: String, name: String, vararg options: Pair<String, Any>) = AnnotationInfo(
        qualifiedName = "io.github.kingg22.godot.api.annotations.RegisterSignal.Param",
        shortName = "Param",
        arguments = mapOf("type" to "io.github.kingg22.godot.api.builtin.Variant.Type.$type", "name" to name) +
            // KSP reports every argument, defaults included.
            mapOf(
                "className" to "",
                "hint" to "io.github.kingg22.godot.api.PropertyHint.NONE",
                "hintString" to "",
                "usages" to emptyList<String>(),
            ) + options,
    )

    private fun type(qualifiedName: String, nullable: Boolean = false) =
        TypeInfo(qualifiedName, qualifiedName.substringAfterLast('.'), isNullable = nullable)

    private fun typed(
        name: String,
        vararg parameters: SignalParameterInfo,
        annotation: AnnotationInfo = registerSignal(),
    ) = PropertyInfo(
        name = name,
        type = typedSignalType,
        isMutable = false,
        annotations = listOf(annotation),
        signal = SignalDeclaration.Typed(parameters.toList()),
    )

    private fun godotClass(vararg properties: PropertyInfo) = ClassInfo(
        qualifiedName = "io.github.kingg22.game.Player",
        shortName = "Player",
        packageName = "io.github.kingg22.game",
        supertypes = listOf(TypeInfo("io.github.kingg22.godot.api.core.Node", "Node")),
        annotations = listOf(AnnotationInfo("io.github.kingg22.godot.api.annotations.Godot", "Godot")),
        properties = properties.toList(),
        filePath = "/project/src/Player.kt",
    )

    private fun ClassInfo.registration(): String =
        GodotBindingGenerator().generate(listOf(this)).files.joinToString("\n") { it.content }

    private fun ClassInfo.errorCodes() = validateSignals().map { it.code }

    @Test
    fun `typed signal parameters are registered from the function type`() {
        val player = godotClass(
            typed(
                "hit",
                SignalParameterInfo("damage", type("kotlin.Long")),
                SignalParameterInfo("ratio", type("kotlin.Float")),
                SignalParameterInfo("small", type("kotlin.Short")),
                SignalParameterInfo("tag", type("kotlin.String")),
                SignalParameterInfo("at", type("io.github.kingg22.godot.api.builtin.Vector2")),
                SignalParameterInfo("raw", type("io.github.kingg22.godot.api.builtin.Variant", nullable = true)),
                SignalParameterInfo(
                    "by",
                    type("io.github.kingg22.godot.api.core.Node", nullable = true),
                    godotClassName = "Node",
                ),
                // Kotlin name differs from the Godot one: the class name comes from @GodotClass.
                SignalParameterInfo(
                    "rpc",
                    type("io.github.kingg22.godot.api.core.Jsonrpc"),
                    godotClassName = "JSONRPC",
                ),
                // A GodotObject whose registered class is unknown: any object.
                SignalParameterInfo("any", type("io.github.kingg22.game.Unregistered"), godotClassName = ""),
            ),
        )

        assertEquals(emptyList<DiagnosticCode>(), player.errorCodes())
        val code = player.registration()
        assertTrue(code.contains("Param(Variant.Type.INT, \"damage\")"), code)
        assertTrue(code.contains("Param(Variant.Type.FLOAT, \"ratio\")"), code)
        assertTrue(code.contains("Param(Variant.Type.INT, \"small\")"), code)
        assertTrue(code.contains("Param(Variant.Type.STRING, \"tag\")"), code)
        assertTrue(code.contains("Param(Variant.Type.VECTOR2, \"at\")"), code)
        assertTrue(code.contains("Param(Variant.Type.NIL, \"raw\")"), code)
        assertTrue(code.contains("Param(Variant.Type.OBJECT, \"by\", className = \"Node\")"), code)
        assertTrue(code.contains("Param(Variant.Type.OBJECT, \"rpc\", className = \"JSONRPC\")"), code)
        assertTrue(code.contains("Param(Variant.Type.OBJECT, \"any\")"), code)
        assertTrue(code.contains("name = \"hit\""), code)
    }

    @Test
    fun `a typed signal without parameters registers none`() {
        val code = godotClass(typed("ready")).registration()

        assertTrue(Regex("""RegisterSignal\(\s*name = "ready",""").containsMatchIn(code), code)
    }

    @Test
    fun `the builtin Signal keeps its explicit params and name`() {
        val legacy = PropertyInfo(
            name = "punch",
            type = builtinSignalType,
            annotations = listOf(registerSignal(name = "on_punch", params = listOf(param("INT", "value")))),
            signal = SignalDeclaration.Builtin,
        )
        val player = godotClass(legacy)

        assertEquals(emptyList<DiagnosticCode>(), player.errorCodes())
        val code = player.registration()
        assertTrue(code.contains("Param(Variant.Type.INT, \"value\")"), code)
        assertTrue(code.contains("name = \"on_punch\""), code)
    }

    @Test
    fun `the builtin Signal forwards className, hint, hintString and usages, and only those set`() {
        val legacy = PropertyInfo(
            name = "picked",
            type = builtinSignalType,
            annotations = listOf(
                registerSignal(
                    params = listOf(
                        param("OBJECT", "item", "className" to "Node"),
                        param(
                            "INT",
                            "count",
                            "hint" to "io.github.kingg22.godot.api.PropertyHint.RANGE",
                            "hintString" to "0,10",
                            "usages" to listOf(
                                "io.github.kingg22.godot.api.PropertyUsageFlags.STORAGE",
                                "io.github.kingg22.godot.api.PropertyUsageFlags.EDITOR",
                            ),
                        ),
                    ),
                ),
            ),
            signal = SignalDeclaration.Builtin,
        )

        val code = godotClass(legacy).registration()
        assertTrue(code.contains("Param(Variant.Type.OBJECT, \"item\", className = \"Node\")"), code)
        assertTrue(
            code.contains(
                "Param(Variant.Type.INT, \"count\", hint = PropertyHint.RANGE, hintString = \"0,10\", " +
                    "usages = arrayOf(PropertyUsageFlags.STORAGE, PropertyUsageFlags.EDITOR))",
            ),
            code,
        )
    }

    @Test
    fun `RegisterSignal on a non signal type is an error`() {
        val wrong = PropertyInfo(
            name = "health",
            type = type("kotlin.Int"),
            annotations = listOf(registerSignal()),
        )

        assertEquals(listOf(DiagnosticCode.INVALID_SIGNAL_PROPERTY_TYPE), godotClass(wrong).errorCodes())
    }

    @Test
    fun `a TypedSignal whose argument is not a function type is an error`() {
        val wrong = typed("hit").copy(signal = SignalDeclaration.Invalid("its type argument is unresolved"))

        assertEquals(listOf(DiagnosticCode.INVALID_SIGNAL_PROPERTY_TYPE), godotClass(wrong).errorCodes())
    }

    @Test
    fun `explicit params on a typed signal are redundant`() {
        val property = typed(
            "hit",
            SignalParameterInfo("damage", type("kotlin.Long")),
            annotation = registerSignal(params = listOf(param("INT", "damage"))),
        )

        assertEquals(listOf(DiagnosticCode.REDUNDANT_SIGNAL_PARAMS), godotClass(property).errorCodes())
    }

    @Test
    fun `a typed signal registered under another name is an error, its own name is fine`() {
        val renamed = typed("hit", annotation = registerSignal(name = "on_hit"))
        val same = typed("jump", annotation = registerSignal(name = "jump"))

        assertEquals(listOf(DiagnosticCode.SIGNAL_NAME_MISMATCH), godotClass(renamed, same).errorCodes())
    }

    @Test
    fun `unnamed parameters are an error`() {
        val property = typed(
            "hit",
            SignalParameterInfo("", type("kotlin.Long")),
            SignalParameterInfo("flag", type("kotlin.Boolean")),
        )

        val diagnostics = godotClass(property).validateSignals()
        assertEquals(listOf(DiagnosticCode.UNNAMED_SIGNAL_PARAMETER), diagnostics.map { it.code })
        assertTrue(diagnostics.single().mainMessage.contains("#1"))
    }

    @Test
    fun `unsupported and nullable value parameter types are errors`() {
        val property = typed(
            "hit",
            SignalParameterInfo("items", type("kotlin.collections.List")),
            SignalParameterInfo("damage", type("kotlin.Long", nullable = true)),
            // Rejected like the compiler plugin does: signed in Kotlin, unsigned in Godot.
            SignalParameterInfo("small", type("kotlin.Byte")),
        )

        assertEquals(List(3) { DiagnosticCode.INVALID_SIGNAL_PARAMETER_TYPE }, godotClass(property).errorCodes())
    }

    @Test
    fun `two properties registering the same signal name is an error`() {
        val typedHit = typed("hit")
        val legacyHit = PropertyInfo(
            name = "legacyHit",
            type = builtinSignalType,
            annotations = listOf(registerSignal(name = "hit")),
            signal = SignalDeclaration.Builtin,
        )

        assertEquals(listOf(DiagnosticCode.DUPLICATE_SIGNAL_NAME), godotClass(typedHit, legacyHit).errorCodes())
    }
}
