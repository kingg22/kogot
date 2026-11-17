package io.github.kingg22.godot.test

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.fail

/** The helper that pins open bugs: it must notice the day the bug is fixed, and must not hide other failures. */
class KnownBugTest {
    @Test
    fun passesWhileTheAssertionStillFails() {
        assertKnownBug("#0") { fail("the bug is still there") }
    }

    @Test
    fun failsWhenTheAssertionStartsToPass() {
        val error = assertFailsWith<AssertionError> { assertKnownBug("#123") { } }
        assertContains(error.message.orEmpty(), "#123")
    }

    @Test
    fun doesNotHideAFailureThatIsNotAnAssertion() {
        assertFailsWith<IllegalStateException> { assertKnownBug("#0") { error("not a failed assertion") } }
    }
}
