package io.github.kingg22.godot.test

import kotlin.test.fail

/**
 * Pins a bug that is still open. [positive] is the assertion the fixed code must satisfy; today it must **fail**, and
 * the test passes because of that. The day the bug is fixed [positive] passes, this test fails and says so: remove the
 * wrapper and keep the body as the ordinary assertion.
 *
 * Only an `AssertionError` counts as "the bug is still there"; any other exception (a crash of the call itself, a
 * missing class) is a different problem and fails the test.
 *
 * @param issue the open issue, for example `"#254"`.
 */
inline fun assertKnownBug(issue: String, positive: () -> Unit) {
    val stillBroken = try {
        positive()
        false
    } catch (_: AssertionError) {
        true
    }
    if (!stillBroken) {
        fail(
            "$issue looks fixed: this test pinned the bug. Remove assertKnownBug and keep its body as the regular assertion.",
        )
    }
}
