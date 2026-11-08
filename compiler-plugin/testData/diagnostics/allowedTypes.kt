// RUN_PIPELINE_TILL: CODEGEN
// Black box: every Variant-compatible type, non-null and nullable, in every checked position. No errors expected.

import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject

fun valueArguments(
    boolean: Boolean,
    nullableBoolean: Boolean?,
    long: Long,
    nullableLong: Long?,
    int: Int,
    nullableInt: Int?,
    short: Short,
    nullableShort: Short?,
    double: Double,
    nullableDouble: Double?,
    float: Float,
    nullableFloat: Float?,
    string: String,
    nullableString: String?,
    unit: Unit,
    nullableUnit: Unit?,
    variant: Variant,
    nullableVariant: Variant?,
    godotString: GodotString,
    nullableGodotString: GodotString?,
    stringName: StringName,
    nullableStringName: StringName?,
    nodePath: NodePath,
    nullableNodePath: NodePath?,
    vector2: Vector2,
    nullableVector2: Vector2?,
    vector2i: Vector2i,
    nullableVector2i: Vector2i?,
    rect2: Rect2,
    nullableRect2: Rect2?,
    rect2i: Rect2i,
    nullableRect2i: Rect2i?,
    vector3: Vector3,
    nullableVector3: Vector3?,
    vector3i: Vector3i,
    nullableVector3i: Vector3i?,
    vector4: Vector4,
    nullableVector4: Vector4?,
    vector4i: Vector4i,
    nullableVector4i: Vector4i?,
    transform2D: Transform2D,
    nullableTransform2D: Transform2D?,
    transform3D: Transform3D,
    nullableTransform3D: Transform3D?,
    plane: Plane,
    nullablePlane: Plane?,
    quaternion: Quaternion,
    nullableQuaternion: Quaternion?,
    aabb: Aabb,
    nullableAabb: Aabb?,
    basis: Basis,
    nullableBasis: Basis?,
    projection: Projection,
    nullableProjection: Projection?,
    color: Color,
    nullableColor: Color?,
    rid: Rid,
    nullableRid: Rid?,
    callable: Callable,
    nullableCallable: Callable?,
    signal: Signal,
    nullableSignal: Signal?,
    dictionary: Dictionary<String, Int>,
    nullableDictionary: Dictionary<String, Int>?,
    godotArray: GodotArray<Int>,
    nullableGodotArray: GodotArray<Int>?,
    packedByteArray: PackedByteArray,
    nullablePackedByteArray: PackedByteArray?,
    packedInt32Array: PackedInt32Array,
    nullablePackedInt32Array: PackedInt32Array?,
    packedInt64Array: PackedInt64Array,
    nullablePackedInt64Array: PackedInt64Array?,
    packedFloat32Array: PackedFloat32Array,
    nullablePackedFloat32Array: PackedFloat32Array?,
    packedFloat64Array: PackedFloat64Array,
    nullablePackedFloat64Array: PackedFloat64Array?,
    packedStringArray: PackedStringArray,
    nullablePackedStringArray: PackedStringArray?,
    packedVector2Array: PackedVector2Array,
    nullablePackedVector2Array: PackedVector2Array?,
    packedVector3Array: PackedVector3Array,
    nullablePackedVector3Array: PackedVector3Array?,
    packedVector4Array: PackedVector4Array,
    nullablePackedVector4Array: PackedVector4Array?,
    packedColorArray: PackedColorArray,
    nullablePackedColorArray: PackedColorArray?,
    godotObject: GodotObject,
    nullableGodotObject: GodotObject?,
    aABB: AABB,
    nullableAABB: AABB?,
    rID: RID,
    nullableRID: RID?,
) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(boolean)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableBoolean)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(long)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableLong)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(int)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableInt)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(short)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableShort)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(double)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableDouble)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(float)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableFloat)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(string)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableString)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(unit)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableUnit)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(variant)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableVariant)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(godotString)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableGodotString)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(stringName)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableStringName)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nodePath)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableNodePath)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(vector2)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableVector2)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(vector2i)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableVector2i)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(rect2)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableRect2)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(rect2i)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableRect2i)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(vector3)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableVector3)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(vector3i)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableVector3i)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(vector4)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableVector4)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(vector4i)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableVector4i)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(transform2D)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableTransform2D)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(transform3D)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableTransform3D)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(plane)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullablePlane)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(quaternion)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableQuaternion)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(aabb)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableAabb)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(basis)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableBasis)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(projection)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableProjection)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(color)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableColor)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(rid)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableRid)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(callable)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableCallable)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(signal)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableSignal)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(dictionary)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableDictionary)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(godotArray)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableGodotArray)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(packedByteArray)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullablePackedByteArray)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(packedInt32Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullablePackedInt32Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(packedInt64Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullablePackedInt64Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(packedFloat32Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullablePackedFloat32Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(packedFloat64Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullablePackedFloat64Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(packedStringArray)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullablePackedStringArray)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(packedVector2Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullablePackedVector2Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(packedVector3Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullablePackedVector3Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(packedVector4Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullablePackedVector4Array)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(packedColorArray)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullablePackedColorArray)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(godotObject)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableGodotObject)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(aABB)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableAABB)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(rID)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableRID)
}

fun explicitTypeArguments() {
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Boolean>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Boolean?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Long>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Long?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Int>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Int?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Short>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Short?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Double>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Double?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Float>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Float?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><String>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><String?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Unit>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Unit?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Variant>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Variant?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><GodotString>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><GodotString?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><StringName>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><StringName?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><NodePath>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><NodePath?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector2>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector2?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector2i>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector2i?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Rect2>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Rect2?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Rect2i>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Rect2i?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector3>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector3?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector3i>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector3i?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector4>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector4?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector4i>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Vector4i?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Transform2D>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Transform2D?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Transform3D>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Transform3D?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Plane>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Plane?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Quaternion>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Quaternion?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Aabb>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Aabb?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Basis>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Basis?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Projection>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Projection?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Color>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Color?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Rid>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Rid?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Callable>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Callable?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Signal>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Signal?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Dictionary<String, Int>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Dictionary<String, Int>?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><GodotArray<Int>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><GodotArray<Int>?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedByteArray>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedByteArray?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedInt32Array>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedInt32Array?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedInt64Array>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedInt64Array?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedFloat32Array>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedFloat32Array?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedFloat64Array>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedFloat64Array?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedStringArray>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedStringArray?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedVector2Array>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedVector2Array?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedVector3Array>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedVector3Array?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedVector4Array>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedVector4Array?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedColorArray>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><PackedColorArray?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><GodotObject>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><GodotObject?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><AABB>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><AABB?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><RID>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><RID?>()
}

fun callableParameters() {
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { boolean: Boolean -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { long: Long -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { int: Int -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { short: Short -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { double: Double -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { float: Float -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { string: String -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { unit: Unit -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { variant: Variant -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { godotString: GodotString -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { stringName: StringName -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { nodePath: NodePath -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector2: Vector2 -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector2i: Vector2i -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { rect2: Rect2 -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { rect2i: Rect2i -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector3: Vector3 -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector3i: Vector3i -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector4: Vector4 -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector4i: Vector4i -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { transform2D: Transform2D -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { transform3D: Transform3D -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { plane: Plane -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { quaternion: Quaternion -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { aabb: Aabb -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { basis: Basis -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { projection: Projection -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { color: Color -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { rid: Rid -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { callable: Callable -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { signal: Signal -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { dictionary: Dictionary<String, Int> -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { godotArray: GodotArray<Int> -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedByteArray: PackedByteArray -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedInt32Array: PackedInt32Array -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedInt64Array: PackedInt64Array -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedFloat32Array: PackedFloat32Array -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedFloat64Array: PackedFloat64Array -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedStringArray: PackedStringArray -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedVector2Array: PackedVector2Array -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedVector3Array: PackedVector3Array -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedVector4Array: PackedVector4Array -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedColorArray: PackedColorArray -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { godotObject: GodotObject -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { aABB: AABB -> }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { rID: RID -> }
}

fun callableResults(
    boolean: Boolean,
    long: Long,
    int: Int,
    short: Short,
    double: Double,
    float: Float,
    string: String,
    unit: Unit,
    variant: Variant,
    godotString: GodotString,
    stringName: StringName,
    nodePath: NodePath,
    vector2: Vector2,
    vector2i: Vector2i,
    rect2: Rect2,
    rect2i: Rect2i,
    vector3: Vector3,
    vector3i: Vector3i,
    vector4: Vector4,
    vector4i: Vector4i,
    transform2D: Transform2D,
    transform3D: Transform3D,
    plane: Plane,
    quaternion: Quaternion,
    aabb: Aabb,
    basis: Basis,
    projection: Projection,
    color: Color,
    rid: Rid,
    callable: Callable,
    signal: Signal,
    dictionary: Dictionary<String, Int>,
    godotArray: GodotArray<Int>,
    packedByteArray: PackedByteArray,
    packedInt32Array: PackedInt32Array,
    packedInt64Array: PackedInt64Array,
    packedFloat32Array: PackedFloat32Array,
    packedFloat64Array: PackedFloat64Array,
    packedStringArray: PackedStringArray,
    packedVector2Array: PackedVector2Array,
    packedVector3Array: PackedVector3Array,
    packedVector4Array: PackedVector4Array,
    packedColorArray: PackedColorArray,
    godotObject: GodotObject,
    aABB: AABB,
    rID: RID,
) {
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { boolean }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> boolean }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { long }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> long }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { int }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> int }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { short }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> short }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { double }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> double }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { float }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> float }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { string }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> string }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { unit }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> unit }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { variant }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> variant }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { godotString }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> godotString }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { stringName }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> stringName }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { nodePath }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> nodePath }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector2 }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> vector2 }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector2i }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> vector2i }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { rect2 }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> rect2 }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { rect2i }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> rect2i }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector3 }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> vector3 }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector3i }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> vector3i }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector4 }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> vector4 }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { vector4i }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> vector4i }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { transform2D }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> transform2D }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { transform3D }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> transform3D }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { plane }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> plane }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { quaternion }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> quaternion }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { aabb }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> aabb }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { basis }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> basis }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { projection }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> projection }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { color }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> color }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { rid }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> rid }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { callable }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> callable }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { signal }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> signal }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { dictionary }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> dictionary }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { godotArray }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> godotArray }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedByteArray }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> packedByteArray }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedInt32Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> packedInt32Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedInt64Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> packedInt64Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedFloat32Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> packedFloat32Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedFloat64Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> packedFloat64Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedStringArray }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> packedStringArray }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedVector2Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> packedVector2Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedVector3Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> packedVector3Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedVector4Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> packedVector4Array }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { packedColorArray }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> packedColorArray }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { godotObject }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> godotObject }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { aABB }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> aABB }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { rID }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> rID }
}
