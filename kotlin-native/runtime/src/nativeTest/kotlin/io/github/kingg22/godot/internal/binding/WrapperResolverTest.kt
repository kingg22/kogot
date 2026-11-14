package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtr
import kotlinx.cinterop.nativeNullPtr
import kotlinx.cinterop.toCPointer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(InternalBinding::class)
class WrapperResolverTest {
    /** Counts every wrapper it builds, like an `init {}` counter would. */
    private class Wrapper(val kind: String)

    private class Node(
        override val godotName: String,
        private val created: MutableList<String>,
        private val children: MutableMap<String, Node> = mutableMapOf(),
    ) : ClassNode<Wrapper> {
        fun add(child: Node) {
            children[child.godotName] = child
        }

        override fun create(ptr: GDExtensionObjectPtr): Wrapper {
            created += godotName
            return Wrapper(godotName)
        }

        override fun child(godotName: String): ClassNode<Wrapper>? = children[godotName]
    }

    private val created = mutableListOf<String>()
    private val ptr: GDExtensionObjectPtr = 1L.toCPointer<kotlinx.cinterop.CPointed>()!!

    // Object > Node > Node2D > Sprite2D, Object > Resource
    private val parents = mapOf(
        "Node" to "Object",
        "Node2D" to "Node",
        "Sprite2D" to "Node2D",
        "Resource" to "Object",
        "Foreign" to "Sprite2D",
        "ForeignRoot" to "Unknown",
        "Unknown" to "ForeignRoot",
    )
    private val root = Node("Object", created)

    init {
        val node = Node("Node", created)
        val node2d = Node("Node2D", created)
        node2d.add(Node("Sprite2D", created))
        node.add(node2d)
        root.add(node)
        root.add(Node("Resource", created))
    }

    private var parentCalls = 0
    private var nameCalls = 0

    private fun resolver(className: String?) = WrapperResolver(
        root,
        classNameOf = {
            nameCalls++
            className
        },
        parentOf = {
            parentCalls++
            parents[it]
        },
    )

    @Test
    fun `resolves the most derived known class`() {
        assertEquals("Sprite2D", resolver("Sprite2D").create(ptr).kind)
        assertEquals("Node", resolver("Node").create(ptr).kind)
        assertEquals("Resource", resolver("Resource").create(ptr).kind)
    }

    @Test
    fun `an unknown class resolves to its nearest known ancestor`() {
        assertEquals("Sprite2D", resolver("Foreign").create(ptr).kind)
    }

    @Test
    fun `the root class and a missing class name resolve to the root`() {
        assertEquals("Object", resolver("Object").create(ptr).kind)
        assertEquals("Object", resolver(null).create(ptr).kind)
        assertEquals("Object", resolver("").create(ptr).kind)
    }

    @Test
    fun `a parent cycle ends at the root instead of looping`() {
        assertEquals("Object", resolver("ForeignRoot").create(ptr).kind)
    }

    @Test
    fun `the ancestor chain is resolved once per class name`() {
        val resolver = resolver("Sprite2D")
        val first = resolver.nodeFor(ptr)
        val parentCallsAfterFirst = parentCalls
        repeat(10) { assertSame(first, resolver.nodeFor(ptr)) }
        assertEquals(parentCallsAfterFirst, parentCalls)
        assertEquals(11, nameCalls)
    }

    @Test
    fun `resolving builds exactly one wrapper per create call`() {
        val resolver = resolver("Node2D")
        val nodes = List(3) { resolver.nodeFor(ptr) }
        assertEquals(3, nodes.size)
        assertEquals(0, created.size)
        val wrapper = resolver.create(ptr)
        assertEquals("Node2D", wrapper.kind)
        assertEquals(listOf("Node2D"), created)
    }
}
