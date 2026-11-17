@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

package io.github.kingg22.godot.test.interop

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.GodotError
import io.github.kingg22.godot.api.builtin.toGodotString
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.SceneTree
import io.github.kingg22.godot.api.core.refcounted.GDScript
import io.github.kingg22.godot.api.singleton.Engine
import io.github.kingg22.godot.castTo
import io.github.kingg22.godot.test.assertKnownBug
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every value that crosses the Kotlin/GDScript boundary, checked from the GDScript side (ported from the former
 * `InteropCheck` scene). The GDScript source below is compiled at runtime and attached to a node in the live
 * SceneTree; each `t_*` function is one check and returns `""` when it passed, or the failure detail.
 *
 * Not ported (they need frames): the `await get_tree().process_frame` clean-up after the calls, and the check
 * "MultiplayerAPIExtension: the engine polls Kotlin every frame" (`pollCount() > 0` after one frame).
 */
class InteropTest {
    // region metadata (#246)

    /**
     * #246 is open: a Variant parameter is registered as OBJECT with the owner class, not as Variant
     * (NIL + NIL_IS_VARIANT). Passes while that holds; see [assertKnownBug].
     */
    @Test fun variantParameterIsNotRegisteredAsVariant_246() = assertKnownBug("#246") { gd("t_variant_param_metadata") }

    /**
     * #246 is open: an int parameter gets the owner class as `class_name` instead of none. Passes while that holds; see
     * [assertKnownBug].
     */
    @Test fun intParameterHasTheOwnerClassName_246() = assertKnownBug("#246") { gd("t_int_param_metadata") }

    // endregion

    // region primitives and builtin value types

    @Test fun intArgumentAndReturn() = gd("t_int")

    @Test fun int64Return() = gd("t_int64")

    @Test fun floatArgumentAndReturn() = gd("t_float")

    @Test fun boolArgumentAndReturn() = gd("t_bool")

    @Test fun stringWithNonAscii() = gd("t_string")

    @Test fun vector2ArgumentAndReturn() = gd("t_vector2")

    @Test fun vector3ArgumentsAndReturn() = gd("t_vector3")

    @Test fun colorThroughVariantParameterAndReturn() = gd("t_color")

    // endregion

    // region collections

    @Test fun untypedArrayArgument() = gd("t_array_untyped")

    @Test fun typedArrayArgument() = gd("t_array_typed")

    @Test fun arrayReturn() = gd("t_array_return")

    @Test fun arraySizeReturnedAfterKotlinAppends() = gd("t_array_size")

    @Test fun arrayIsPassedByReference() = gd("t_array_reference")

    @Test fun dictionaryArgument() = gd("t_dictionary_argument")

    @Test fun dictionaryReturn() = gd("t_dictionary_return")

    @Test fun packedInt32ArrayArgumentAndReturn() = gd("t_packed_int32")

    // endregion

    // region objects and Variant parameters

    @Test fun objectArgument() = gd("t_object_argument")

    @Test fun nullObjectArgument() = gd("t_object_null")

    @Test fun kotlinReceivesTheSameInstanceAsItsParent() = gd("t_identity_same")

    @Test fun anotherObjectIsNotTheParent() = gd("t_identity_other")

    @Test fun variantParameterInt() = gd("t_variant_int")

    @Test fun variantParameterString() = gd("t_variant_string")

    @Test fun variantParameterVector2() = gd("t_variant_vector2")

    @Test fun variantParameterArray() = gd("t_variant_array")

    @Test fun variantParameterNull() = gd("t_variant_null")

    // endregion

    // region Kotlin to GDScript, @Export

    @Test fun kotlinCallsAGdScriptMethodAndGetsItsReturn() = gd("t_call_parent")

    @Test fun kotlinSignalReachesAGdScriptHandler() = gd("t_signal")

    @Test fun exportIntDefault() = gd("t_export_default")

    @Test fun exportIntSetAndGet() = gd("t_export_int")

    @Test fun exportStringSetAndGet() = gd("t_export_string")

    @Test fun exportFloatSetAndGet() = gd("t_export_float")

    // endregion

    // region GDScript inheriting a Kotlin class (#245)

    /** #245: GDScript subclass of a Kotlin class calls the inherited Kotlin method (untyped on purpose). */
    @Test fun gdScriptSubclassCallsInheritedKotlinMethod() = gd("t_child_calls_kotlin")

    @Test fun gdScriptSubclassIsStillTheKotlinClass() = gd("t_child_is_class")

    // endregion

    // region ImageFormatLoaderExtension

    @Test fun imageFormatLoaderKotlinLoadsTheImage() = gd("t_image_loads")

    @Test fun imageFormatLoaderPixelsKotlinWrote() = gd("t_image_pixels")

    @Test fun imageFormatLoaderCalledOnce() = gd("t_image_called_once")

    // endregion

    // region MultiplayerAPIExtension

    @Test fun multiplayerGetUniqueId() = gd("t_mp_unique_id")

    @Test fun multiplayerGetPeers() = gd("t_mp_peers")

    @Test fun multiplayerGetRemoteSenderId() = gd("t_mp_sender_id")

    @Test fun multiplayerRpcReturnsTheErrorFromKotlin() = gd("t_mp_rpc_error_ok")

    @Test fun multiplayerKotlinReceivesTheCall() = gd("t_mp_rpc_received")

    @Test fun multiplayerKotlinForwardsTheArgumentsToGdScript() = gd("t_mp_rpc_forwards")

    @Test fun multiplayerErrorCodeFromKotlinReachesGdScript() = gd("t_mp_rpc_fail")

    // endregion

    // region StreamPeerExtension

    @Test fun streamPeerGetAvailableBytes() = gd("t_stream_available")

    /**
     * #249 is open: the raw-pointer `_get_data` virtual is never dispatched to Kotlin, so `getDataCount()` stays 0.
     * Passes while that holds; see [assertKnownBug].
     */
    @Test fun streamPeerGetDataDoesNotReachTheKotlinOverride_249() = assertKnownBug("#249") { gd("t_stream_get_data") }

    // endregion

    /**
     * Compiles [GDSCRIPT] as a real `GDScript`, attaches it to a fresh node of the live SceneTree (some checks need
     * `get_tree()`, a parent and `add_child`), runs [function] and asserts it returned `""`.
     */
    private fun gd(function: String) {
        val script = GDScript()
        script.sourceCode = GDSCRIPT.toGodotString()
        // Not an assertion: a script that does not compile must not be mistaken for a known bug (see assertKnownBug).
        check(script.reload() == GodotError.OK) { "the GDScript did not compile" }
        val node = Node()
        node.setScript(script)
        Engine.instance.getMainLoop().castTo<SceneTree>().root.addChild(node = node)
        val failure = function.toStringName().use { name -> node.call(name).use { it.toString() } }
        assertEquals("", failure, "$function failed")
    }

    private companion object {
        /** GDScript side of the checks, one `t_*` function per check. */
        const val GDSCRIPT = """
extends Node

var pings := []
var received := []


func ok(cond: bool, detail = null) -> String:
	return "" if cond else "FAIL: %s" % str(detail)


# Called by Kotlin (TestInteropTarget.callParent).
func gd_triple(value: int) -> int:
	return value * 3


# Reached through Node.rpc(), routed by the Kotlin MultiplayerAPIExtension.
func gd_receive(a, b, c) -> void:
	received = [a, b, c]


func _on_ping(value: int, text: String) -> void:
	pings.append([value, text])


# A GDScript class that extends a Kotlin class. Untyped on purpose: with typed arguments GDScript compiles the call to
# the Kotlin method as a ptrcall, which kogot does not register yet, and the engine crashes (#245).
class GdChild extends TestInteropTarget:
	func gd_sum_via_kotlin(a, b):
		return addInts(a, b) + 1


func method_info(cls: StringName, method: String) -> Dictionary:
	for m in ClassDB.class_get_method_list(cls, true):
		if m["name"] == method:
			return m
	return {}


# Built through ClassDB on purpose. With `TestInteropTarget.new()` (typed or not, GDScript infers it) the static checker
# refuses to parse every call that passes a non-object to a Variant parameter, because kogot registers those
# parameters as OBJECT of the class (#246).
func target():
	var t: Object = ClassDB.instantiate(&"TestInteropTarget")
	add_child(t)
	return t


func t_variant_param_metadata():
	var info := method_info("TestInteropTarget", "variantType")
	var arg: Dictionary = info["args"][0]
	return ok(arg["type"] == TYPE_NIL and arg["usage"] & PROPERTY_USAGE_NIL_IS_VARIANT != 0, arg)


func t_int_param_metadata():
	var int_arg: Dictionary = method_info("TestInteropTarget", "addInts")["args"][0]
	return ok(int_arg["type"] == TYPE_INT and int_arg["class_name"] == &"", int_arg)


func t_int():
	var sum = target().addInts(2, 3)
	return ok(sum == 5 and typeof(sum) == TYPE_INT, sum)


func t_int64():
	var big = target().bigNumber()
	return ok(big == 5_000_000_001 and typeof(big) == TYPE_INT, big)


func t_float():
	var half = target().half(5.0)
	return ok(is_equal_approx(half, 2.5) and typeof(half) == TYPE_FLOAT, half)


func t_bool():
	var t = target()
	return ok(t.negate(true) == false and t.negate(false) == true)


func t_string():
	var greeting = target().greet("Ñandú 🐧")
	return ok(greeting == "Hola, Ñandú 🐧, desde Kotlin" and typeof(greeting) == TYPE_STRING, greeting)


func t_vector2():
	var scaled = target().scale(Vector2(1.5, -2), 2.0)
	return ok(scaled == Vector2(3, -4), scaled)


func t_vector3():
	var crossed = target().cross(Vector3(1, 0, 0), Vector3(0, 1, 0))
	return ok(crossed == Vector3(0, 0, 1), crossed)


func t_color():
	var inverted = target().invert(Color(0.25, 0.5, 1.0, 0.75))
	return ok(inverted.is_equal_approx(Color(0.75, 0.5, 0.0, 0.75)), inverted)


func t_array_untyped():
	return ok(target().sumArray([1, 2, 3]) == 6)


func t_array_typed():
	var typed: Array[int] = [4, 5]
	return ok(target().sumTyped(typed) == 9)


func t_array_return():
	var made = target().makeArray(3)
	return ok(made == [0, 1, 2, "end"], made)


func t_array_size():
	var shared := [1, 2, 3]
	var new_size = target().appendTo(shared)
	return ok(new_size == 4, new_size)


func t_array_reference():
	var shared := [1, 2, 3]
	target().appendTo(shared)
	return ok(shared == [1, 2, 3, 99], shared)


func t_dictionary_argument():
	var t = target()
	return ok(t.describe({"b": 2, "a": 1}) == "a=1,b=2", t.describe({"b": 2, "a": 1}))


func t_dictionary_return():
	var dict = target().makeDictionary()
	return ok(dict == {"uno": 1, "dos": 2} and typeof(dict) == TYPE_DICTIONARY, dict)


func t_packed_int32():
	var doubled = target().doubled(PackedInt32Array([1, 2, 3]))
	return ok(doubled == PackedInt32Array([2, 4, 6]), doubled)


func t_object_argument():
	var t = target()
	return ok(t.nodeName(self) == String(name), t.nodeName(self))


func t_object_null():
	var t = target()
	return ok(t.nodeName(null) == "<null>", t.nodeName(null))


func t_identity_same():
	return ok(target().isParent(self) == true)


func t_identity_other():
	return ok(target().isParent(Resource.new()) == false)


func t_variant_int():
	var t = target()
	return ok(t.variantType(42) == "INT" and t.stringify(42) == "42", [t.variantType(42), t.stringify(42)])


func t_variant_string():
	var t = target()
	return ok(t.variantType("texto") == "STRING" and t.stringify("texto") == "texto", t.variantType("texto"))


func t_variant_vector2():
	var t = target()
	return ok(t.variantType(Vector2(1, 2)) == "VECTOR2" and t.stringify(Vector2(1, 2)) == str(Vector2(1, 2)), t.stringify(Vector2(1, 2)))


func t_variant_array():
	var t = target()
	return ok(t.variantType([1, "a"]) == "ARRAY", t.variantType([1, "a"]))


func t_variant_null():
	var t = target()
	return ok(t.variantType(null) == "NIL", t.variantType(null))


func t_call_parent():
	var t = target()
	return ok(t.callParent("gd_triple", 7) == 21, t.callParent("gd_triple", 7))


func t_signal():
	var t = target()
	t.pinged.connect(_on_ping)
	t.emitPing(5, "hola")
	return ok(pings == [[5, "hola"]], pings)


func t_export_default():
	var t = target()
	return ok(t.counter == 0, t.counter)


func t_export_int():
	var t = target()
	t.counter = 7
	return ok(t.counter == 7 and t.get("counter") == 7, t.counter)


func t_export_string():
	var t = target()
	t.label = "desde gd"
	return ok(t.label == "desde gd", t.label)


func t_export_float():
	var t = target()
	t.ratio = 0.25
	return ok(is_equal_approx(t.ratio, 0.25), t.ratio)


func t_child_calls_kotlin():
	var child = GdChild.new()
	add_child(child)
	return ok(child.gd_sum_via_kotlin(2, 3) == 6, child.gd_sum_via_kotlin(2, 3))


func t_child_is_class():
	var child = GdChild.new()
	add_child(child)
	return ok(child.is_class("TestInteropTarget") and child is TestInteropTarget)


# Writes a 3x2 .kimg (RGBA 10, 20, 30, 255), registers the Kotlin loader and loads it. Every Kotlin object is held in an
# untyped variable: a statically typed call to an @ExportMethod crashes the engine (no ptrcall registered, #245).
func load_kimg() -> Array:
	var loader = TestKotlinImageLoader.new()
	loader.add_format_loader()
	var path := "user://interop_probe.kimg"
	var file := FileAccess.open(path, FileAccess.WRITE)
	file.store_32(3)
	file.store_32(2)
	for byte in [10, 20, 30, 255]:
		file.store_8(byte)
	file.close()
	var image := Image.load_from_file(path)
	return [loader, image]


func t_image_loads():
	var loaded := load_kimg()
	var image = loaded[1]
	loaded[0].remove_format_loader()
	return ok(image != null and image.get_size() == Vector2i(3, 2), image)


func t_image_pixels():
	var loaded := load_kimg()
	var image = loaded[1]
	loaded[0].remove_format_loader()
	if image == null:
		return ok(false, "no image")
	return ok(image.get_pixel(2, 1).is_equal_approx(Color8(10, 20, 30, 255)), image.get_pixel(2, 1))


func t_image_called_once():
	var loaded := load_kimg()
	var loader = loaded[0]
	var count = loader.loadCount()
	loader.remove_format_loader()
	return ok(count == 1, count)


# Installs the Kotlin multiplayer in this node's SceneTree.
func install_mp():
	var mp = TestKotlinMultiplayer.new()
	get_tree().set_multiplayer(mp)
	return mp


func t_mp_unique_id():
	install_mp()
	return ok(multiplayer.get_unique_id() == 4242, multiplayer.get_unique_id())


func t_mp_peers():
	install_mp()
	return ok(multiplayer.get_peers() == PackedInt32Array([1, 7]), multiplayer.get_peers())


func t_mp_sender_id():
	install_mp()
	return ok(multiplayer.get_remote_sender_id() == 9, multiplayer.get_remote_sender_id())


func t_mp_rpc_error_ok():
	install_mp()
	var err := rpc("gd_receive", 3, "dato", Vector2(1, 2))
	return ok(err == OK, err)


func t_mp_rpc_received():
	var mp = install_mp()
	rpc("gd_receive", 3, "dato", Vector2(1, 2))
	return ok(mp.lastRpc() == "0:gd_receive:3", mp.lastRpc())


func t_mp_rpc_forwards():
	install_mp()
	rpc("gd_receive", 3, "dato", Vector2(1, 2))
	return ok(received == [3, "dato", Vector2(1, 2)], received)


func t_mp_rpc_fail():
	install_mp()
	return ok(rpc_id(7, "fail") == ERR_UNAVAILABLE)


func t_stream_available():
	var peer = TestKotlinStreamPeer.new()
	return ok(peer.get_available_bytes() == 5, peer.get_available_bytes())


func t_stream_get_data():
	var peer = TestKotlinStreamPeer.new()
	peer.get_data(2)
	return ok(peer.getDataCount() == 1, peer.getDataCount())
"""
    }
}
