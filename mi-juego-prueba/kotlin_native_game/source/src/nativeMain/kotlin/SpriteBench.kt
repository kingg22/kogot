import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.builtin.asGodotString
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.node.Node2D
import io.github.kingg22.godot.api.core.node.TextEdit
import io.github.kingg22.godot.api.core.refcounted.Texture2D
import io.github.kingg22.godot.api.singleton.ProjectSettings
import io.github.kingg22.godot.api.singleton.ResourceLoader
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

private const val frameCount = 1_000
private const val startFrame = 100
private const val spriteCount = 20_000

// TODO the main.tscn must have this
// [node name="Main" type="SpriteBench"]
// instead of [node name="Node2D" type="Node2D"]
@Godot
@OptIn(ExperimentalForeignApi::class)
class SpriteBench(nativePtr: COpaquePointer) : Node2D(nativePtr) {
    private lateinit var frameTimes: DoubleArray
    private var currentFrame = 0
    private var frameIndex = 0
    private var windowSize: Vector2 = Vector2.ZERO

    override fun _ready() {
        frameTimes = DoubleArray(frameCount) { 0.0 }

        // TODO in swift bench performs a safe cast to Texture2D, currently doesn't provide a API to do that with GD.load
        val icon = ResourceLoader.instance.load("res://icon.svg".asGodotString())
            .let { Texture2D(it.rawPtr) }
        /* ?: run {
            GD.pushWarning("Failed to load icon texture")
            return
        }
         */

        // From Swift:
        // TODO: When running from the editor, getWindow().size and getViewportRect() return zero values.
        // There seems to be something wrong, but for now we use the project settings to get the viewport size.
        val vpw = ProjectSettings.instance
            .getSetting(name = "display/window/size/viewport_width".asGodotString(), defaultValue = Variant(1920))
            .asIntOrNull()
        val vph = ProjectSettings.instance
            .getSetting(name = "display/window/size/viewport_height".asGodotString(), defaultValue = Variant(1080))
            .asIntOrNull()
        windowSize = Vector2(x = (vpw ?: 1920L).toDouble(), y = (vph ?: 1080L).toDouble())
        val halfSize = icon.getSize() / 2.0

        (0..<spriteCount).forEach { _ ->
            // FIXME create a .Sprite factory function without param pointer
            val sprite = Sprite()
            sprite.texture = icon
            sprite.halfSize = halfSize
            sprite.windowSize = windowSize
            sprite.pos = windowSize / 2.0
            sprite.position = sprite.pos
            addChild(node = sprite)
        }
    }

    override fun _process(delta: Double) {
        currentFrame += 1

        if (currentFrame >= startFrame) {
            if (frameIndex == frameCount) {
                for (child in getChildren().asList()) {
                    Node(child.asObject().rawPtr).queueFree()
                }

                // FIXME create a TextEdit factory function without param pointer
                val edit = TextEdit()
                val outText = StringBuilder(frameCount * 12)
                for (t in frameTimes) {
                    outText.append("(").append(t).append(")\n")
                }
                edit.text = outText.toString().asGodotString()
                edit.setSize(windowSize)
                addChild(node = edit)
            } else if (frameIndex < frameCount) {
                frameTimes[frameIndex] = delta
            }

            frameIndex += 1
        }
    }
}
