@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class, ExperimentalGodotApi::class)

import io.github.kingg22.godot.api.EnumMask
import io.github.kingg22.godot.api.ExperimentalGodotApi
import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.GodotNotification
import io.github.kingg22.godot.api.annotations.Export
import io.github.kingg22.godot.api.annotations.ExportMethod
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.annotations.RegisterSignal
import io.github.kingg22.godot.api.builtin.Color
import io.github.kingg22.godot.api.builtin.Dictionary
import io.github.kingg22.godot.api.builtin.GodotArray
import io.github.kingg22.godot.api.builtin.PackedByteArray
import io.github.kingg22.godot.api.builtin.PackedInt32Array
import io.github.kingg22.godot.api.builtin.PackedStringArray
import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.VariantArray
import io.github.kingg22.godot.api.builtin.VariantDictionary
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.builtin.Vector3
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.builtin.getValue
import io.github.kingg22.godot.api.builtin.toGodotString
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.refcounted.FileAccess
import io.github.kingg22.godot.api.core.refcounted.Image
import io.github.kingg22.godot.api.core.refcounted.ImageFormatLoader
import io.github.kingg22.godot.api.core.refcounted.ImageFormatLoaderExtension
import io.github.kingg22.godot.api.core.refcounted.MultiplayerAPIExtension
import io.github.kingg22.godot.api.core.refcounted.MultiplayerPeer
import io.github.kingg22.godot.api.core.refcounted.StreamPeerExtension
import io.github.kingg22.godot.api.signal.emit
import io.github.kingg22.godot.api.signal.signal
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UByteVar

/*
 * Kotlin side of `interop_check.tscn`. The scene is driven by GDScript (`interop_check.gd`): it calls into these
 * classes and checks every value that comes back, so each crossing is seen from the GDScript side.
 */

/**
 * Calls in both directions: GDScript → Kotlin through `@ExportMethod`, `@Export` and a `@RegisterSignal` signal emitted
 * from Kotlin to a GDScript handler; Kotlin → GDScript through `Object.call` on a GDScript object.
 *
 * Parameters only use the types the processor reads correctly today (`Int`, `Float`, `Boolean`, `String`, `Vector2`,
 * `Vector3`) and `Variant`, which carries everything else (collections, colours, objects). A `Long`, `Double`, `Color`,
 * collection or packed-array parameter generates a binding that does not compile, and so does a `Variant` return (#247);
 * an object parameter or return and a `Unit` return are refused (#248). Other return types work.
 */
@Godot
class InteropTarget(nativePtr: COpaquePointer) :
    Node(nativePtr),
    GodotNotification {
    @Export var counter: Int = 0

    @Export var label: String = "kotlin"

    @Export var ratio: Float = 0.5f

    @RegisterSignal
    val pinged by signal<(value: Long, text: String) -> Unit>()

    override fun _notification(what: Int) {
        if (what == NOTIFICATION_PREDELETE.toInt()) pinged.close()
    }

    @ExportMethod
    fun addInts(a: Int, b: Int): Int = a + b

    @ExportMethod
    fun bigNumber(): Long = 5_000_000_001L

    @ExportMethod
    fun half(value: Float): Double = value / 2.0

    @ExportMethod
    fun negate(value: Boolean): Boolean = !value

    @ExportMethod
    fun greet(name: String): String = "Hola, $name, desde Kotlin"

    @ExportMethod
    fun scale(vector: Vector2, factor: Float): Vector2 = Vector2(
        vector.x * factor.toDouble(),
        vector.y * factor.toDouble(),
    )

    @ExportMethod
    fun cross(a: Vector3, b: Vector3): Vector3 = a.cross(b)

    @ExportMethod
    fun invert(color: Variant): Color {
        val value = color.getValue<Color>()
        return Color(1.0 - value.r, 1.0 - value.g, 1.0 - value.b, value.a.toDouble())
    }

    @ExportMethod
    fun sumArray(values: Variant): Long =
        values.getValue<VariantArray>().use { array -> array.sumOf { it.getValue<Long>() } }

    @ExportMethod
    fun sumTyped(values: Variant): Long = values.getValue<GodotArray<Long>>().use { it.sum() }

    @ExportMethod
    fun makeArray(count: Int): VariantArray {
        val array = VariantArray()
        for (i in 0 until count) array.append(i.toLong())
        array.append("end")
        return array
    }

    /** Godot arrays are references: the caller must see the element appended here. Returns the new size. */
    @ExportMethod
    fun appendTo(values: Variant): Long = values.getValue<VariantArray>().use { array ->
        array.append(99L)
        array.size.toLong()
    }

    @ExportMethod
    fun describe(map: Variant): String = map.getValue<VariantDictionary>().use { dictionary ->
        dictionary.entries
            .map { (key, value) -> key.toString() to value.toString() }
            .sortedBy { it.first }
            .joinToString(",") { "${it.first}=${it.second}" }
    }

    @ExportMethod
    fun makeDictionary(): Dictionary<String, Long> = Dictionary<String, Long>().apply {
        put("uno", 1L)
        put("dos", 2L)
    }

    @ExportMethod
    fun doubled(values: Variant): PackedInt32Array = values.getValue<PackedInt32Array>().use { source ->
        val result = PackedInt32Array()
        for (i in 0 until source.size()) {
            val _ = result.append(source.get(i) * 2)
        }
        result
    }

    /** An object through a `Variant` parameter: the name of the node, or `<null>`. */
    @ExportMethod
    fun nodeName(node: Variant): String = (node.toObjectOrNull() as? Node)?.name?.toString() ?: "<null>"

    /**
     * Object identity across the boundary, seen from Kotlin: the object GDScript passed is the very same Kotlin
     * instance as the parent node. (A `Variant` return, the only way to return an object, does not compile yet: #247.)
     */
    @ExportMethod
    fun isParent(value: Variant): Boolean = value.toObjectOrNull() === getParent()

    /** A `Variant` parameter keeps the Variant type GDScript sent: `"INT"`, `"STRING"`, `"NIL"`, ... */
    @ExportMethod
    fun variantType(value: Variant): String = value.getType().name

    /** A `Variant` parameter keeps the value GDScript sent. */
    @ExportMethod
    fun stringify(value: Variant): String = value.toString()

    /** Kotlin → GDScript: calls [method] on the parent (the GDScript node of the scene) and returns what it returned. */
    @ExportMethod
    fun callParent(method: String, value: Int): Long = method.toStringName().use { name ->
        Variant.from(value.toLong()).use { argument -> getParent().call(name, argument).use { it.getValue<Long>() } }
    }

    /** Kotlin → GDScript through a signal: the GDScript handler receives both arguments. */
    @ExportMethod
    fun emitPing(value: Int, text: String): Boolean {
        val _ = pinged.emit(value.toLong(), text)
        return true
    }
}

/**
 * An `ImageFormatLoaderExtension`: after `add_format_loader()`, `Image.load_from_file("*.kimg")` reads the file through
 * Kotlin. Format: width and height as 32-bit integers, then one RGBA8 colour that fills the image.
 */
@Godot
class KotlinImageLoader(nativePtr: COpaquePointer) : ImageFormatLoaderExtension(nativePtr) {
    private var loads = 0

    override fun _getRecognizedExtensions(): PackedStringArray =
        PackedStringArray().apply { "kimg".toGodotString().use { val _ = append(it) } }

    override fun _loadImage(
        image: Image?,
        fileaccess: FileAccess?,
        flags: EnumMask<ImageFormatLoader.LoaderFlags>,
        scale: Float,
    ): GodotError {
        if (image == null || fileaccess == null) return GodotError.UNAVAILABLE
        val width = fileaccess.get32().toInt()
        val height = fileaccess.get32().toInt()
        if (width <= 0 || height <= 0) return GodotError.FILE_CORRUPT
        val rgba = List(4) { fileaccess.get8().toLong() }
        PackedByteArray().use { data ->
            repeat(width * height) { rgba.forEach { val _ = data.append(it) } }
            image.setData(width, height, false, Image.Format.RGBA8, data)
        }
        loads++
        return GodotError.OK
    }

    @ExportMethod
    fun loadCount(): Int = loads
}

/**
 * A `MultiplayerAPIExtension`: once set with `SceneTree.set_multiplayer()`, every `multiplayer.*` call and every
 * `Node.rpc()` of that tree goes through Kotlin. `_rpc` records the call and forwards it to the target object
 * (Kotlin → GDScript with the original arguments), and returns `ERR_UNAVAILABLE` for a method named `fail`.
 */
@Godot
class KotlinMultiplayer(nativePtr: COpaquePointer) : MultiplayerAPIExtension(nativePtr) {
    private var lastRpc = ""
    private var polls = 0

    override fun _poll(): GodotError {
        polls++
        return GodotError.OK
    }

    override fun _setMultiplayerPeer(multiplayerPeer: MultiplayerPeer?) {}

    override fun _getMultiplayerPeer(): MultiplayerPeer? = null

    override fun _getUniqueId(): Int = 4242

    override fun _getPeerIds(): PackedInt32Array = PackedInt32Array().apply {
        val _ = append(1L)
        val _ = append(7L)
    }

    override fun _getRemoteSenderId(): Int = 9

    override fun _rpc(peer: Int, `object`: GodotObject?, method: StringName, args: VariantArray): GodotError {
        val name = method.toString()
        lastRpc = "$peer:$name:${args.size}"
        if (name == "fail") return GodotError.UNAVAILABLE
        `object`?.callv(method, args)?.close()
        return GodotError.OK
    }

    override fun _objectConfigurationAdd(`object`: GodotObject?, configuration: Variant): GodotError = GodotError.OK

    override fun _objectConfigurationRemove(`object`: GodotObject?, configuration: Variant): GodotError = GodotError.OK

    @ExportMethod
    fun lastRpc(): String = lastRpc

    @ExportMethod
    fun pollCount(): Int = polls
}

/**
 * A `StreamPeerExtension`. `_get_available_bytes` is dispatched to Kotlin. The data virtuals take raw pointers
 * (`uint8_t*`) and are not dispatched (#249): `_getData` compiles as an override, but Godot never calls it.
 */
@Godot
class KotlinStreamPeer(nativePtr: COpaquePointer) : StreamPeerExtension(nativePtr) {
    private var getDataCalls = 0

    override fun _getAvailableBytes(): Int = 5

    override fun _getData(rBuffer: CPointer<UByteVar>, rBytes: Int, rReceived: CPointer<IntVar>): GodotError {
        getDataCalls++
        return GodotError.OK
    }

    @ExportMethod
    fun getDataCount(): Int = getDataCalls
}
