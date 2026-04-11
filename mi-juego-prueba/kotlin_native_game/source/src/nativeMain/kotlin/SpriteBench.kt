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
        try {
            println("[SpriteBench] _ready started")
            frameTimes = DoubleArray(FRAME_COUNT) { 0.0 }

            // Try to create Texture2D - but first let's test WITHOUT texture
            val icon = Texture2D(ResourceLoader.instance.load("res://icon.svg".asGodotString()).rawPtr)
            println("[SpriteBench] Texture2D wrapper created")

            // From Swift:
            // TODO: When running from the editor, getWindow().size and getViewportRect() return zero values.
            val vpw = ProjectSettings.instance
                .getSetting(name = "display/window/size/viewport_width".asGodotString(), defaultValue = Variant(1920))
                .asIntOrNull()
            val vph = ProjectSettings.instance
                .getSetting(name = "display/window/size/viewport_height".asGodotString(), defaultValue = Variant(1080))
                .asIntOrNull()
            windowSize = Vector2(x = (vpw ?: 1920L).toDouble(), y = (vph ?: 1080L).toDouble())
            val halfSize = icon.getSize() / 2.0
            println("[SpriteBench] Window: $windowSize, HalfSize: $halfSize")

            println("[SpriteBench] Creating $SPRITE_COUNT sprites (WITHOUT texture first)...")
            (0 until SPRITE_COUNT).forEach { i ->
                try {
                    val sprite = instantiate(::Sprite)
                    // Skip texture assignment for now - test addChild first
                    sprite.halfSize = halfSize
                    sprite.windowSize = windowSize
                    sprite.pos = windowSize / 2.0
                    sprite.position = sprite.pos
                    addChildTest(node = sprite)
                    if (i % 1000 == 0) {
                        println("[SpriteBench] Added $i sprites")
                    }
                    // Now try to set texture on all sprites
                    try {
                        // sprite.texture = icon // Skip for now
                        // println("[SpriteBench] Texture assignment skipped")
                    } catch (e: Throwable) {
                        println("[SpriteBench] === Texture assignment failed ===")
                        e.printStackTrace()
                    }
                } catch (e: Throwable) {
                    println("[SpriteBench] === Sprite $i addChild failed ===")
                    e.printStackTrace()
                    throw e
                }
            }
            println("[SpriteBench] All $SPRITE_COUNT sprites added successfully")
        } catch (e: Throwable) {
            println("[SpriteBench] === _ready failed ===")
            e.printStackTrace()
        }
    }

    override fun _process(delta: Double) {
        try {
            currentFrame += 1

            if (currentFrame >= START_FRAME) {
                if (frameIndex == FRAME_COUNT) {
                    println("[SpriteBench] Frame count reached, freeing children")
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
                    addChildTest(node = edit)
                    println("[SpriteBench] TextEdit added")
                } else if (frameIndex < FRAME_COUNT) {
                    frameTimes[frameIndex] = delta
                }

                frameIndex += 1
            }
        } catch (e: Throwable) {
            println("[SpriteBench] === _process failed ===")
            e.printStackTrace()
        }
    }
}
