# Godot native test harness

Runs the Kotlin/Native tests of **any module** inside a headless Godot, so code that needs the engine (`Node`, `Variant`,
`ClassDB`, registration, signals, GDScript) can have regression tests written as plain `kotlin.test` (issue #260).
It is the Gradle plugin `io.github.kingg22.kogot.test` (`gradle-plugin/plugin`, package `...gradle.test`).

The complementary half is the KSP processor test suite (#183, source in → generated binding out); this one runs the
generated binding, the registration and the runtime.

## Using it in a module

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kogot.test)          // io.github.kingg22.kogot.test
}

kotlin {
    macosArm64()
    linuxX64()
    sourceSets {
        commonTest {
            dependencies { implementation(kotlin("test")) }
        }
    }
}
```

Then write tests in `src/nativeTest/kotlin` like any Kotlin test. `@Godot` classes declared there are registered in the
engine before the tests run (the kogot processor runs on the test compilation). Run them with `macosArm64Test` /
`linuxX64Test`, or from the IDE (gutter icon, single test, class, `--tests` filters): the plugin keeps the ordinary
test task, so Gradle and the IDE get real test events.

Only the targets the host OS can run are wired (`macosArm64` on macOS, `linuxX64` on Linux). `mingwX64` is not.

### Options

There is no extension of its own: the plugin reads the `kogot { }` extension (and the `gradle.properties` fallbacks) of
the other kogot plugin, which create it whichever is applied first.

| Option (`kogot { }`) | Property | Meaning |
|---|---|---|
| `godotExecutable` | `kogot.godotExecutable` | Executable, or a macOS `.app`. Else `GODOT_BIN`, then `godot4`/`godot` in `PATH`. The same lookup (`GodotExecutableLocator`) as `runGodot` / `testGodot`. |
| `godotVersion` | `kogot.godotVersion` | Written as `compatibility_minimum` of the test `.gdextension`; Godot refuses to load it on an older engine. |
| `godotCliArgs` | | Extra arguments of every Godot process, the same ones `runGodot` gets. |
| `testTimeoutSeconds` | `kogot.testTimeoutSeconds` | Per Godot process (default 120). |
| `testIsolation` | `kogot.testIsolation` | `PER_TEST` (default) or `NONE`. |

## How it works

1. KSP runs on the test compilation (`ksp<Target>Test`) with the `kogot.generatedBindingsPackage` option, so the
   `GeneratedBindings` aggregator lives in a package that cannot clash with the main one.
2. `generateKogotTestHarness<Target>` adds a generated file to the test compilation: a `KogotTestRunner` `@Godot` node and
   the GDExtension entry point. The test compilation is also linked as a shared library.
3. `prepareKogotGodotTestHost<Target>` writes a checked-nowhere host project under `build/kogotGodotTest/<target>`
   (no assets; main scene = `KogotTestRunner`), the `.gdextension`, a `launcher.properties` and a `run-godot-tests` script.
4. The `<target>Test` task runs that script **instead of the test executable**. The Kotlin Gradle plugin gives it the
   usual arguments (`--ktest_logger=TEAMCITY`, `--ktest_gradle_filter=...`) and converts the TeamCity messages it prints
   into Gradle test events. That is what makes the IDE work.
5. The script starts `GodotTestLauncher` on the Gradle JVM. It lists the tests (`--ktest_list_tests` inside Godot),
   applies the filters, and runs **each test in its own `godot --headless`**, re-emitting the runner's messages (one suite
   per class). `KogotTestRunner._ready` runs the Kotlin/Native test runner on the main thread with a live `SceneTree`
   and quits with its exit code; it does not start at extension initialization because the engine is not fully up there
   (GDScript cannot compile yet).
6. A Godot that crashes or hangs becomes a failed test with the engine's last output (and the build fails); the
   process is killed on timeout.

## Writing tests

- Tests run on the main thread. Engine objects a test creates are not freed for it; each test has a fresh engine.
- Synchronous only: a test cannot wait for frames or timers (the runner is inside `_ready`).
- Fixtures (`@Godot` classes) belong to the test source set: the main source set's classes are not registered in the test
  library.
- The test module sees the main classes, so give fixtures their own package.
- **Open bugs are pinned, not left red.** Wrap the assertion the fixed code must satisfy in `assertKnownBug("#123") { ... }`:
  the test passes while the bug exists and fails the day it is fixed, saying to remove the wrapper and keep the body as
  the regular assertion. Only an `AssertionError` counts as "still broken", so a crash or a script that does not compile
  is never mistaken for the bug. Name such tests after the buggy behaviour (`..._254`), and keep the issue number.

## CI

`.github/workflows/gradle.yml` keeps the Godot tests out of the regular `build` job and runs them in their own job,
`godot-tests`:

- `build` excludes the test tasks of `mi-juego-prueba:kotlin_native_game` (`allTests`, the per-target `*Test`, and the
  test shared library link). Without that it would compile and link the whole test source set and then fail for lack of a
  Godot executable. Excluding those tasks also removes everything only they need (test compilation, KSP on tests).
- `godot-tests` downloads the official Godot Linux build (version pinned in the job, SHA-512 checked against the
  release's `SHA512-SUMS.txt`) and exports it as `GODOT_BIN`, the variable `GodotExecutableLocator` reads.
- Compile/link and run are **separate steps with separate Gradle processes**, so the memory of the JVM and the
  Kotlin/Native compiler is released before the tests start. The tests themselves are light because each one runs in
  its own Godot process, one after another (`testIsolation = PER_TEST`, the default).
- The job has a 30 minute timeout (the first Linux run took about 14, two of them running the tests) and uploads its
  reports as an artifact. It is blocking: the suite is green, because open bugs are pinned with `assertKnownBug` (below)
  instead of being left red.

## Known limits / next steps

- One Godot start per test (about a second each): `NONE` is available when speed matters more than isolation.
- Windows is not wired.
- The launcher is a JVM program started by a small script; the script needs no extra tooling (it uses Gradle's JVM).
- CI matrix over several Godot versions is tracked separately; `kogot.godotExecutable` / `GODOT_BIN` are the hooks for it.
- Sharding the CI job by test class (several runners, each one a filtered `--tests` run) is possible with the same
  filters the IDE uses; not done because a single runner is enough for the current ~260 tests.
