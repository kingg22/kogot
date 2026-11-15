# GDScript side of interop_check.tscn: drives the Kotlin classes of InteropCheck.kt and checks, from GDScript, every
# value that crosses the boundary in both directions, plus three engine `*Extension` classes implemented in Kotlin.
extends Node

var passed := 0
var failed := 0
var pings := []
var received := []


func check(label: String, ok: bool, detail = null) -> void:
	if ok:
		passed += 1
		print("[InteropCheck] PASS %s" % label)
	else:
		failed += 1
		print("[InteropCheck] FAIL %s -> %s" % [label, detail])


# Called by Kotlin (InteropTarget.callParent).
func gd_triple(value: int) -> int:
	return value * 3


# Reached through Node.rpc(), routed by the Kotlin MultiplayerAPIExtension.
func gd_receive(a, b, c) -> void:
	received = [a, b, c]


func _on_ping(value: int, text: String) -> void:
	pings.append([value, text])


# A GDScript class that extends a Kotlin class. Untyped on purpose: with typed arguments GDScript compiles the call to
# the Kotlin method as a ptrcall, which kogot does not register yet, and the engine crashes (#245).
class GdChild extends InteropTarget:
	func gd_sum_via_kotlin(a, b):
		return addInts(a, b) + 1


func method_info(cls: StringName, method: String) -> Dictionary:
	for m in ClassDB.class_get_method_list(cls, true):
		if m["name"] == method:
			return m
	return {}


func _ready() -> void:
	await calls_into_kotlin()
	await extensions()
	print("[InteropCheck] DONE passed=%d failed=%d" % [passed, failed])
	get_tree().quit(0 if failed == 0 else 1)


func calls_into_kotlin() -> void:
	# Built through ClassDB on purpose. With `InteropTarget.new()` (typed or not, GDScript infers it) the static checker
	# refuses to parse every call that passes a non-object to a Variant parameter, because kogot registers those
	# parameters as OBJECT of class InteropTarget (#246, checked below).
	var t: Object = ClassDB.instantiate(&"InteropTarget")
	add_child(t)

	# Method metadata as Godot sees it (what the editor, the docs and GDScript's static checker read).
	var info := method_info("InteropTarget", "variantType")
	var arg: Dictionary = info["args"][0]
	check("a Variant parameter is registered as Variant (NIL + NIL_IS_VARIANT) (#246)", arg["type"] == TYPE_NIL and arg["usage"] & PROPERTY_USAGE_NIL_IS_VARIANT != 0, arg)
	var int_arg: Dictionary = method_info("InteropTarget", "addInts")["args"][0]
	check("an int parameter has no class_name (#246)", int_arg["type"] == TYPE_INT and int_arg["class_name"] == &"", int_arg)

	# Primitive types, with their Variant type preserved.
	var sum = t.addInts(2, 3)
	check("int argument and return", sum == 5 and typeof(sum) == TYPE_INT, sum)
	var big = t.bigNumber()
	check("64-bit int return", big == 5_000_000_001 and typeof(big) == TYPE_INT, big)
	var half = t.half(5.0)
	check("float argument and return", is_equal_approx(half, 2.5) and typeof(half) == TYPE_FLOAT, half)
	check("bool argument and return", t.negate(true) == false and t.negate(false) == true)
	var greeting = t.greet("Ñandú 🐧")
	check("String with non-ASCII text", greeting == "Hola, Ñandú 🐧, desde Kotlin" and typeof(greeting) == TYPE_STRING, greeting)

	# Builtin value types.
	var scaled = t.scale(Vector2(1.5, -2), 2.0)
	check("Vector2 argument and return", scaled == Vector2(3, -4), scaled)
	var crossed = t.cross(Vector3(1, 0, 0), Vector3(0, 1, 0))
	check("Vector3 arguments and return", crossed == Vector3(0, 0, 1), crossed)
	var inverted = t.invert(Color(0.25, 0.5, 1.0, 0.75))
	check("Color (through a Variant parameter) and Color return", inverted.is_equal_approx(Color(0.75, 0.5, 0.0, 0.75)), inverted)

	# Collections.
	check("untyped Array argument", t.sumArray([1, 2, 3]) == 6)
	var typed: Array[int] = [4, 5]
	check("typed Array[int] argument", t.sumTyped(typed) == 9)
	var made = t.makeArray(3)
	check("Array return", made == [0, 1, 2, "end"], made)
	var shared := [1, 2, 3]
	var new_size = t.appendTo(shared)
	check("Array size returned after Kotlin appends", new_size == 4, new_size)
	check("Array is passed by reference (Kotlin appends, GDScript sees it)", shared == [1, 2, 3, 99], shared)
	check("Dictionary argument", t.describe({"b": 2, "a": 1}) == "a=1,b=2", t.describe({"b": 2, "a": 1}))
	var dict = t.makeDictionary()
	check("Dictionary return", dict == {"uno": 1, "dos": 2} and typeof(dict) == TYPE_DICTIONARY, dict)
	var doubled = t.doubled(PackedInt32Array([1, 2, 3]))
	check("PackedInt32Array argument and return", doubled == PackedInt32Array([2, 4, 6]), doubled)

	# Objects, through Variant parameters and returns (an object-typed parameter is refused by the processor).
	check("Object argument (a Node)", t.nodeName(self) == String(name), t.nodeName(self))
	check("null Object argument", t.nodeName(null) == "<null>", t.nodeName(null))
	check("Object identity: Kotlin receives the same instance as its parent", t.isParent(self) == true)
	check("Object identity: another object is not the parent", t.isParent(Resource.new()) == false)

	# Variant parameters keep the type and value GDScript sent.
	check("Variant parameter: int", t.variantType(42) == "INT" and t.stringify(42) == "42", [t.variantType(42), t.stringify(42)])
	check("Variant parameter: String", t.variantType("texto") == "STRING" and t.stringify("texto") == "texto", t.variantType("texto"))
	check("Variant parameter: Vector2", t.variantType(Vector2(1, 2)) == "VECTOR2" and t.stringify(Vector2(1, 2)) == str(Vector2(1, 2)), t.stringify(Vector2(1, 2)))
	check("Variant parameter: Array", t.variantType([1, "a"]) == "ARRAY", t.variantType([1, "a"]))
	check("Variant parameter: null", t.variantType(null) == "NIL", t.variantType(null))

	# Kotlin -> GDScript.
	check("Kotlin calls a GDScript method and gets its return", t.callParent("gd_triple", 7) == 21, t.callParent("gd_triple", 7))
	t.pinged.connect(_on_ping)
	t.emitPing(5, "hola")
	check("Kotlin signal reaches a GDScript handler with its arguments", pings == [[5, "hola"]], pings)

	# @Export properties.
	check("@Export Int default", t.counter == 0, t.counter)
	t.counter = 7
	check("@Export Int set and get", t.counter == 7 and t.get("counter") == 7, t.counter)
	t.label = "desde gd"
	check("@Export String set and get", t.label == "desde gd", t.label)
	t.ratio = 0.25
	check("@Export Float set and get", is_equal_approx(t.ratio, 0.25), t.ratio)

	# GDScript inheriting a Kotlin class.
	var child = GdChild.new()
	add_child(child)
	check("GDScript subclass of a Kotlin class calls the inherited Kotlin method", child.gd_sum_via_kotlin(2, 3) == 6, child.gd_sum_via_kotlin(2, 3))
	check("GDScript subclass is still the Kotlin class", child.is_class("InteropTarget") and child is InteropTarget)
	child.queue_free()
	t.queue_free()
	await get_tree().process_frame


func extensions() -> void:
	# ImageFormatLoaderExtension: Image.load_from_file reads a .kimg file through Kotlin.
	# Every Kotlin object is held in an untyped variable: a statically typed call to an @ExportMethod crashes the engine
	# (no ptrcall registered, #245).
	var loader = KotlinImageLoader.new()
	loader.add_format_loader()
	var path := "user://interop_probe.kimg"
	var file := FileAccess.open(path, FileAccess.WRITE)
	file.store_32(3)
	file.store_32(2)
	for byte in [10, 20, 30, 255]:
		file.store_8(byte)
	file.close()
	var image := Image.load_from_file(path)
	check("ImageFormatLoaderExtension: Kotlin loads the image", image != null and image.get_size() == Vector2i(3, 2), image)
	if image != null:
		check("ImageFormatLoaderExtension: the pixels Kotlin wrote", image.get_pixel(2, 1).is_equal_approx(Color8(10, 20, 30, 255)), image.get_pixel(2, 1))
	check("ImageFormatLoaderExtension: called once", loader.loadCount() == 1, loader.loadCount())
	loader.remove_format_loader()

	# MultiplayerAPIExtension: multiplayer.* and Node.rpc() go through Kotlin.
	var mp = KotlinMultiplayer.new()
	get_tree().set_multiplayer(mp)
	check("MultiplayerAPIExtension: get_unique_id()", multiplayer.get_unique_id() == 4242, multiplayer.get_unique_id())
	check("MultiplayerAPIExtension: get_peers() returns a PackedInt32Array", multiplayer.get_peers() == PackedInt32Array([1, 7]), multiplayer.get_peers())
	check("MultiplayerAPIExtension: get_remote_sender_id()", multiplayer.get_remote_sender_id() == 9, multiplayer.get_remote_sender_id())
	var err := rpc("gd_receive", 3, "dato", Vector2(1, 2))
	check("MultiplayerAPIExtension: rpc() returns the Error from Kotlin", err == OK, err)
	check("MultiplayerAPIExtension: Kotlin receives the call", mp.lastRpc() == "0:gd_receive:3", mp.lastRpc())
	check("MultiplayerAPIExtension: Kotlin forwards the arguments to GDScript", received == [3, "dato", Vector2(1, 2)], received)
	check("MultiplayerAPIExtension: an error code from Kotlin reaches GDScript", rpc_id(7, "fail") == ERR_UNAVAILABLE)
	await get_tree().process_frame
	check("MultiplayerAPIExtension: the engine polls Kotlin every frame", mp.pollCount() > 0, mp.pollCount())
	get_tree().set_multiplayer(SceneMultiplayer.new())

	# StreamPeerExtension: _get_available_bytes is dispatched; the raw-pointer _get_data is not.
	var peer = KotlinStreamPeer.new()
	check("StreamPeerExtension: get_available_bytes()", peer.get_available_bytes() == 5, peer.get_available_bytes())
	peer.get_data(2)
	check("StreamPeerExtension: get_data() reaches the Kotlin _getData override (#249)", peer.getDataCount() == 1, peer.getDataCount())
