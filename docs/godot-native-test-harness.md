# Godot native test harness

Runs Kotlin/Native tests **inside a headless Godot**, so code that needs the engine (`Node`, `Variant`, `ClassDB`,
registration, signals, GDScript) can have regression tests. Module: `kotlin-native/godot-test` (issue #260).

The complementary half is the KSP processor test suite (#183, source in → generated binding out); this one runs the
generated binding, the registration and the runtime.

## How it works

1. The test source set (`src/nativeTest`) is compiled with the Kotlin/Native test runner (`-generate-test-runner`) and
   linked as a **shared library** (`libkogot_godot_tests`), not as the usual test executable.
2. The kogot processor runs on the test source set (`kspMacosArm64Test` / `kspLinuxX64Test`), so every `@Godot` class
   declared in the tests is registered like in a game. The generated `GeneratedBindings` is wired to the GDExtension
   entry point `kogot_test_init` (`src/harnessEntry`, compiled in the same fragment as the generated code).
3. The `godotTest` task copies the library to `host/bin`, writes `host/kogot_tests.gdextension`, runs
   `godot --headless --import` (Godot only learns about an extension from the import pass) and then
   `godot --headless --path host`.
4. `host/main.tscn` has a single `HarnessRunner` node. Its `_ready` starts the Kotlin/Native test runner (TeamCity
   logger) on the main thread with a live `SceneTree` and quits with the runner's exit code. It is not started at
   extension initialization: the engine is not fully up there (GDScript cannot compile yet).
5. The task parses the runner's output and writes JUnit XML to `build/test-results/godotTest/TEST-godotTest.xml`
   plus the full engine output to `godot-output.log` in the same folder.

## Running

```bash
./gradlew :kotlin-native:godot-test:godotTest
```

`godotTest` is part of `check` of the module. The host target is picked from the OS (`macosArm64`, `linuxX64`);
`mingwX64` is not wired yet.

### Selecting Godot

Resolution order (same idea as `godot-version/track.sh`):

| Option                         | Meaning                                                                                      |
|--------------------------------|----------------------------------------------------------------------------------------------|
| `-Pgodot.bin=<path>` / `GODOT_BIN` | Executable, or a macOS `.app` bundle (resolved to `Contents/MacOS/Godot`).               |
| `-Pgodot.version=4.7.2`        | Looks in `~/Downloads` for `Godot_v4.7.2-stable*` and fails if the binary reports another minor. |
| none                           | `godot` / `godot4` in `PATH`.                                                                |
| `-Pgodot.timeout=<seconds>`    | Kills the run after this time (default 120).                                                 |

The task always runs (the engine binary is not a tracked input).

### Failures, crashes and timeouts

The build fails on: a failing test (message and Kotlin stack trace are printed and in the XML), a non-zero engine
exit before the runner reported its result (a crash; the message names the **last started test** and
`godot-output.log` has the engine's stderr), a timeout (the process is killed), and a run with no tests.
Everything else (engine `ERROR:` lines that do not stop the run) is only in the log.

## Writing a test

Plain `kotlin.test` in `kotlin-native/godot-test/src/nativeTest/kotlin`:

```kotlin
class MyTest {
    @Test
    fun probeTriplesFromGdscript() {
        val probe = instantiate<HarnessProbe>()      // a @Godot fixture, registered before the tests start
        // ... assert through the engine
    }
}
```

Fixtures (`@Godot` classes) live in `Fixtures.kt`. Remember:

- Tests run on the main thread, in a single Godot process for the whole suite.
- `@Ignore` is reported as skipped. `aChildDeclaredBeforeItsParentIsRegistered_issue254` is ignored until the processor
  registers parent-first; remove the `@Ignore` when #254 is fixed.
- A class whose registration the engine refuses prints an `ERROR:` in the log but does not stop the run.
- Engine objects created by a test are not freed automatically; free what you need to keep counters meaningful.

## Known limits / next steps

- One process for the whole suite (a crash hides the rest of the tests); per-class processes are a possible follow-up.
- Windows is not wired (`mingwX64` is commented out in the repo).
- CI matrix over several Godot versions is tracked separately; `-Pgodot.version` / `-Pgodot.bin` are the hooks for it.
