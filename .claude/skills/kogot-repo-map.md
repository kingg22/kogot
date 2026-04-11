---
name: kogot-repo-map
description: Quick navigation aid for the kogot codebase. Use when exploring modules, finding key files, understanding generated vs hand-written code, or needing to understand the project structure. Trigger on: exploring kogot, finding where something is implemented, understanding code flow.
---

# Kogot Repo Map

Quick navigation aid for the kogot codebase. Where to find what.

## Module Map

| Module                       | Purpose                                        | Key Files                                             |
|------------------------------|------------------------------------------------|-------------------------------------------------------|
| `analysis/`                  | KSP backend-agnostic metadata extraction       | `extractors/`, `models/ClassInfo.kt`                  |
| `codegen/`                   | Godot API model + KotlinPoet generation        | `models/extensionapi/`, `impl/KotlinPoetGenerator.kt` |
| `processor/`                 | KSP compiler plugin (annotation processing)    | `KogotProcessor.kt`, `generation/kotlin/`             |
| `kotlin-native/annotations/` | User annotations: @Godot, @Export, @Rpc, @Tool | -                                                     |
| `kotlin-native/api/`         | User API + **generated** Godot classes         | `build/generated/` (2000+ files)                      |
| `kotlin-native/binding/`     | Runtime binding registration                   | `ClassRegistrationHelpers.kt`                         |
| `kotlin-native/ffi/`         | Low-level FFI to Godot C functions             | `.def` cinterop files                                 |
| `kotlin-native/runtime/`     | Kotlin/Native runtime support                  | -                                                     |
| `jvm-ffm/`                   | Java FFM integration (incomplete)              | -                                                     |
| `build-logic/`               | Gradle convention plugins                      | `buildlogic.*.gradle.kts`                             |

## Generated vs Hand-Written

**Generated** (don't edit directly):
- `kotlin-native/api/build/generated/sources/godotApi/**` - Godot API classes
- `processor/build/generated/**` - KSP processor output

**Hand-written**:
- All source in `analysis/`, `codegen/`, `processor/`
- `kotlin-native/annotations/`, `binding/`, `ffi/`, `runtime/`

## Key Files by Task

| Task                        | Key Files                                              |
|-----------------------------|--------------------------------------------------------|
| Add new Godot builtin type  | `codegen/models/extensionapi/`, run codegen            |
| Add new engine class        | `codegen/` + run API generation                        |
| Fix KSP processor           | `processor/KogotProcessor.kt`                          |
| Add new annotation          | `kotlin-native/annotations/` + `processor/validation/` |
| Modify binding registration | `kotlin-native/binding/ClassRegistrationHelpers.kt`    |
| Change FFI layer            | `kotlin-native/ffi/` + `.def` files                    |
| Debug spritebench           | `mi-juego-prueba/kotlin_native_game/`                  |

## Code Flows

**Annotation flow:**
```
@Godot → processor/KogotProcessor → GodotBindingGenerator → *_Binding.kt
```

**Codegen flow:**
```
Godot JSON API → codegen/models/ → KotlinPoetGenerator → kotlin-native/api/build/generated/
```

**Registration flow:**
```
register() → ClassRegistrationHelpers → GDExtension API → Godot ClassDB
```

## Docs to Read

- `docs/kogot_binding_philosophy.md` - Design principles
- `docs/roadmap-22feb.md` - Historical roadmap
- `docs/summary-6march.md` - Recent status
- `docs/signal-design.md` - Signal implementation
