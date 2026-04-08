import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.core.node.Sprite2D
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.random.nextInt

// TODO create a factory function named "Spite" without params to allow build it without nativePtr
@Godot
class Sprite
@OptIn(ExperimentalForeignApi::class)
constructor(nativePtr: COpaquePointer) : Sprite2D(nativePtr) {
    private var angle: Double = Random.nextDouble(0.0, PI * 2)
    private var speed: Int = Random.nextInt(100..600)
    var pos: Vector2 = Vector2.ZERO
    var windowSize: Vector2 = Vector2.ZERO
    var halfSize: Vector2 = Vector2.ZERO

    override fun _process(delta: Double) {
        // Prefers kotlin math functions instead of GD utils
        pos += Vector2(x = cos(angle), y = sin(angle)) * speed.toDouble() * delta
        position = pos

        if (pos.x < halfSize.x || pos.x > windowSize.x - halfSize.x) {
            angle = PI - angle
        }
        if (pos.y < halfSize.y || pos.y > windowSize.y - halfSize.y) {
            angle = -angle
        }
    }
}
