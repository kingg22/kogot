@file:OptIn(ExperimentalForeignApi::class)

import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.core.node.Sprite2D
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * PROTOTYPE of a plain two-float vector: the value lives in a `Long`, so creating one, adding or scaling it allocates
 * nothing (no native heap, no `Cleaner`, no GC work). The math is Kotlin; the engine only sees a [Vector2] at the
 * boundary, filled with [writeTo] into a reusable instance.
 */
value class V2 private constructor(private val bits: Long) {
    constructor(x: Float, y: Float) : this((x.toRawBits().toLong() shl 32) or (y.toRawBits().toLong() and MASK))

    val x: Float get() = Float.fromBits((bits ushr 32).toInt())
    val y: Float get() = Float.fromBits(bits.toInt())

    operator fun plus(other: V2): V2 = V2(x + other.x, y + other.y)
    operator fun minus(other: V2): V2 = V2(x - other.x, y - other.y)
    operator fun times(scalar: Float): V2 = V2(x * scalar, y * scalar)
    operator fun div(scalar: Float): V2 = V2(x / scalar, y / scalar)

    /** Copies this vector into an existing engine [Vector2] (two float stores, no allocation). */
    fun writeTo(target: Vector2) {
        target.x = x
        target.y = y
    }

    /** A new engine [Vector2]: this one does allocate, use it only where a one-off value is needed. */
    fun toEngine(): Vector2 = Vector2(x.toDouble(), y.toDouble())

    companion object {
        private const val MASK = 0xFFFFFFFFL
        val ZERO = V2(0f, 0f)

        fun from(vector: Vector2): V2 = V2(vector.x, vector.y)
    }
}

@Godot class Sprite(nativePtr: COpaquePointer) : Sprite2D(nativePtr) {
    private var angle: Double = Random.nextDouble(0.0, PI * 2)
    private val speed: Double = Random.nextDouble(100.0, 600.0)
    var pos: V2 = V2.ZERO
    var windowSize: V2 = V2.ZERO
    var halfSize: V2 = V2.ZERO

    /** Reused every frame to hand [pos] to the engine, so `_process` allocates no [Vector2]. */
    private val enginePosition = Vector2()

    override fun _process(delta: Double) {
        // Prefers kotlin math functions instead of GD utils
        pos += V2(cos(angle).toFloat(), sin(angle).toFloat()) * (speed * delta).toFloat()
        pos.writeTo(enginePosition)
        position = enginePosition

        if (pos.x < halfSize.x || pos.x > windowSize.x - halfSize.x) {
            angle = PI - angle
        }
        if (pos.y < halfSize.y || pos.y > windowSize.y - halfSize.y) {
            angle = -angle
        }
    }
}
