@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

package io.github.kingg22.godot.test.downcast

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.annotations.Export
import io.github.kingg22.godot.api.annotations.ExportMethod
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.annotations.RegisterSignal
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.SceneTree
import io.github.kingg22.godot.api.core.node.Area2D
import io.github.kingg22.godot.api.core.node.Button
import io.github.kingg22.godot.api.core.node.Node2D
import io.github.kingg22.godot.api.core.node.Sprite2D
import io.github.kingg22.godot.api.core.refcounted.Mesh
import io.github.kingg22.godot.api.core.refcounted.PackedScene
import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.api.signal.connect
import io.github.kingg22.godot.api.signal.signal
import io.github.kingg22.godot.api.singleton.Engine
import io.github.kingg22.godot.binding.instantiate
import io.github.kingg22.godot.castTo
import io.github.kingg22.godot.castToOrNull
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.ObjectBinding
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** How many instances of each `@Godot` class of [DowncastTest] were built: casting and boxing must never add one. */
object DowncastCounters {
    var node = 0
    var resource = 0
    var base = 0
    var baseEnteredTree = 0
}

@Godot
class DowncastNode(nativePtr: COpaquePointer) : Node(nativePtr) {
    init {
        DowncastCounters.node++
    }
}

/**
 * A user class that extends another user class: `Derived : Base : Node`. Base has one of each member a subclass
 * inherits (a virtual override, a method, a property, a signal), so the tests see that Derived gets them all.
 */
@Godot
open class DowncastBase(nativePtr: COpaquePointer) : Node(nativePtr) {
    init {
        DowncastCounters.base++
    }

    @RegisterSignal
    val baseHint by signal()

    @Export var baseValue: Int = 3

    @ExportMethod
    fun baseGreeting(): Int = 7

    override fun _enterTree() {
        DowncastCounters.baseEnteredTree++
    }
}

@Godot
class DowncastDerived(nativePtr: COpaquePointer) : DowncastBase(nativePtr)

@Godot
class DowncastResource(nativePtr: COpaquePointer) : Resource(nativePtr) {
    init {
        DowncastCounters.resource++
    }
}

/**
 * Tests of issue #206: an object read back from a `Variant`, a signal, a `Callable`, a collection or an engine return
 * is the one canonical wrapper of its most derived class, never a second instance.
 *
 * The `@Godot` classes above count their constructions in `init {}`, so "no new instance" is measured, not assumed.
 *
 * Not ported (the test host project has no assets): the `res://icon.svg` reference-count check and the `#213` check
 * (`res://probe_mesh.tres`); and the one-frame-later quit of the live scene, which needs asynchronous progress.
 */
class DowncastTest {
    private val tree: SceneTree get() = Engine.instance.getMainLoop().castTo()

    /** The node the test runner lives in: the last child of the root (the root itself is busy setting up its children). */
    private val host: Node get() = tree.root.let { it.getChild(it.getChildCount() - 1) }

    // --- variants -----------------------------------------------------------------------------------------------

    @Test
    fun variantHoldingSprite2DGivesThatSprite2DTheSameWrapperEveryTime() {
        val sprite = Sprite2D()
        Variant.from(sprite).use { variant ->
            val first = variant.getValue<Node2D>()
            assertTrue(first is Sprite2D, "got ${first::class.simpleName}")
            assertSame(sprite, first, "the constructed Sprite2D is not the canonical one")
            assertSame(first, variant.getValue<Node2D>(), "a second read gave another instance")
            assertSame(first, variant.toObject(), "toObject() gave another instance")
        }
        sprite.queueFree()
    }

    @Test
    fun getValueOrNullToUnrelatedClassIsNullAndGetValueThrowsClassCastException() {
        val node = Node()
        Variant.from(node).use { variant ->
            assertNull(variant.getValueOrNull<Button>(), "a Node read as Button")
            val _ = assertFailsWith<ClassCastException>("getValue<Button>()") { variant.getValue<Button>() }
            assertSame(node, variant.getValueOrNull<Node>(), "the Node did not come back")
        }
        node.queueFree()
    }

    @Test
    fun castToAndCastToOrNullBehaveLikeAsAndAsOrNull() {
        val sprite = Sprite2D()
        val asNode: Node = sprite
        assertSame(sprite, asNode.castToOrNull<Sprite2D>(), "castToOrNull<Sprite2D>")
        assertNull(asNode.castToOrNull<Button>(), "castToOrNull<Button> was not null")
        val _ = assertFailsWith<ClassCastException>("castTo<Button>()") { asNode.castTo<Button>() }
        sprite.queueFree()
    }

    // --- engine returns -----------------------------------------------------------------------------------------

    @Test
    fun getChildGivesTheSprite2DThatWasAddedNotABareNode() {
        val parent = Node()
        val child = Sprite2D()
        parent.addChild(node = child)
        val back = parent.getChild(0)
        assertTrue(back is Sprite2D, "got ${back::class.simpleName}")
        assertSame(child, back, "getChild gave another instance")
        parent.queueFree()
    }

    @Test
    fun treeRootIsAWindowAndStaysTheSameInstance() {
        val root = tree.root
        assertSame(root, tree.root, "root gave two instances")
        assertEquals("Window", root::class.simpleName, "root is ${root::class.simpleName}")
    }

    // --- signals and callables ----------------------------------------------------------------------------------

    @Test
    fun childEnteredTreeDeliversTheSprite2DItself() {
        val parent = Node().also { host.addChild(node = it) }
        val child = Sprite2D()
        var received: Node? = null
        parent.childEnteredTree.connect { node -> received = node }.use {
            parent.addChild(node = child)
        }
        assertTrue(received is Sprite2D, "got ${received?.let { it::class.simpleName }}")
        assertSame(child, received, "the signal gave another instance")
        parent.queueFree()
    }

    @Test
    fun typedCallableReceivesTheConcreteClass() {
        val area = Area2D()
        var received: Node2D? = null
        val callable = Callable { node: Node2D -> received = node }
        callable.call(Variant.from(area)).close()
        assertSame(area, received, "received=$received")
        callable.close()
        area.queueFree()
    }

    // --- collections --------------------------------------------------------------------------------------------

    @Test
    fun godotArrayOfNodeReturnsTheInstancesThatWentIn() {
        val a = Sprite2D()
        val b = Area2D()
        GodotArray<Node>().use { list ->
            list.add(a)
            list.add(b)
            assertSame(a, list[0], "element 0 is another instance")
            assertSame(b, list[1], "element 1 is another instance")
            assertSame(list[0], list[0], "two reads of element 0 differ")
        }
        a.queueFree()
        b.queueFree()
    }

    // --- user classes -------------------------------------------------------------------------------------------

    /** Red on purpose until #212 is fixed (createInstanceFunc builds two instances for a @Godot parent). */
    @Test
    fun godotClassExtendingAnotherGodotClassReadsBackAsEitherAndNotAsASibling_212() {
        DowncastCounters.base = 0
        val derived = instantiate<DowncastDerived>()
        val plain = instantiate<DowncastBase>()
        assertEquals(2, DowncastCounters.base, "built ${DowncastCounters.base} instead of 2")
        Variant.from(derived).use { variant ->
            assertSame(derived, variant.getValue<DowncastBase>(), "Derived read as Base is another instance")
            assertSame(derived, variant.getValue<DowncastDerived>(), "Derived read as Derived")
            assertNull(variant.getValueOrNull<DowncastNode>(), "Derived read as an unrelated user class")
        }
        Variant.from(plain).use { variant ->
            assertSame(plain, variant.getValue<DowncastBase>(), "Base read as Base")
            assertNull(variant.getValueOrNull<DowncastDerived>(), "Base read as Derived")
        }
        assertEquals(2, DowncastCounters.base, "reading built more: ${DowncastCounters.base}")
        derived.queueFree()
        plain.queueFree()
    }

    /** #212 */
    @Test
    fun godotClassInheritsVirtualsMethodsPropertiesAndSignalsOfItsGodotParent_212() {
        DowncastCounters.baseEnteredTree = 0
        val derived = instantiate<DowncastDerived>()
        host.addChild(node = derived)
        assertEquals(
            1,
            DowncastCounters.baseEnteredTree,
            "Base._enter_tree ran ${DowncastCounters.baseEnteredTree} times for a Derived",
        )
        assertEquals("DowncastDerived", derived.getClass(), "get_class=${derived.getClass()}")
        "DowncastBase".toStringName().use { name ->
            assertTrue(derived.isClass(name), "is_class(DowncastBase) is false")
        }
        "baseGreeting".toStringName().use { name ->
            derived.call(name).use { assertEquals("7", it.toString(), "baseGreeting()=$it") }
        }
        "baseValue".toStringName().use { name ->
            derived.get(name).use { assertEquals("3", it.toString(), "baseValue=$it") }
        }
        "baseHint".toStringName().use { name ->
            assertTrue(derived.hasSignal(name), "has_signal(baseHint) is false")
        }
        derived.queueFree()
    }

    @Test
    fun godotNodeInstancedFromPackedSceneIsTheUserClassBuiltOncePerInstance() {
        DowncastCounters.node = 0
        val original = instantiate<DowncastNode>()
        val scene = PackedScene()
        assertEquals(GodotError.OK, scene.pack(original), "pack failed")
        val before = DowncastCounters.node
        val copy = scene.instantiate()
        assertTrue(copy is DowncastNode, "instantiate gave ${copy::class.simpleName}")
        assertEquals(before + 1, DowncastCounters.node, "instantiate built ${DowncastCounters.node - before}")
        assertNotSame(original, copy, "the copy is the original")
        Variant.from(copy).use { assertSame(copy, it.getValue<DowncastNode>(), "copy read back differs") }
        assertEquals(before + 1, DowncastCounters.node, "reading built more")
        original.queueFree()
        copy.queueFree()
    }

    @Test
    fun godotClassThroughAVariantIsBuiltOnceAndComesBackAsItself() {
        DowncastCounters.node = 0
        val mine = instantiate<DowncastNode>()
        assertEquals(1, DowncastCounters.node, "after instantiate: ${DowncastCounters.node}")
        Variant.from(mine).use { variant ->
            val back = variant.getValue<DowncastNode>()
            assertSame(mine, back, "another instance came back")
            assertSame(mine, variant.getValue<Node>(), "read as Node is another instance")
        }
        assertEquals(1, DowncastCounters.node, "boxing built more: ${DowncastCounters.node}")
        mine.queueFree()
    }

    @Test
    fun godotClassThroughASignalACallableAndAnArrayBuildsNothingNew() {
        DowncastCounters.node = 0
        val parent = Node().also { host.addChild(node = it) }
        val mine = instantiate<DowncastNode>()
        var viaSignal: Node? = null
        parent.childEnteredTree.connect { node -> viaSignal = node }.use { parent.addChild(node = mine) }
        assertSame(mine, viaSignal, "signal gave another instance")

        var viaCallable: DowncastNode? = null
        Callable { node: DowncastNode -> viaCallable = node }.use { it.call(Variant.from(mine)).close() }
        assertSame(mine, viaCallable, "Callable gave another instance")

        GodotArray<Node>().use { list ->
            list.add(mine)
            assertSame(mine, list[0], "array gave another instance")
        }
        assertSame(mine, parent.getChild(0), "getChild gave another instance")
        assertEquals(1, DowncastCounters.node, "built ${DowncastCounters.node} instances")
        parent.queueFree()
    }

    // --- references ---------------------------------------------------------------------------------------------

    @Test
    fun godotResourceThroughAVariantIsBuiltOnceAndKeepsItsCount() {
        DowncastCounters.resource = 0
        val mine = instantiate<DowncastResource>()
        val baseline = mine.getReferenceCount()
        repeat(3) {
            Variant.from(mine).use { variant -> assertSame(mine, variant.getValue<Resource>(), "other") }
        }
        assertEquals(1, DowncastCounters.resource, "built ${DowncastCounters.resource}")
        assertEquals(baseline, mine.getReferenceCount(), "refcount $baseline -> ${mine.getReferenceCount()}")
    }

    // --- class names --------------------------------------------------------------------------------------------

    /**
     * The resolver asks the engine for the class with `Object.get_class`. `object_get_class_name` is the function
     * meant for picking a wrapper (it walks to the nearest *exposed* class); this prints both so they can be compared.
     * The test only fails when `get_class_name` gives nothing. The loaded `probe_mesh.tres` of the live check is
     * replaced by an instantiated [DowncastProbeMesh] (the host has no assets), and the live scene by the runner node.
     */
    @Test
    fun classNames() {
        val samples: List<Pair<String, GodotObject>> = listOf(
            "Node" to Node(),
            "Sprite2D" to Sprite2D(),
            "Button" to Button(),
            "root" to tree.root,
            "runner" to host,
            "Resource" to instantiate<DowncastProbeMesh>(),
        )
        for ((label, obj) in samples) {
            val viaGetClass = obj.getClass()
            val viaGetClassName = StringName("").use { out ->
                val ok = ObjectBinding.getClassName(obj.rawPtr, BindingProcAddressHolder.library, out.rawPtr)
                assertTrue(ok, "$label: object_get_class_name failed")
                out.toString()
            }
            val verdict = if (viaGetClass == viaGetClassName) "same" else "DIFFERENT"
            println(
                "[DowncastTest] name $label wrapper=${obj::class.simpleName} get_class=$viaGetClass " +
                    "get_class_name=$viaGetClassName -> $verdict",
            )
            assertTrue(viaGetClassName.isNotEmpty(), "$label: empty class name")
        }
        // The samples that were built here (not the scene, the root or the loaded resource) are freed now.
        for ((label, obj) in samples) {
            if (label == "Node" || label == "Sprite2D" || label == "Button") ObjectBinding.destroyRaw(obj.rawPtr)
        }
    }
}

/** Stands for the `probe_mesh.tres` resource of the live check in [DowncastTest.classNames]. */
@Godot
class DowncastProbeMesh(nativePtr: COpaquePointer) : Mesh(nativePtr)
