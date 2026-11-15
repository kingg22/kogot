package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.CodeBlock
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.KotlinNativeTypeResolver
import io.github.kingg22.godot.codegen.extensionapi.impl.noop.EmptyContext
import io.github.kingg22.godot.codegen.extensionapi.impl.noop.NoOpPackageRegistry
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A default argument that builds a closeable builtin (`GodotArray()`, `StringName("")`, ...) was built at every call
 * site and never closed (#232). The parameter now defaults to a marker and the body builds, uses and frees the real
 * value per call.
 */
class NativeMethodGeneratorDefaultsTest {
    private val resolver = KotlinNativeTypeResolver()
    private val generator = NativeMethodGenerator(resolver, DefaultValueGenerator(resolver))
    private val testContext: Context = EmptyContext(NoOpPackageRegistry("test.api"))

    private fun method(returnType: String?, vararg args: MethodArg) = UtilityFunction(
        name = "f",
        returnType = returnType,
        category = "general",
        isVararg = false,
        hash = 0,
        arguments = args.toList(),
    )

    /** A body that reads every argument by the name it is given, like the real body generator. */
    private fun build(method: UtilityFunction): String = context(testContext) {
        generator.buildMethod(
            method,
            "Test",
            codeBody = CodeBlock.of("TODO()\n"),
            bodyFor = { args -> CodeBlock.of("call(%L)\n", args.joinToString(", ") { it.name }) },
        ).toString()
    }

    @Test
    fun `a closeable builtin default becomes a marker and the body owns a fresh value`() {
        val code =
            build(method(null, MethodArg("signal", "String"), MethodArg("arguments", "Array", defaultValue = "[]")))

        assertTrue(code.contains("= test.api.DefaultArguments.array()"), code)
        assertTrue(code.contains("withDefaultArgument(arguments, test.api.DefaultArguments.array(), {"), code)
        assertTrue(code.contains("{ argumentsGd ->"), code)
        assertTrue(code.contains("call(signal, argumentsGd)"), code)
        assertFalse(code.contains("TODO()"), code)
    }

    @Test
    fun `every closeable kind has a marker, value types and objects do not`() {
        val code = build(
            method(
                "String",
                MethodArg("name", "StringName", defaultValue = "&\"\""),
                MethodArg("text", "String", defaultValue = "\"\""),
                MethodArg("callback", "Callable", defaultValue = "Callable()"),
                MethodArg("options", "Dictionary", defaultValue = "{}"),
                MethodArg("list", "PackedStringArray", defaultValue = "PackedStringArray()"),
                MethodArg("count", "int", defaultValue = "0"),
            ),
        )

        assertTrue(code.contains("name: test.api.StringName = test.api.DefaultArguments.stringName,"), code)
        assertTrue(code.contains("text: test.api.GodotString = test.api.DefaultArguments.string,"), code)
        assertTrue(code.contains("callback: test.api.Callable = test.api.DefaultArguments.callable,"), code)
        assertTrue(code.contains("= test.api.DefaultArguments.dictionary(),"), code)
        assertTrue(
            code.contains("list: test.api.PackedStringArray = test.api.DefaultArguments.packedStringArray,"),
            code,
        )
        assertTrue(code.contains("count: kotlin.Long = 0L,"), code)
        assertTrue(code.contains("call(nameGd, textGd, callbackGd, optionsGd, listGd, count)"), code)
        // Returns a value from inside the blocks: the end of each block is unreachable to the compiler.
        assertTrue(code.contains("@kotlin.Suppress(\"UNREACHABLE_CODE\")"), code)
    }

    @Test
    fun `without bodyFor the defaults are the plain values, as before`() {
        val code = context(testContext) {
            generator.buildMethod(
                method(null, MethodArg("arguments", "Array", defaultValue = "[]")),
                "Test",
                codeBody = CodeBlock.of("TODO()\n"),
            ).toString()
        }

        assertFalse(code.contains("DefaultArguments"), code)
        assertFalse(code.contains("withDefaultArgument"), code)
    }
}
