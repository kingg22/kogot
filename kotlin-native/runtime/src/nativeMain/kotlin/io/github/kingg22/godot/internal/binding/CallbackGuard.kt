package io.github.kingg22.godot.internal.binding

/**
 * Runs [block] as the body of a callback the engine invokes through the C ABI (a `staticCFunction`), and turns
 * any [Throwable] it throws into a Godot error report plus [fallback], instead of letting it cross the C
 * boundary (issue #113).
 *
 * In Kotlin/Native an exception that escapes a `staticCFunction` terminates the whole process with no Godot-side
 * trace: one bad `_process` override, a failed `requireNotNull` on a callback argument or a throwing getter would
 * take the editor or the game down. This is the same policy as godot-rust's panic guard and C#'s exception
 * handling at the interop boundary: report, return a neutral value, keep running.
 *
 * Safe on any thread. Engine callbacks do arrive on worker threads (`ResourceFormatLoader._load` during threaded
 * loading, `_process` of a node in a `SUB_THREAD` process group, a `Callable` run by `WorkerThreadPool`), and
 * the report goes through GDExtension's `print_error_with_message`, which Godot itself calls from any thread.
 *
 * [fallback] must be the value that means "nothing happened" for that callback (`FALSE`, `null`, ...). Out
 * parameters should be set to their neutral value **before** calling this, so a failure leaves them valid.
 *
 * @param callback a short name of the entry point, shown as the function of the Godot error ("Node2D._process").
 */
@InternalBinding
public inline fun <R> guardCallback(callback: String, fallback: R, block: () -> R): R = try {
    block()
} catch (t: Throwable) {
    reportCallbackFailure(callback, t)
    fallback
}

/** [guardCallback] for a callback that returns nothing. */
@InternalBinding
public inline fun guardCallback(callback: String, block: () -> Unit) {
    guardCallback(callback, Unit, block)
}

/**
 * Reports a [Throwable] caught at the C boundary. Never throws: if reporting through the engine fails as well
 * (the extension is not initialized yet, or is already shutting down), it falls back to the native stderr.
 */
@InternalBinding
public fun reportCallbackFailure(callback: String, t: Throwable) {
    try {
        PrintBinding.errorWithMessage(
            description = "Uncaught ${t::class.qualifiedName ?: "Throwable"} in a Kotlin callback",
            message = t.message ?: t.toString(),
            function = callback,
            file = "kogot",
            pLine = 0,
            editorNotify = true,
        )
    } catch (_: Throwable) {
        // Reporting through the engine is best effort; the stack trace below is printed either way.
    }
    try {
        t.printStackTrace()
    } catch (_: Throwable) {
        // Nothing left to report to.
    }
}
