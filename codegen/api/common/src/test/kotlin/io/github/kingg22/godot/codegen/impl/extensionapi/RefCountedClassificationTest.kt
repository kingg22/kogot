package io.github.kingg22.godot.codegen.impl.extensionapi

import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.noop.NoOpPackageRegistry
import io.github.kingg22.godot.codegen.models.config.CodegenOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * [Context.isRefCounted] decides which generated wrappers own an engine reference (issue #114, #191). It reads the
 * engine's own `is_refcounted` flag; these tests pin that the flag agrees with the class hierarchy, so a JSON
 * that ever disagreed (a class flagged without inheriting `RefCounted`, or the reverse) fails here instead of
 * silently generating a wrapper that leaks or over-releases its reference.
 */
class RefCountedClassificationTest {

    @ParameterizedTest
    @ValueSource(strings = ["v4_6_1", "v4_6_3", "v4_7_0", "v4_7_2"])
    fun `is_refcounted agrees with inheriting RefCounted for every engine class`(godotVersion: String) {
        val context = buildContext(godotVersion)
        val classes = context.model.engineClassesByName

        fun inheritsRefCounted(name: String): Boolean =
            generateSequence(name) { classes[it]?.inherits }.take(classes.size + 1).any { it == "RefCounted" }

        val mismatches = classes.keys.filter { context.isRefCounted(it) != inheritsRefCounted(it) }
        assertEquals(emptyList<String>(), mismatches, "is_refcounted disagrees with the hierarchy [$godotVersion]")
    }

    @ParameterizedTest
    @ValueSource(strings = ["v4_6_1", "v4_7_2"])
    fun `classifies representative classes`(godotVersion: String) {
        val context = buildContext(godotVersion)

        listOf("RefCounted", "Resource", "Texture2D", "Image", "JSON", "Tween").forEach {
            assertTrue(context.isRefCounted(it), "$it should be RefCounted [$godotVersion]")
        }
        // Manually managed objects, builtins and unknown names own no reference.
        listOf("Object", "Node", "Node2D", "MainLoop", "Vector2", "String", "Array", "NotAClass").forEach {
            assertFalse(context.isRefCounted(it), "$it should not be RefCounted [$godotVersion]")
        }
    }

    private fun buildContext(godotVersion: String): Context = Context.buildFromApi(
        api = loadExtensionApi(godotVersion),
        rootPackage = "test",
        packageRegistryFactory = { _, _ -> NoOpPackageRegistry("test") },
        options = CodegenOptions(),
    )
}
