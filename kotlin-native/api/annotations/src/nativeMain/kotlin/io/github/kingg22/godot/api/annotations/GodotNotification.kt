package io.github.kingg22.godot.api.annotations

/**
 * Opt-in hook for the engine `notification` callback on a `@Godot` class.
 *
 * A `@Godot` class that implements this interface receives every `NOTIFICATION_*` Godot sends to the
 * object through [_notification] — most importantly `GodotObject.NOTIFICATION_PREDELETE`, sent right
 * before the object is freed.
 *
 * Kotlin/Native's GC will eventually reclaim Kotlin objects, but Godot has no GC and tears the engine
 * down first, so any native handle a class holds for its whole lifetime — a builtin like
 * [io.github.kingg22.godot.api.builtin.StringName] / `Signal` / `Callable`, or a manually created
 * engine object — must be released here or it is reported as leaked at exit
 * (`Orphan StringName …` / `ObjectDB instances were leaked`).
 *
 * ```kotlin
 * @Godot
 * class MyNode(nativePtr: COpaquePointer) : Node(nativePtr), GodotNotification {
 *     private val actionName = "jump".toStringName()
 *
 *     override fun _notification(what: Int) {
 *         if (what == GodotObject.NOTIFICATION_PREDELETE.toInt()) actionName.close()
 *     }
 * }
 * ```
 *
 * Implementing this interface is what wires the callback up; classes that do not implement it pay no
 * per-notification cost.
 */
public interface GodotNotification {
    /** Handles a Godot `NOTIFICATION_*` constant. Called for every notification sent to this object. */
    public fun _notification(what: Int)
}
