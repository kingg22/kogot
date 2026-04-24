# Contributing to kogot

> [!NOTE]
> The project is still in development. This is not ready for anything yet.

Welcome! kogot is an experimental Kotlin/Native GDExtension binding for Godot 4.6+. We're at an early stage with plenty to do. Contributions are welcome.

## How to Contribute

### 1. Pick an Issue

Browse the [issue tracker](https://github.com/kingg22/kogot/issues) and find something that interests you. Comment on the issue saying you'd like to work on it so we can assign it to you.

### 2. Create a New Issue

If you found a bug, have a feature request, or want to propose a change:
- Open an issue with a clear description
- For bugs: include steps to reproduce, expected vs actual behavior
- For features: describe the problem you're solving and why it matters

### 3. Development Setup

```bash
# Clone the repository
git clone https://github.com/kingg22/kogot.git
cd kogot

# Build the project
./gradlew assemble

# Run tests (if any)
./gradlew test
```

### 4. Testing the Sample

Run the `mi-juego-prueba` project with [Godot v4.6.1](https://godotengine.org/download/archive/)
- Currently only tested on Linux, sometimes on Windows. In macOS testing is welcome.
- If you have problems with the Godot project, delete the `.uid` file of `addons/gdkotlin-native`.

## Key Resources

- [CLAUDE.md](CLAUDE.md) — Agent directives and project context
- [binding-roadmap.md](.claude/skills/binding-roadmap.md) — Track what's implemented, in progress, and pending
- [docs/signal-design.md](docs/technical-design/signal-design.md) — Signal implementation design
- [docs/kogot_binding_philosophy.md](docs/kogot_binding_philosophy.md) — Design principles and architecture guidelines
- [Java 25 SE – Foreign Function and Memory API](https://docs.oracle.com/en/java/javase/25/core/foreign-function-and-memory-api.html)
- [Kotlin/Native Guide](https://kotlinlang.org/docs/native-overview.html)

## Modules

| Module                       | Purpose                                        |
|------------------------------|------------------------------------------------|
| `analysis/`                  | KSP backend-agnostic metadata extraction       |
| `codegen/`                   | Godot API model + KotlinPoet generation        |
| `processor/`                 | KSP compiler plugin (annotation processing)    |
| `kotlin-native/annotations/` | User annotations: @Godot, @Export, @Rpc, @Tool |
| `kotlin-native/api/`         | User API + **generated** Godot classes         |
| `kotlin-native/binding/`     | Runtime binding registration                   |
| `kotlin-native/ffi/`         | Low-level FFI to Godot C functions             |
| `build-logic/`               | Gradle convention plugins                      |

## Design Philosophy

kogot follows the philosophy of [godot-rust/gdext](https://github.com/godot-rust/gdext): the public API must be a mechanical, verifiable projection of Godot's official contract. The generated code must be traceable from JSON/API official docs to Kotlin code without manual jumps. See [docs/kogot_binding_philosophy.md](/docs/kogot_binding_philosophy.md) for details.

## Code Quality

When contributing, keep in mind:
- Generated code lives in `kotlin-native/api/build/generated/` — do not edit directly
- Handwritten code is in `analysis/`, `codegen/`, `processor/`, `kotlin-native/annotations/`, `binding/`, `ffi/`, `runtime/`
- The KSP processor must compile without errors
- Run `./gradlew spotlessApply ktlintFormat` before committing if configured

## Questions?

Open an issue and tag it as a question. We're happy to help and colaborate together.
