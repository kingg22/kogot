/**
 * A tiny assertion harness for the checks that run inside the real engine (`CollectionsCheck`, `GetValueCheck`): the
 * project has no test runner for engine-bound code yet. Each check prints `[tag] PASS name` or `[tag] FAIL name -> why`,
 * and [finish] prints the totals and returns the exit code for `SceneTree.quit`.
 */
class Checks(private val tag: String) {
    private var passed = 0
    private var failed = 0

    fun check(name: String, body: () -> Unit) {
        try {
            body()
            passed++
            println("[$tag] PASS $name")
        } catch (e: Throwable) {
            failed++
            println("[$tag] FAIL $name -> ${e::class.simpleName}: ${e.message}")
        }
    }

    inline fun expect(condition: Boolean, message: () -> String) {
        if (!condition) throw AssertionError(message())
    }

    /** Passes when [body] throws an [E]; a different exception, or none, is a failure that says which. */
    @IgnorableReturnValue
    inline fun <reified E : Throwable> expectThrows(what: String, body: () -> Any?): E {
        val result = try {
            body()
        } catch (e: Throwable) {
            if (e is E) return e
            throw AssertionError("$what threw ${e::class.simpleName}: ${e.message} instead of ${E::class.simpleName}")
        }
        throw AssertionError("$what returned $result instead of throwing ${E::class.simpleName}")
    }

    /** Prints the totals and returns the exit code: `0` when every check passed. */
    fun finish(): Int {
        println("[$tag] DONE passed=$passed failed=$failed")
        return if (failed == 0) 0 else 1
    }
}
