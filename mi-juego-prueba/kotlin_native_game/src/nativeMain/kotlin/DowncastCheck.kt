@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.annotations.Export
import io.github.kingg22.godot.api.annotations.ExportMethod
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.annotations.RegisterSignal
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.node.Area2D
import io.github.kingg22.godot.api.core.node.Button
import io.github.kingg22.godot.api.core.node.Node2D
import io.github.kingg22.godot.api.core.node.Sprite2D
import io.github.kingg22.godot.api.core.refcounted.PackedScene
import io.github.kingg22.godot.api.core.refcounted.Resource
import io.github.kingg22.godot.api.signal.connect
import io.github.kingg22.godot.api.signal.signal
import io.github.kingg22.godot.api.singleton.ResourceLoader
import io.github.kingg22.godot.binding.instantiate
import io.github.kingg22.godot.castTo
import io.github.kingg22.godot.castToOrNull
import io.github.kingg22.godot.internal.binding.BindingProcAddressHolder
import io.github.kingg22.godot.internal.binding.ObjectBinding
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

/** How many instances of each `@Godot` class of [DowncastCheck] were built: casting and boxing must never add one. */
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
 * inherits (a virtual override, a method, a property, a signal), so the checks see that Derived gets them all.
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
 * Live checks of issue #206 (`downcast_check.tscn`): an object read back from a `Variant`, a signal, a `Callable`, a
 * collection or an engine return is the one canonical wrapper of its most derived class, never a second instance.
 *
 * The `@Godot` classes above count their constructions in `init {}`, so "no new instance" is measured, not assumed.
 */
@Godot
class DowncastCheck(nativePtr: COpaquePointer) : Node(nativePtr) {
    private val checks = Checks("DowncastCheck")

    override fun _ready() {
        variants()
        engineReturns()
        signalsAndCallables()
        collections()
        userClasses()
        references()
        classNames()
        val code = checks.finish()
        // One frame later, so the queueFree() calls of the checks run before the engine reports leaks at exit.
        var done = false
        val _ = getTree().processFrame.connect {
            if (!done) {
                done = true
                getTree().quit(code)
            }
        }
    }

    private fun variants() = with(checks) {
        check("a Variant holding a Sprite2D gives that Sprite2D, the same wrapper every time") {
            val sprite = Sprite2D()
            Variant.from(sprite).use { variant ->
                val first = variant.getValue<Node2D>()
                expect(first is Sprite2D) { "got ${first::class.simpleName}" }
                expect(first === sprite) { "the constructed Sprite2D is not the canonical one" }
                expect(variant.getValue<Node2D>() === first) { "a second read gave another instance" }
                expect(variant.toObject() === first) { "toObject() gave another instance" }
            }
            sprite.queueFree()
        }
        check("getValueOrNull to an unrelated class is null and getValue throws ClassCastException") {
            val node = Node()
            Variant.from(node).use { variant ->
                expect(variant.getValueOrNull<Button>() == null) { "a Node read as Button" }
                expectThrows<ClassCastException>("getValue<Button>()") { variant.getValue<Button>() }
                expect(variant.getValueOrNull<Node>() === node) { "the Node did not come back" }
            }
            node.queueFree()
        }
        check("castTo and castToOrNull behave like as and as?") {
            val sprite = Sprite2D()
            val asNode: Node = sprite
            expect(asNode.castToOrNull<Sprite2D>() === sprite) { "castToOrNull<Sprite2D>" }
            expect(asNode.castToOrNull<Button>() == null) { "castToOrNull<Button> was not null" }
            expectThrows<ClassCastException>("castTo<Button>()") { asNode.castTo<Button>() }
            sprite.queueFree()
        }
    }

    private fun engineReturns() = with(checks) {
        check("getChild gives the Sprite2D that was added, not a bare Node") {
            val parent = Node()
            val child = Sprite2D()
            parent.addChild(node = child)
            val back = parent.getChild(0)
            expect(back is Sprite2D) { "got ${back::class.simpleName}" }
            expect(back === child) { "getChild gave another instance" }
            parent.queueFree()
        }
        check("getTree().root is a Window and stays the same instance") {
            val root = getTree().root
            expect(root === getTree().root) { "root gave two instances" }
            expect(root::class.simpleName == "Window") { "root is ${root::class.simpleName}" }
        }
    }

    private fun signalsAndCallables() = with(checks) {
        check("child_entered_tree delivers the Sprite2D itself") {
            val parent = Node().also { addChild(node = it) }
            val child = Sprite2D()
            var received: Node? = null
            parent.childEnteredTree.connect { node -> received = node }.use {
                parent.addChild(node = child)
            }
            expect(received is Sprite2D) { "got ${received?.let { it::class.simpleName }}" }
            expect(received === child) { "the signal gave another instance" }
            parent.queueFree()
        }
        check("a typed Callable receives the concrete class") {
            val area = Area2D()
            var received: Node2D? = null
            val callable = Callable { node: Node2D -> received = node }
            callable.call(Variant.from(area)).close()
            expect(received === area) { "received=$received" }
            callable.close()
            area.queueFree()
        }
    }

    private fun collections() = with(checks) {
        check("GodotArray<Node> returns the instances that went in") {
            val a = Sprite2D()
            val b = Area2D()
            GodotArray<Node>().use { list ->
                list.add(a)
                list.add(b)
                expect(list[0] === a) { "element 0 is another instance" }
                expect(list[1] === b) { "element 1 is another instance" }
                expect(list[0] === list[0]) { "two reads of element 0 differ" }
            }
            a.queueFree()
            b.queueFree()
        }
    }

    private fun userClasses() = with(checks) {
        // Red on purpose until #212 is fixed (createInstanceFunc builds two instances for a @Godot parent).
        check("a @Godot class that extends another @Godot class reads back as either, and not as a sibling (#212)") {
            DowncastCounters.base = 0
            val derived = instantiate<DowncastDerived>()
            val plain = instantiate<DowncastBase>()
            expect(DowncastCounters.base == 2) { "built ${DowncastCounters.base} instead of 2" }
            Variant.from(derived).use { variant ->
                expect(variant.getValue<DowncastBase>() === derived) { "Derived read as Base is another instance" }
                expect(variant.getValue<DowncastDerived>() === derived) { "Derived read as Derived" }
                expect(variant.getValueOrNull<DowncastNode>() == null) { "Derived read as an unrelated user class" }
            }
            Variant.from(plain).use { variant ->
                expect(variant.getValue<DowncastBase>() === plain) { "Base read as Base" }
                expect(variant.getValueOrNull<DowncastDerived>() == null) { "Base read as Derived" }
            }
            expect(DowncastCounters.base == 2) { "reading built more: ${DowncastCounters.base}" }
            derived.queueFree()
            plain.queueFree()
        }
        check("a @Godot class inherits the virtuals, methods, properties and signals of its @Godot parent (#212)") {
            DowncastCounters.baseEnteredTree = 0
            val derived = instantiate<DowncastDerived>()
            addChild(node = derived)
            expect(DowncastCounters.baseEnteredTree == 1) {
                "Base._enter_tree ran ${DowncastCounters.baseEnteredTree} times for a Derived"
            }
            expect(derived.getClass() == "DowncastDerived") { "get_class=${derived.getClass()}" }
            "DowncastBase".toStringName().use { name ->
                expect(derived.isClass(name)) { "is_class(DowncastBase) is false" }
            }
            "baseGreeting".toStringName().use { name ->
                derived.call(name).use { expect(it.toString() == "7") { "baseGreeting()=$it" } }
            }
            "baseValue".toStringName().use { name ->
                derived.get(name).use { expect(it.toString() == "3") { "baseValue=$it" } }
            }
            "baseHint".toStringName().use { name ->
                expect(derived.hasSignal(name)) { "has_signal(baseHint) is false" }
            }
            derived.queueFree()
        }
        check("a @Godot node instanced from a PackedScene is the user class, built once per instance") {
            DowncastCounters.node = 0
            val original = instantiate<DowncastNode>()
            val scene = PackedScene()
            expect(scene.pack(original) == GodotError.OK) { "pack failed" }
            val before = DowncastCounters.node
            val copy = scene.instantiate()
            expect(copy is DowncastNode) { "instantiate gave ${copy::class.simpleName}" }
            expect(DowncastCounters.node == before + 1) { "instantiate built ${DowncastCounters.node - before}" }
            expect(copy !== original) { "the copy is the original" }
            Variant.from(copy).use { expect(it.getValue<DowncastNode>() === copy) { "copy read back differs" } }
            expect(DowncastCounters.node == before + 1) { "reading built more" }
            original.queueFree()
            copy.queueFree()
        }
        check("a @Godot class through a Variant is built once and comes back as itself") {
            DowncastCounters.node = 0
            val mine = instantiate<DowncastNode>()
            expect(DowncastCounters.node == 1) { "after instantiate: ${DowncastCounters.node}" }
            Variant.from(mine).use { variant ->
                val back = variant.getValue<DowncastNode>()
                expect(back === mine) { "another instance came back" }
                expect(variant.getValue<Node>() === mine) { "read as Node is another instance" }
            }
            expect(DowncastCounters.node == 1) { "boxing built more: ${DowncastCounters.node}" }
            mine.queueFree()
        }
        check("a @Godot class through a signal, a Callable and an array builds nothing new") {
            DowncastCounters.node = 0
            val parent = Node().also { addChild(node = it) }
            val mine = instantiate<DowncastNode>()
            var viaSignal: Node? = null
            parent.childEnteredTree.connect { node -> viaSignal = node }.use { parent.addChild(node = mine) }
            expect(viaSignal === mine) { "signal gave another instance" }

            var viaCallable: DowncastNode? = null
            Callable { node: DowncastNode -> viaCallable = node }.use { it.call(Variant.from(mine)).close() }
            expect(viaCallable === mine) { "Callable gave another instance" }

            GodotArray<Node>().use { list ->
                list.add(mine)
                expect(list[0] === mine) { "array gave another instance" }
            }
            expect(parent.getChild(0) === mine) { "getChild gave another instance" }
            expect(DowncastCounters.node == 1) { "built ${DowncastCounters.node} instances" }
            parent.queueFree()
        }
    }

    private fun references() = with(checks) {
        check("reading a Resource again does not grow its reference count") {
            // An engine Resource (a texture); a `@Godot` one is checked below.
            val path = "res://icon.svg".toGodotString()
            val first = checkNotNull(ResourceLoader.instance.load(path)) { "icon.svg not found" }
            val baseline = first.getReferenceCount()
            repeat(5) {
                val again = ResourceLoader.instance.load(path)
                expect(again === first) { "load #$it gave another wrapper" }
            }
            val after = first.getReferenceCount()
            expect(after == baseline) { "refcount $baseline -> $after" }
        }
        check("a @Godot RefCounted returned by the engine is its canonical instance (#213)") {
            // Was a known limitation of #206: the canonical instance of a `@Godot` RefCounted owned no engine
            // reference, so an engine return of it got a separate adopting wrapper of the nearest engine class (Mesh).
            val path = "res://probe_mesh.tres".toGodotString()
            val first = checkNotNull(ResourceLoader.instance.load(path)) { "probe_mesh.tres not found" }
            expect(first is ProbeMesh) { "loaded as ${first::class.simpleName}" }
            val baseline = first.getReferenceCount()
            repeat(3) {
                val again = ResourceLoader.instance.load(path)
                expect(again === first) { "load #$it gave another wrapper" }
            }
            expect(first.getReferenceCount() == baseline) { "refcount $baseline -> ${first.getReferenceCount()}" }
        }
        check("a @Godot Resource through a Variant is built once and keeps its count") {
            DowncastCounters.resource = 0
            val mine = instantiate<DowncastResource>()
            val baseline = mine.getReferenceCount()
            repeat(3) {
                Variant.from(mine).use { variant -> expect(variant.getValue<Resource>() === mine) { "other" } }
            }
            expect(DowncastCounters.resource == 1) { "built ${DowncastCounters.resource}" }
            expect(mine.getReferenceCount() == baseline) { "refcount $baseline -> ${mine.getReferenceCount()}" }
        }
    }

    /**
     * The resolver asks the engine for the class with `Object.get_class`. `object_get_class_name` is the function
     * meant for picking a wrapper (it walks to the nearest *exposed* class); this prints both so they can be compared.
     * The check only fails when `get_class_name` gives nothing.
     */
    private fun classNames() = with(checks) {
        val samples: List<Pair<String, GodotObject>> = listOf(
            "Node" to Node(),
            "Sprite2D" to Sprite2D(),
            "Button" to Button(),
            "root" to getTree().root,
            "currentScene" to getTree().currentScene,
            "DowncastCheck" to this@DowncastCheck,
            "Resource" to checkNotNull(ResourceLoader.instance.load("res://probe_mesh.tres".toGodotString())),
        )
        for ((label, obj) in samples) {
            check("class name of $label") {
                val viaGetClass = obj.getClass()
                val viaGetClassName = StringName("").use { out ->
                    val ok = ObjectBinding.getClassName(obj.rawPtr, BindingProcAddressHolder.library, out.rawPtr)
                    expect(ok) { "object_get_class_name failed" }
                    out.toString()
                }
                val verdict = if (viaGetClass == viaGetClassName) "same" else "DIFFERENT"
                println(
                    "[DowncastCheck] name $label wrapper=${obj::class.simpleName} get_class=$viaGetClass " +
                        "get_class_name=$viaGetClassName -> $verdict",
                )
                expect(viaGetClassName.isNotEmpty()) { "empty class name" }
            }
        }
        // The samples that were built here (not the scene, the root or the loaded resource) are freed now.
        for ((label, obj) in samples) {
            if (label == "Node" || label == "Sprite2D" || label == "Button") ObjectBinding.destroyRaw(obj.rawPtr)
        }
    }
}
