---
name: godot-debug
description: Debug Kotlin Native + Godot GDExtension issues. Use when debugging crashes, extension loading problems, method binding errors, or Kotlin/Native memory issues in kogot. Trigger on: GDExtension errors, crashes in Godot, native debugging, API version mismatch.
---

# Godot Debug - kogot

How to debug Kotlin Native + Godot GDExtension issues.

## Running Godot

| Mode                 | Command                                                    |
|----------------------|------------------------------------------------------------|
| Editor               | `godot --editor --path /path/to/project`                   |
| Headless server      | `godot --headless --path /path/to/project`                 |
| Headless with script | `godot --headless -s res://scripts/main.gd`                |
| Export               | `godot --headless --export-release linux/x11 /output/path` |

## Common GDExtension Errors

**"Extension library not found"** - Check `.gdextension` file has correct `entry` path to `.so`/`.dll`, verify library compiled and in correct location

**"Symbol not found" / "Procedure not found"** - MethodBind name mismatch between JSON API and actual Godot version, check `godot-version/` matches your Godot binary version

**"Invalid method bind"** - Method name changed in Godot update, re-run API generation: `./gradlew :codegen:run --refresh`

**Crash on createInstance** - ObjectPtr may be null or wrong type, check `freeInstance()` was not called on live object

## Kotlin Native Debugging

**Memory issues** - Kotlin/Native uses ARC-like memory model, use `kotlin.native.internal.GC.logMemoryUsage()` for diagnostics

**Stack traces** - Kotlin/Native stack traces can be cryptic, use `konan.lldb` for native debugging

**Debug build** - `./gradlew :kotlin-native:binding:linkDebug` then check build/logs for symbols

## Attaching Debugger

**GDB to Godot process:**
```bash
gdb godot
(gdb) run --editor --path /path/to/project
# When crash occurs
(gdb) bt
(gdb) info threads
(gdb) thread apply all bt
```

**LLDB to Godot process:**
```bash
lldb godot
(lldb) run --editor --path /path/to/project
(lldb) bt
```

## Checking API Version Mismatch

```bash
# Generate API dump
./gradlew :codegen:generateGodotApi
# Check kotlin-native/api/build/generated/sources/

# Compare with godot binary
godot --version
# Ensure version in godot-version/ matches
```

## Spritebench-specific Debug

Common issues in `mi-juego-prueba/kotlin_native_game/`:
- Texture2D loading via GD.load
- Editor viewport size returning zero (known issue)
- Static properties captured in lambdas (see commit bddfc1a fix)

## Quick Checks

1. **Is extension loaded?** → Check Godot console for "Loaded extension"
2. **Is registerClass called?** → Add log in `onInitScene()`
3. **Is createInstance working?** → Check return ptr != null
4. **Is method invocation working?** → Test with simple method first
