# kogot — Kotlin Native GDExtension Binding for Godot 4.6+

> [!WARNING]
> **This project is in heavy development and is not ready for production use.**
> Expect breaking changes, incomplete APIs, and rough edges. You're welcome to explore the code, but don't build anything serious on it yet.

**kogot** is an experimental Kotlin/Native binding for [Godot 4.6+](https://godotengine.org) via the GDExtension interface. The goal is to bring Kotlin to Godot with the same philosophy as [godot-rust/gdext](https://github.com/godot-rust/gdext) — a mechanical, verifiable projection of Godot's official API contract, with a minimal generated runtime layer.

## What Works (Early Stage)

| Feature                                                                 | Status                       |
|-------------------------------------------------------------------------|------------------------------|
| Engine classes (codegen from Godot JSON API)                            | ~2000+ files generated. Done |
| Builtin types: `String`, `Array`, `Dictionary`, `Variant`               | Done. Pending improvements   |
| Class registration (`register()`, `freeInstance()`, `createInstance()`) | Done. Pending processor      |
| MethodBind calls via `invoke()` with Variant conversion                 | Pending                      |
| Properties (`@Export` annotation)                                       | Pending                      |
| Virtual methods (`NodeVirtualCalls`, `NodeVirtualDispatcher`)           | In progress                  |
| Signals (`Signal0` — zero args)                                         | Design                       |
| Signals with params (`Signal1..22`)                                     | Design                       |

## Architecture

```
User Code (@Godot class)
    ↓
processor/KogotProcessor (KSP annotation processing)
    ↓
codegen/ → kotlin-native/api/build/generated/ (2000+ generated .kt files)
    ↓
kotlin-native/binding/ (runtime class registration)
    ↓
kotlin-native/ffi/ (GDExtension FFI via cinterop)
    ↓
Godot Engine (C++)
```

## Project Structure

```
kogot/
├── analysis/              # KSP backend-agnostic metadata extraction
├── codegen/               # Godot API model + KotlinPoet code generation
├── processor/             # KSP compiler plugin (annotation processing)
├── kotlin-native/
│   ├── annotations/       # User annotations: @Godot, @Export, @Rpc, @Tool
│   ├── api/               # Generated Godot API classes (build/generated/)
│   ├── binding/           # Runtime binding registration
│   ├── ffi/               # Low-level FFI to Godot C functions via .def cinterop
│   └── runtime/           # Kotlin/Native runtime support
├── jvm-ffm/               # Java FFM integration (incomplete)
├── build-logic/           # Gradle convention plugins (including jextract-gradle-plugin)
├── mi-juego-prueba/       # Test Godot project
└── docs/                  # Design docs, roadmap, and exploration notes
```

## Generated API Sample

```kotlin
// Generated from Godot's JSON API — DO NOT EDIT
class GodotString(rawPtr: COpaquePointer?) : GodotNative, AutoCloseable {
    constructor()  // empty string
    constructor(from: GodotString)  // copy constructor
    constructor(value: String)  // from Kotlin String
    override fun close() { ... }
}

// Generated Node class with full Godot API projection
public open class Node(nativePtr: COpaquePointer) : GodotObject(nativePtr) {
    public var name: StringName
    public var uniqueNameInOwner: Boolean
    public var sceneFilePath: GodotString
    // ... full Godot Node API
}
```

## User Annotations

```kotlin
@Godot class MyNode(nativePtr: COpaquePointer) : Node(nativePtr) {
    @Export var health: Int = 100

    companion object {
        val isDeadSignal: Signal0 = signal("is_dead")
        val healthSignal: Signal1<Int> = signal("health", param<Int>("value"))
    }
}
```

## Build

```bash
./gradlew assemble
```

## Roadmap

Follow the [project board](https://github.com/users/kingg22/projects/2/views/4) for updates.

## References

- [godot-rust/gdext](https://github.com/godot-rust/gdext) — Reference binding design
- [Java 25 SE – Foreign Function and Memory API](https://docs.oracle.com/en/java/javase/25/core/foreign-function-and-memory-api.html)
- [Kotlin/Native Guide](https://kotlinlang.org/docs/native-overview.html)
- [Godot 4.6 GDExtension docs](https://docs.godotengine.org/en/4.6/tutorials/scripting/gdextension/what_is_gdextension.html)
