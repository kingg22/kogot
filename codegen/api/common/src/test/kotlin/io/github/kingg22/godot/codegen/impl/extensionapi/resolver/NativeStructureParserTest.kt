package io.github.kingg22.godot.codegen.impl.extensionapi.resolver

import io.github.kingg22.godot.codegen.extensionapi.resolver.NativeStructureParser
import io.github.kingg22.godot.codegen.extensionapi.resolver.NativeStructureParser.NativeStructureField
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class NativeStructureParserTest {
    @Test
    fun `test all native structures from extension api`() {
        assertAll(
            testCases.map { (input, expected) ->
                Executable {
                    val result = NativeStructureParser.parseFormat(input)
                    assertEquals(expected, result, "Failed parsing format: $input")
                }
            },
        )
    }
}

@Suppress("ktlint:standard:max-line-length")
private val testCases = mapOf(
    "float left;float right" to listOf(
        NativeStructureField("left", "float"),
        NativeStructureField("right", "float"),
    ),
    "Rect2 leading_caret;Rect2 trailing_caret;TextServer::Direction leading_direction;TextServer::Direction trailing_direction" to
        listOf(
            NativeStructureField("leading_caret", "Rect2"),
            NativeStructureField("trailing_caret", "Rect2"),
            NativeStructureField("leading_direction", "enum::TextServer.Direction"),
            NativeStructureField("trailing_direction", "enum::TextServer.Direction"),
        ),
    "int start = -1;int end = -1;uint8_t count = 0;uint8_t repeat = 1;uint16_t flags = 0;float x_off = 0.f;float y_off = 0.f;float advance = 0.f;RID font_rid;int font_size = 0;int32_t index = 0" to
        listOf(
            NativeStructureField("start", "int", defaultValue = "-1"),
            NativeStructureField("end", "int", defaultValue = "-1"),
            NativeStructureField("count", "uint8_t", defaultValue = "0"),
            NativeStructureField("repeat", "uint8_t", defaultValue = "1"),
            NativeStructureField("flags", "uint16_t", defaultValue = "0"),
            NativeStructureField("x_off", "float", defaultValue = "0.f"),
            NativeStructureField("y_off", "float", defaultValue = "0.f"),
            NativeStructureField("advance", "float", defaultValue = "0.f"),
            NativeStructureField("font_rid", "RID"),
            NativeStructureField("font_size", "int", defaultValue = "0"),
            NativeStructureField("index", "int32_t", defaultValue = "0"),
        ),
    "uint64_t id = 0" to listOf(
        NativeStructureField("id", "uint64_t", defaultValue = "0"),
    ),
    "Vector2 travel;Vector2 remainder;Vector2 collision_point;Vector2 collision_normal;Vector2 collider_velocity;real_t collision_depth;real_t collision_safe_fraction;real_t collision_unsafe_fraction;int collision_local_shape;ObjectID collider_id;RID collider;int collider_shape" to
        listOf(
            NativeStructureField("travel", "Vector2"),
            NativeStructureField("remainder", "Vector2"),
            NativeStructureField("collision_point", "Vector2"),
            NativeStructureField("collision_normal", "Vector2"),
            NativeStructureField("collider_velocity", "Vector2"),
            NativeStructureField("collision_depth", "real_t"),
            NativeStructureField("collision_safe_fraction", "real_t"),
            NativeStructureField("collision_unsafe_fraction", "real_t"),
            NativeStructureField("collision_local_shape", "int"),
            NativeStructureField("collider_id", "ObjectID"),
            NativeStructureField("collider", "RID"),
            NativeStructureField("collider_shape", "int"),
        ),
    "Vector2 position;Vector2 normal;RID rid;ObjectID collider_id;Object *collider;int shape" to listOf(
        NativeStructureField("position", "Vector2"),
        NativeStructureField("normal", "Vector2"),
        NativeStructureField("rid", "RID"),
        NativeStructureField("collider_id", "ObjectID"),
        NativeStructureField("collider", "Object*"),
        NativeStructureField("shape", "int"),
    ),
    "Vector2 point;Vector2 normal;RID rid;ObjectID collider_id;int shape;Vector2 linear_velocity" to listOf(
        NativeStructureField("point", "Vector2"),
        NativeStructureField("normal", "Vector2"),
        NativeStructureField("rid", "RID"),
        NativeStructureField("collider_id", "ObjectID"),
        NativeStructureField("shape", "int"),
        NativeStructureField("linear_velocity", "Vector2"),
    ),
    "RID rid;ObjectID collider_id;Object *collider;int shape" to listOf(
        NativeStructureField("rid", "RID"),
        NativeStructureField("collider_id", "ObjectID"),
        NativeStructureField("collider", "Object*"),
        NativeStructureField("shape", "int"),
    ),
    "Vector3 position;Vector3 normal;Vector3 collider_velocity;Vector3 collider_angular_velocity;real_t depth;int local_shape;ObjectID collider_id;RID collider;int collider_shape" to
        listOf(
            NativeStructureField("position", "Vector3"),
            NativeStructureField("normal", "Vector3"),
            NativeStructureField("collider_velocity", "Vector3"),
            NativeStructureField("collider_angular_velocity", "Vector3"),
            NativeStructureField("depth", "real_t"),
            NativeStructureField("local_shape", "int"),
            NativeStructureField("collider_id", "ObjectID"),
            NativeStructureField("collider", "RID"),
            NativeStructureField("collider_shape", "int"),
        ),
    "Vector3 travel;Vector3 remainder;real_t collision_depth;real_t collision_safe_fraction;real_t collision_unsafe_fraction;PhysicsServer3DExtensionMotionCollision collisions[32];int collision_count" to
        listOf(
            NativeStructureField("travel", "Vector3"),
            NativeStructureField("remainder", "Vector3"),
            NativeStructureField("collision_depth", "real_t"),
            NativeStructureField("collision_safe_fraction", "real_t"),
            NativeStructureField("collision_unsafe_fraction", "real_t"),
            NativeStructureField("collisions", "PhysicsServer3DExtensionMotionCollision", arraySize = 32),
            NativeStructureField("collision_count", "int"),
        ),
    "Vector3 position;Vector3 normal;RID rid;ObjectID collider_id;Object *collider;int shape;int face_index" to
        listOf(
            NativeStructureField("position", "Vector3"),
            NativeStructureField("normal", "Vector3"),
            NativeStructureField("rid", "RID"),
            NativeStructureField("collider_id", "ObjectID"),
            NativeStructureField("collider", "Object*"),
            NativeStructureField("shape", "int"),
            NativeStructureField("face_index", "int"),
        ),
    "Vector3 point;Vector3 normal;RID rid;ObjectID collider_id;int shape;Vector3 linear_velocity" to listOf(
        NativeStructureField("point", "Vector3"),
        NativeStructureField("normal", "Vector3"),
        NativeStructureField("rid", "RID"),
        NativeStructureField("collider_id", "ObjectID"),
        NativeStructureField("shape", "int"),
        NativeStructureField("linear_velocity", "Vector3"),
    ),
    "RID rid;ObjectID collider_id;Object *collider;int shape" to listOf(
        NativeStructureField("rid", "RID"),
        NativeStructureField("collider_id", "ObjectID"),
        NativeStructureField("collider", "Object*"),
        NativeStructureField("shape", "int"),
    ),
    "StringName signature;uint64_t call_count;uint64_t total_time;uint64_t self_time" to listOf(
        NativeStructureField("signature", "StringName"),
        NativeStructureField("call_count", "uint64_t"),
        NativeStructureField("total_time", "uint64_t"),
        NativeStructureField("self_time", "uint64_t"),
    ),
)
