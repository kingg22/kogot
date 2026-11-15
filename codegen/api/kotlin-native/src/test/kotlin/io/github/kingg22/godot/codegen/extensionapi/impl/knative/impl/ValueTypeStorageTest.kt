package io.github.kingg22.godot.codegen.extensionapi.impl.knative.impl

import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.KotlinNativeTypeResolver
import io.github.kingg22.godot.codegen.extensionapi.impl.noop.EmptyContext
import io.github.kingg22.godot.codegen.extensionapi.impl.noop.NoOpPackageRegistry
import io.github.kingg22.godot.codegen.models.config.BuildConfiguration
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinLayout
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The storage a value-type builtin allocates is freed by a Cleaner, and only that storage (#216): a wrapper over a
 * pointer it was given (an argument slot, a return buffer) never frees it.
 */
class ValueTypeStorageTest {
    private val resolver = KotlinNativeTypeResolver()
    private val generator = BuiltinClassImplGen(resolver, BuiltinMethodImplGen(resolver))
    private val testContext: Context = EmptyContext(NoOpPackageRegistry("test.api"))

    private fun builtin(name: String, hasDestructor: Boolean, size: Int): ResolvedBuiltinClass {
        val raw = Json.decodeFromString<BuiltinClass>(
            """{"name": "$name", "is_keyed": false, "has_destructor": $hasDestructor}""",
        )
        val layout = ResolvedBuiltinLayout(
            className = name,
            buildConfiguration = BuildConfiguration.defaultFor("single", 64),
            size = size,
            align = 4,
            memberOffsets = emptyMap(),
            memberMeta = emptyMap(),
        )
        return ResolvedBuiltinClass(raw, layout, emptyList(), emptyList())
    }

    private fun generate(cls: ResolvedBuiltinClass): String = context(testContext) {
        generator.configureStorageBackedBuiltin(cls, TypeSpec.classBuilder(cls.name)).build().toString()
    }

    @Test
    fun `a value type frees the storage it allocated with a Cleaner, never a borrowed one`() {
        val code = generate(builtin("Vector2", hasDestructor = false, size = 8))

        assertTrue(code.contains("private val storageCleaner: kotlin.native.ref.Cleaner?"), code)
        assertTrue(
            code.contains(
                "if (rawPtr == null) kotlin.native.ref.createCleaner(storage) { " +
                    "kotlinx.cinterop.nativeHeap.free(it.rawValue) } else null",
            ),
            code,
        )
        assertTrue(code.contains("@kotlin.OptIn(kotlin.experimental.ExperimentalNativeApi::class)"), code)
    }

    @Test
    fun `a closeable builtin keeps close() as its release, with no Cleaner`() {
        val code = generate(builtin("String", hasDestructor = true, size = 8))

        assertFalse(code.contains("storageCleaner"), code)
        assertTrue(code.contains("closed"), code)
    }
}
