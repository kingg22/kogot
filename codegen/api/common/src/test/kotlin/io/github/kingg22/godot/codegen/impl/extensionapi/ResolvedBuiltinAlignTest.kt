package io.github.kingg22.godot.codegen.impl.extensionapi

import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.noop.NoOpPackageRegistry
import io.github.kingg22.godot.codegen.models.config.BuildConfiguration
import io.github.kingg22.godot.codegen.models.config.CodegenOptions
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

private val GODOT_PRIMITIVE_TYPES = setOf("Nil", "bool", "int", "float")

class ResolvedBuiltinAlignTest {

    /**
     * For every builtin with member offsets, size must be a multiple of align.
     * This verifies that resolveBuiltinAlign never produces an under-aligned value.
     */
    @ParameterizedTest
    @EnumSource(BuildConfiguration::class)
    fun `all builtin layouts satisfy size is multiple of align`(config: BuildConfiguration) {
        val context = buildTestContext(config)

        val tests = context.model.builtins
            .filter { it.name !in GODOT_PRIMITIVE_TYPES }
            .map { builtin ->
                val layout = builtin.layout ?: return@map Executable {}
                val size = layout.size
                val align = layout.align
                Executable {
                    assertEquals(0, size % align) {
                        "${builtin.name} [${config.jsonName}]: size=$size is not a multiple of align=$align"
                    }
                }
            }

        assertAll("Layout consistency failures", tests)
    }

    /**
     * Concrete expected values for float_32 — the most common build config.
     * Values derived from JSON member metas (float -> align=4 for single precision).
     */
    @Test
    fun `float_32 specific align values are correct`() {
        val context = buildTestContext(BuildConfiguration.FLOAT_32)
        val byName = context.model.builtins.mapNotNull { b ->
            b.layout?.let { b.name to it }
        }.toMap()

        fun assertAlign(name: String, expectedAlign: Int) {
            val layout = checkNotNull(byName[name]) { "Missing layout for $name" }
            assertEquals(
                expectedAlign,
                layout.align,
                "$name [float_32]: expected align=$expectedAlign, got ${layout.align}",
            )
        }

        // Primitives / float-based -> align 4
        assertAlign("Vector2", 4)
        assertAlign("Vector3", 4)
        assertAlign("Vector4", 4)
        assertAlign("Color", 4)
        assertAlign("Quaternion", 4)
        assertAlign("Basis", 4)
        assertAlign("Transform3D", 4)
        assertAlign("Projection", 4)
        // int32-based -> align 4
        assertAlign("Vector2i", 4)
        assertAlign("Vector3i", 4)
        // Opaque pointer-based, 32-bit pointers in float_32 -> align 4
        assertAlign("String", 4)
        assertAlign("StringName", 4)
        assertAlign("NodePath", 4)
        // RID is uint64 -> always 8
        assertAlign("RID", 8)
    }

    /**
     * In double_64, all real-containing builtins must have align=8 (double).
     */
    @Test
    fun `double_64 real-containing builtins have align 8`() {
        val context = buildTestContext(BuildConfiguration.DOUBLE_64)
        val byName = context.model.builtins.mapNotNull { b ->
            b.layout?.let { b.name to it }
        }.toMap()

        listOf("Vector2", "Vector3", "Vector4", "Color", "Basis", "Transform3D").forEach { name ->
            val align = byName[name]?.align
            assertEquals(8, align, "$name [double_64]: expected align=8, got $align")
        }
    }

    /**
     * int32-based types must always have align=4 regardless of build config.
     * Their members are always int32, never affected by float/double precision.
     */
    @ParameterizedTest
    @EnumSource(BuildConfiguration::class)
    fun `int32-based types always have align 4`(config: BuildConfiguration) {
        val context = buildTestContext(config)
        val byName = context.model.builtins.mapNotNull { b ->
            b.layout?.let { b.name to it }
        }.toMap()

        listOf("Vector2i", "Vector3i", "Vector4i").forEach { name ->
            val align = byName[name]?.align ?: return@forEach // not all configs have offsets
            assertEquals(4, align, "$name [${config.jsonName}]: int32 type must have align=4, got $align")
        }
    }

    @Test
    fun `RID always has align 8 across all build configs`() {
        BuildConfiguration.entries.forEach { config ->
            val context = buildTestContext(config)
            val rid = context.model.builtins.firstOrNull { it.name == "RID" }
            val align = rid?.layout?.align
            if (align != null) {
                assertEquals(8, align, "RID [${config.jsonName}]: must always have align=8, got $align")
            }
        }
    }

    // ── Test fixture builder ──────────────────────────────────────────────────

    private fun buildTestContext(config: BuildConfiguration): Context {
        // Loads the real extension_api.json + extension_built_sizes_offsets.json
        // from src/test/resources (or wherever your test resources live)
        val extensionApi = loadExtensionApi()
        return Context.buildFromApi(
            api = extensionApi,
            rootPackage = "test",
            packageRegistryFactory = { _, _ -> NoOpPackageRegistry("test") },
            options = CodegenOptions(buildConfiguration = config),
        )
    }
}
