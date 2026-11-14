package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.ClassName
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.noop.EmptyContext
import io.github.kingg22.godot.codegen.extensionapi.impl.noop.NoOpPackageRegistry
import io.github.kingg22.godot.codegen.models.extensionapi.EngineClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedEngineClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NativeEngineClassNodeGeneratorTest {
    private val generator = NativeEngineClassNodeGenerator { ClassName("test.binding", "ClassNode") }
    private val testContext: Context = EmptyContext(NoOpPackageRegistry("test.api"))

    private fun engineClass(name: String, inherits: String?, singleton: Boolean = false) = ResolvedEngineClass(
        raw = EngineClass(
            name = name,
            isRefcounted = false,
            isInstantiable = true,
            apiType = "core",
            inherits = inherits,
        ),
        isSingleton = singleton,
        isSingletonExtensible = false,
        enums = emptyList(),
        allMethods = emptyList(),
        allProperties = emptyList(),
        allSignals = emptyList(),
    )

    private fun generate(cls: ResolvedEngineClass, children: List<ResolvedEngineClass> = emptyList()): String =
        context(testContext) { generator.generateFile(cls, children) }.toString()

    @Test
    fun `a node builds its own wrapper from the pointer and exposes its godot name`() {
        val code = generate(engineClass("Node", "Object"))

        assertTrue(code.contains("internal object NodeClassNode : ClassNode<GodotObject>"), code)
        assertTrue(code.contains("override val godotName: String = \"Node\""), code)
        assertTrue(code.contains("create(ptr: COpaquePointer): GodotObject = Node(ptr)"), code)
    }

    @Test
    fun `a node only lists its direct children, in a stable order`() {
        val children = listOf(engineClass("Node2D", "Node"), engineClass("CanvasLayer", "Node"))
        val code = generate(engineClass("Node", "Object"), children)

        val canvasLayer = code.indexOf("\"CanvasLayer\" -> CanvasLayerClassNode")
        val node2d = code.indexOf("\"Node2D\" -> Node2DClassNode")
        assertTrue(canvasLayer in 0 until node2d, code)
        assertTrue(code.contains("else -> null"), code)
    }

    @Test
    fun `a leaf node has no children`() {
        val code = generate(engineClass("Sprite2D", "Node2D"))

        assertFalse(code.contains("when"), code)
        assertTrue(code.contains("ClassNode<GodotObject>? = null"), code)
    }

    @Test
    fun `the root builds a GodotObject with its renamed class`() {
        val code = generate(engineClass("Object", null), listOf(engineClass("Node", "Object")))

        assertTrue(code.contains("object GodotObjectClassNode"), code)
        assertTrue(code.contains("= GodotObject(ptr)"), code)
        assertTrue(code.contains("override val godotName: String = \"Object\""), code)
    }

    @Test
    fun `a singleton is the companion instance, never a new object`() {
        val code = generate(engineClass("Engine", "Object", singleton = true))

        assertTrue(code.contains("= Engine.instance"), code)
        assertFalse(code.contains("Engine(ptr)"), code)
    }

    @Test
    fun `the generated file is named after the class node`() {
        val file = context(testContext) { generator.generateFile(engineClass("Node", "Object"), emptyList()) }

        assertEquals("NodeClassNode", file.name)
        assertEquals("test.api", file.packageName)
    }
}
