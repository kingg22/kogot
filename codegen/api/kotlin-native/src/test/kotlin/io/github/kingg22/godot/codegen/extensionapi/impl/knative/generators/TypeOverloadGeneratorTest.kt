package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.KotlinNativeTypeResolver
import io.github.kingg22.godot.codegen.extensionapi.impl.noop.EmptyContext
import io.github.kingg22.godot.codegen.extensionapi.impl.noop.NoOpPackageRegistry
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The `String` overloads of a `GodotString` method convert through a `GodotString` the overload itself creates or
 * receives: both must be closed, or every call leaks the `nativeHeap` slot and the engine string (#211).
 */
class TypeOverloadGeneratorTest {
    private val resolver = KotlinNativeTypeResolver()
    private val generator = TypeOverloadGenerator(resolver)
    private val testContext: Context = EmptyContext(NoOpPackageRegistry("test.api"))

    private fun godotString(): TypeName = context(testContext) { testContext.classNameForOrDefault("String") }

    private fun method(returnType: String?, vararg args: Pair<String, String>) = UtilityFunction(
        name = "f",
        returnType = returnType,
        category = "general",
        isVararg = false,
        hash = 0,
        arguments = args.map { (name, type) -> MethodArg(name, type) },
    )

    private fun original(returnType: TypeName?, vararg params: Pair<String, TypeName>): FunSpec = FunSpec.builder("f")
        .apply { params.forEach { (name, type) -> addParameter(name, type) } }
        .apply { returnType?.let { returns(it) } }
        .addStatement("TODO()")
        .build()

    private fun overloads(method: UtilityFunction, original: FunSpec): List<FunSpec> = context(testContext) {
        generator.buildOverloadsForMethod(method, original, TypeOverloadGenerator.GodotStringMapping)
    }

    /** The overloads that delegate (inline), as source: the original keeps its own body. */
    private fun List<FunSpec>.delegates(): List<String> =
        filter { KModifier.INLINE in it.modifiers }.map { it.toString() }

    @Test
    fun `a String return closes the GodotString it converts`() {
        val specs = overloads(method("String"), original(godotString()))

        val kotlinOverload = specs.single { it.name == "f" }
        assertEquals(STRING, kotlinOverload.returnType)
        val code = kotlinOverload.toString()
        assertTrue(code.contains("fAsGdStr().use { it.toKString() }"), code)
    }

    @Test
    fun `a String parameter closes the GodotString it wraps`() {
        val specs = overloads(method(null, "text" to "String"), original(null, "text" to godotString()))

        val code = specs.delegates().single()
        assertTrue(code.contains("GodotString(text).use { textGd ->"), code)
        assertTrue(code.contains("f(textGd)"), code)
        assertFalse(code.contains("f(GodotString(text))"), code)
    }

    @Test
    fun `parameters and return are all closed, the GodotString-returning overload hands its result to the caller`() {
        val specs = overloads(
            method("String", "a" to "String", "count" to "int", "b" to "String"),
            original(
                godotString(),
                "a" to godotString(),
                "count" to LONG,
                "b" to godotString(),
            ),
        )

        val (toKotlin, toGodot) = specs.filter { KModifier.INLINE in it.modifiers }.partition { it.name == "f" }
        val kotlinCode = toKotlin.single().toString()
        assertTrue(kotlinCode.contains("GodotString(a).use { aGd ->"), kotlinCode)
        assertTrue(kotlinCode.contains("GodotString(b).use { bGd ->"), kotlinCode)
        assertTrue(kotlinCode.contains("fAsGdStr(aGd, count, bGd).use { it.toKString() }"), kotlinCode)

        // Returns the GodotString: the caller owns it, so it must not be closed here.
        val godotCode = toGodot.single().toString()
        assertTrue(godotCode.contains("fAsGdStr(aGd, count, bGd)\n"), godotCode)
        assertFalse(godotCode.contains("toKString"), godotCode)
    }

    @Test
    fun `a local name never shadows another parameter`() {
        val specs = overloads(
            method(null, "text" to "String", "textGd" to "String"),
            original(null, "text" to godotString(), "textGd" to godotString()),
        )

        val code = specs.delegates().single()
        assertTrue(code.contains("GodotString(text).use { textGd_ ->"), code)
        assertTrue(code.contains("GodotString(textGd).use { textGdGd ->"), code)
        assertTrue(code.contains("f(textGd_, textGdGd)"), code)
    }

    // ── Variant parameters (#219) ────────────────────────────────────────────────

    private fun variant(): TypeName = context(testContext) { testContext.classNameForOrDefault("Variant") }

    private fun variantOverloads(method: UtilityFunction, original: FunSpec): List<FunSpec> = context(testContext) {
        generator.buildOverloadsForMethod(method, original, TypeOverloadGenerator.VariantMapping)
    }

    @Test
    fun `an Any parameter frees the Variant it converts, never the caller's`() {
        val specs = variantOverloads(method(null, "item" to "Variant"), original(null, "item" to variant()))

        val code = specs.single { it.parameters.single().type != variant() }.toString()
        assertTrue(code.contains("withVariant(item) { itemGd ->"), code)
        assertTrue(code.contains("f(itemGd)"), code)
        assertFalse(code.contains("toVariant()"), code)
    }

    @Test
    fun `an Any vararg frees every Variant it converts`() {
        val method = UtilityFunction(
            name = "f",
            returnType = null,
            category = "general",
            isVararg = true,
            hash = 0,
            arguments = listOf(MethodArg("name", "StringName")),
        )
        val stringName = context(testContext) { testContext.classNameForOrDefault("StringName") }
        val original = FunSpec.builder("f")
            .addParameter("name", stringName)
            .addParameter("args", variant(), KModifier.VARARG)
            .addStatement("TODO()")
            .build()

        val code = variantOverloads(method, original).single { it.parameters.size == 3 }.toString()
        assertTrue(code.contains("withVariants(first, args) { argsGd ->"), code)
        assertTrue(code.contains("f(name, *argsGd)"), code)
        assertFalse(code.contains("toVariant()"), code)
    }

    // ── Constructors (#219) ──────────────────────────────────────────────────────

    @Test
    fun `a String constructor frees its GodotString after the engine constructor, not in the delegation`() {
        val constructor = Json.decodeFromString<BuiltinClass.Constructor>(
            """{"index": 5, "arguments": [{"name": "code", "type": "String"}, {"name": "alpha", "type": "float"}]}""",
        )
        val original = FunSpec.constructorBuilder()
            .callThisConstructor("null")
            .addParameter("code", godotString())
            .addParameter("alpha", DOUBLE)
            .addStatement("init(code.rawPtr, alpha)")
            .build()

        val overload = context(testContext) {
            generator.buildOverloadsForConstructor(
                constructor,
                original,
                TypeOverloadGenerator.GodotStringMapping,
            ) { locals ->
                CodeBlock.of("init(%N.rawPtr, alpha)\n", locals.getValue(0))
            }
        }

        val code = checkNotNull(overload).toString()
        assertEquals(STRING, overload.parameters.first().type)
        assertTrue(code.contains(": this(null)"), code)
        assertFalse(code.contains("this(test.api.GodotString(code)"), code)
        assertTrue(code.contains("GodotString(code).use { codeGd ->"), code)
        assertTrue(code.contains("init(codeGd.rawPtr, alpha)"), code)
    }
}
