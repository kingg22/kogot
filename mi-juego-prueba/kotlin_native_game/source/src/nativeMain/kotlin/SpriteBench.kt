@file:OptIn(ExperimentalForeignApi::class)

import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.builtin.asGodotString
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.node.Node2D
import io.github.kingg22.godot.api.core.node.TextEdit
import io.github.kingg22.godot.api.core.refcounted.Texture2D
import io.github.kingg22.godot.api.singleton.ProjectSettings
import io.github.kingg22.godot.api.singleton.ResourceLoader
import io.github.kingg22.godot.binding.instantiate
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

private const val FRAME_COUNT = 1_000
private const val START_FRAME = 100
private const val SPRITE_COUNT = 20_000

@Godot class SpriteBench(nativePtr: COpaquePointer) : Node2D(nativePtr) {
    private lateinit var frameTimes: DoubleArray
    private var currentFrame = 0
    private var frameIndex = 0
    private var windowSize: Vector2 = Vector2.ZERO

    override fun _ready() {
        frameTimes = DoubleArray(FRAME_COUNT) { 0.0 }

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

        (0..<SPRITE_COUNT).forEach { _ ->
            val sprite = instantiate(::Sprite)
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

        if (currentFrame >= START_FRAME) {
            if (frameIndex == FRAME_COUNT) {
                for (child in getChildren().asList()) {
                    Node(child.asObject().rawPtr).queueFree()
                }

                val edit = TextEdit()
                val outText = StringBuilder(FRAME_COUNT * 12)
                for (t in frameTimes) {
                    outText.append("(").append(t).append(")\n")
                }
                edit.text = outText.toString().asGodotString()
                edit.setSize(windowSize)
                addChild(node = edit)
            } else if (frameIndex < FRAME_COUNT) {
                frameTimes[frameIndex] = delta
            }

            frameIndex += 1
        }
    }
}
