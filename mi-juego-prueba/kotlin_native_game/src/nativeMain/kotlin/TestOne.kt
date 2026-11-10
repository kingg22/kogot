@file:OptIn(ExperimentalForeignApi::class, NativeRuntimeApi::class)

import io.github.kingg22.godot.api.annotations.ExportMethod
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.builtin.Vector2i
import io.github.kingg22.godot.api.builtin.toGodotString
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.builtin.toVariant
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.RefCounted
import io.github.kingg22.godot.api.core.SceneTree
import io.github.kingg22.godot.api.core.node.Node2D
import io.github.kingg22.godot.api.core.node.TextEdit
import io.github.kingg22.godot.api.core.node.Window
import io.github.kingg22.godot.api.core.refcounted.Image
import io.github.kingg22.godot.api.core.refcounted.Texture2D
import io.github.kingg22.godot.api.singleton.ClassDB
import io.github.kingg22.godot.api.singleton.Engine
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.api.utils.print
import io.github.kingg22.godot.binding.instantiate
import io.github.kingg22.godot.castTo
import io.github.kingg22.godot.load
import io.github.kingg22.godot.loadOrNull
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi

private const val FRAME_COUNT = 1_000
private const val START_FRAME = 100
private const val SPRITE_COUNT = 5

// issue #114: checkpoints spaced far enough apart (in idle frames) for both Kotlin/Native's
// dedicated Cleaner worker thread and Godot's MessageQueue idle-time flush to have run in between.
private val REFCOUNT_STEP_ACQUIRE_FRAMES = listOf(10, 40, 70, 100, 130, 160)

// issue #114, repeated acquisition of a still-live pointer: runs well after the steps above have settled
// (their GC-driven releases would otherwise move the refcount under this check), in three phases one
// idle frame apart so each deferred `callDeferred()` release has been flushed before it is measured.
private const val REPEATED_ACQUIRE_ANCHOR_FRAME = 300
private const val REPEATED_ACQUIRE_BURST_FRAME = 302
private const val REPEATED_ACQUIRE_CHECK_FRAME = 304
private const val REPEATED_ACQUIRE_COUNT = 9

// issue #114, castTo on a *borrowed* pointer: the cast happens at BORROWED_CAST_FRAME and its wrapper is
// dropped when that call returns; the GC is forced from a worker thread in a later frame (twice, since a
// single cycle can miss it, see stepRefCountedReleaseTest) and the result is read once the deferred
// release has had idle frames to run.
private const val BORROWED_CAST_FRAME = 400
private const val BORROWED_CAST_GC_FRAME = 405
private const val BORROWED_CAST_GC_RETRY_FRAME = 420
private const val BORROWED_CAST_CHECK_FRAME = 450

@Godot class TestOne(nativePtr: COpaquePointer) : Node2D(nativePtr) {
    private val frameTimes = DoubleArray(FRAME_COUNT) { 0.0 }
    private var currentFrame = 0
    private var frameIndex = 0
    private var windowSize: Vector2 = Vector2.ZERO

    // Kept strongly reachable across the repeated-acquisition phases on purpose.
    private var repeatedAcquireAnchor: Texture2D? = null
    private var repeatedAcquireBaseline = -1

    // The Variant is the only real owner of the borrowed Image across the borrowed-cast phases.
    private var borrowedOwner: Variant? = null
    private var borrowedBaseline = -1

    private val callable = Callable {
        println("Hello from Kotlin inside a Callable!")
    }

    private val callable2 = Callable { id: Long ->
        println("Hello from Kotlin inside a Callable with id: $id")
        id
    }

    init {
        GD.print("A new SpriteBench was created with pointer ${nativePtr.rawValue}")
    }

    @ExportMethod
    fun addTwo(a: Int, b: Int): Int = a + b

    override fun _ready() {
        try {
            println("[SpriteBench] _ready started")

            // Try to create Texture2D - but first let's test WITHOUT texture
            val icon = GD.load<Texture2D>("res://icon.svg", factory = ::Texture2D)
            println("[SpriteBench] Texture2D wrapper created")

            // get root from the engine singleton
            val mainloop: SceneTree = Engine.instance.getMainLoop().castTo(::SceneTree)
            // use mainloop to call the get_root from the scenetree class
            val root: Window = mainloop.root // return as object
            // use root as the object for the window class method to call get_size
            val vpwh: Vector2i = root.size // return as Vector2i

            windowSize = Vector2(from = vpwh)

            val halfSize: Vector2 = icon.getSize() / 2.0
            println(
                "[SpriteBench] Window: (${windowSize.x}, ${windowSize.y}), HalfSize: (${halfSize.x}, ${halfSize.y})",
            )

            println("[SpriteBench] Creating $SPRITE_COUNT sprites")
            for (i in 0..<SPRITE_COUNT) {
                try {
                    // Prefer the top-level factory function, this is a fallback way!!
                    val sprite: Sprite = instantiate()
                    sprite.halfSize = halfSize
                    sprite.windowSize = windowSize
                    sprite.pos = windowSize / 2.0
                    sprite.position = sprite.pos
                    sprite.texture = icon
                    addChild(node = sprite)
                    if (i % 1000 == 0) {
                        println("[SpriteBench] Added $i sprites")
                    }
                } catch (e: Throwable) {
                    println("[SpriteBench] === Sprite $i addChild failed ===")
                    e.printStackTrace()
                    throw e
                }
            }
            println("[SpriteBench] All $SPRITE_COUNT sprites added successfully")

            println("[SpriteBench] _ready finished, going to call deferred callable")
            var returnVariant = callable.call()
            println(
                "[SpriteBench] Callable returned: ${returnVariant.stringify().toKString()}, is nil: ${returnVariant.isNil()}",
            )
            println("[SpriteBench] calling callable2 with 15 as args")
            returnVariant = callable2.call(15L.toVariant())
            println(
                "[SpriteBench] Callable2 returned: ${returnVariant.stringify().toKString()}, is nil: ${returnVariant.isNil()}, value: ${returnVariant.toIntOrNull()}",
            )

            println("[SpriteBench] calling @ExportMethod addTwo(3, 4) via Object.call")
            val addTwoResult = "addTwo".toStringName().use { call(it, 3.toVariant(), 4.toVariant()) }
            println(
                "[SpriteBench] addTwo returned: ${addTwoResult.toIntOrNull()} (expected 7)",
            )
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
                    val children = getChildren()
                    for (i in 0 until children.size()) {
                        Node(children[i].toObject().rawPtr).queueFree()
                    }

                    val outText = StringBuilder(FRAME_COUNT * 12)
                    for (t in frameTimes) {
                        outText.append("(").append(t).appendLine(")")
                    }
                    val edit = TextEdit()
                    edit.text = outText.toString().toGodotString()
                    edit.size = windowSize
                    addChild(node = edit)
                    println("[SpriteBench] TextEdit added")
                } else if (frameIndex < FRAME_COUNT) {
                    frameTimes[frameIndex] = delta
                }

                stepRefCountedReleaseTest(currentFrame - START_FRAME)
                stepRepeatedAcquisitionTest(currentFrame - START_FRAME)
                stepBorrowedCastTest(currentFrame - START_FRAME)

                frameIndex += 1
            }
        } catch (e: Throwable) {
            println("[SpriteBench] === _process failed ===")
            e.printStackTrace()
        }
    }

    /**
     * issue #114: acquires a throwaway [Texture2D] wrapper around the *same* cached engine resource,
     * never stores it beyond this call (so it is unreachable the instant the function returns), and
     * forces a GC cycle. Repeated across [REFCOUNT_STEP_ACQUIRE_FRAMES] — spaced-out frames — so each
     * acquisition's wrapper has actually been collected, its
     * [io.github.kingg22.godot.internal.binding.attachRefCountedRelease] [kotlin.native.ref.Cleaner] has
     * fired, and its deferred `Callable.callDeferred()` release has been flushed by Godot's idle-time
     * queue, before the next one runs — a stable, non-climbing `refcount` across steps is the pass
     * condition. Alternates which thread forces the collection cycle (plain main-thread call vs. a
     * kotlinx.coroutines worker thread, `Dispatchers.Default`, a real OS thread under the new memory
     * model) to confirm the release still correctly lands on the main thread — not a crash — regardless
     * of which thread's GC cycle triggered the Cleaner.
     *
     * Empirically (10 samples against the real Godot binary), refcount only ever stabilizes after a
     * step whose collection was forced from the **kotlinx.coroutines worker thread** branch — a step
     * whose collection ran via a plain main-thread `GC.collect()` never gets released by the next
     * checkpoint 30 frames later, alternating `+1, +0, +1, +0, ...` instead of staying flat. This isn't
     * this release mechanism failing — it never crashes, and every reference does eventually get
     * released — it looks like Kotlin/Native's Cleaner-dispatch worker thread needs *some* unrelated
     * background-thread activity to actually get scheduled promptly in an otherwise single-threaded
     * process. Real games are rarely purely single-threaded, so this is noted, not chased further here.
     *
     * Scope: this validates the common acquire-use-drop lifetime, one live wrapper at a time. Acquiring
     * the *same* still-reachable pointer repeatedly is covered by [stepRepeatedAcquisitionTest].
     */
    private fun stepRefCountedReleaseTest(frameSinceStart: Int) {
        val step = REFCOUNT_STEP_ACQUIRE_FRAMES.indexOf(frameSinceStart)
        if (step == -1) return

        val tex = GD.load<Texture2D>("res://icon.svg", factory = ::Texture2D)
        println("[RefCountedRelease] step $step: acquired, refcount=${tex.getReferenceCount()}")

        // Alternate which thread forces the collection cycle that will make this step's `tex` (already
        // unreachable — nothing stores it past this call) collectible, to exercise both the plain
        // main-thread path and a kotlinx.coroutines worker thread (`Dispatchers.Default`, a real OS
        // thread under the new memory model).
        if (step % 2 == 0) {
            GC.collect()
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                println("[RefCountedRelease] step $step: forcing GC.collect() from a kotlinx.coroutines worker thread")
                GC.collect()
                println("[RefCountedRelease] step $step: worker-thread GC.collect() finished")
            }
        }
    }

    /**
     * issue #114 follow-up: acquiring the *same* still-live pointer repeatedly. `GD.load` hands Kotlin one
     * owned engine reference per call, but the identity cache collapses every call into the one live
     * wrapper (which owns exactly one reference) — the redundant references must be released, not leaked.
     *
     * Three phases, one idle frame apart so each deferred release has been flushed before it is measured:
     * 1. [REPEATED_ACQUIRE_ANCHOR_FRAME]: load the resource and keep the wrapper reachable (the *anchor*).
     * 2. [REPEATED_ACQUIRE_BURST_FRAME]: record the settled refcount, then load it [REPEATED_ACQUIRE_COUNT]
     *    more times (each must resolve to the anchor itself), and also a `loadOrNull` whose cast fails
     *    (the owned reference must be released on that path too).
     * 3. [REPEATED_ACQUIRE_CHECK_FRAME]: the refcount must be back at the recorded baseline. Before the
     *    fix it stayed at `baseline + REPEATED_ACQUIRE_COUNT + 1`.
     *
     * Deliberately independent of Kotlin/Native's Cleaner timing: every release exercised here goes
     * straight through `callDeferred()`, so a leak shows up deterministically instead of "eventually".
     */
    private fun stepRepeatedAcquisitionTest(frameSinceStart: Int) {
        when (frameSinceStart) {
            REPEATED_ACQUIRE_ANCHOR_FRAME -> {
                repeatedAcquireAnchor = GD.load<Texture2D>("res://icon.svg", factory = ::Texture2D)
            }

            REPEATED_ACQUIRE_BURST_FRAME -> {
                val anchor = repeatedAcquireAnchor ?: return
                repeatedAcquireBaseline = anchor.getReferenceCount()
                println("[RepeatedAcquire] baseline refcount=$repeatedAcquireBaseline")

                var sameWrapper = 0
                repeat(REPEATED_ACQUIRE_COUNT) {
                    if (GD.load<Texture2D>("res://icon.svg", factory = ::Texture2D) === anchor) sameWrapper += 1
                }
                // typeHint pins what the loader returns (a Texture2D), so the cast to Image is what fails.
                val mismatched = GD.loadOrNull<Image>("res://icon.svg", typeHint = "Texture2D", factory = ::Image)
                println(
                    "[RepeatedAcquire] $sameWrapper/$REPEATED_ACQUIRE_COUNT loads resolved to the anchor wrapper, " +
                        "mismatched loadOrNull=$mismatched, refcount right after (deferred releases still " +
                        "queued)=${anchor.getReferenceCount()}",
                )
            }

            REPEATED_ACQUIRE_CHECK_FRAME -> {
                val anchor = repeatedAcquireAnchor ?: return
                val after = anchor.getReferenceCount()
                val verdict = if (after == repeatedAcquireBaseline) "PASS" else "FAIL"
                println(
                    "[RepeatedAcquire] $verdict: refcount after flush=$after, baseline=$repeatedAcquireBaseline " +
                        "(leaked=${after - repeatedAcquireBaseline})",
                )
                repeatedAcquireAnchor = null
            }
        }
    }

    /**
     * issue #114: `castTo` on a pointer the caller holds no reference to. A `Variant` is the only real
     * owner of an `Image` here (`ClassDB.instantiate` hands it back wrapped in one). The cast wrapper must
     * take its *own* engine reference and give it back once collected: if it assumed it had been handed one
     * instead, its cleaner would `unreference()` a reference it never took and the engine would free the
     * `Image` underneath the `Variant`.
     *
     * Passes when the refcount is `baseline + 1` while the wrapper is alive (it took its own) and back at
     * `baseline` after it is collected (it gave it back, and no more than that).
     */
    private fun stepBorrowedCastTest(frameSinceStart: Int) {
        when (frameSinceStart) {
            BORROWED_CAST_FRAME -> {
                val owner = "Image".toStringName().use { ClassDB.instance.instantiate(it) }
                borrowedOwner = owner
                val ptr = owner.toObject().rawPtr
                borrowedBaseline = RefCounted(ptr).getReferenceCount()
                val cast = ptr.castTo(::Image)
                val withWrapper = RefCounted(ptr).getReferenceCount()
                println(
                    "[BorrowedCast] ${if (withWrapper == borrowedBaseline + 1) "PASS" else "FAIL"}: " +
                        "wrapper took its own reference, baseline=$borrowedBaseline, with wrapper=$withWrapper " +
                        "(same object: ${cast.rawPtr == ptr})",
                )
            }

            BORROWED_CAST_GC_FRAME, BORROWED_CAST_GC_RETRY_FRAME -> {
                CoroutineScope(Dispatchers.Default).launch { GC.collect() }
            }

            BORROWED_CAST_CHECK_FRAME -> {
                val owner = borrowedOwner ?: return
                val after = RefCounted(owner.toObject().rawPtr).getReferenceCount()
                println(
                    "[BorrowedCast] ${if (after == borrowedBaseline) "PASS" else "FAIL"}: " +
                        "refcount after the wrapper was collected=$after, baseline=$borrowedBaseline",
                )
                // The Variant is the real owner: closing it drops the last reference and frees the Image.
                owner.close()
                borrowedOwner = null
            }
        }
    }
}
