---
name: binding-roadmap
description: Track progress on kogot GDExtension binding development. Use this skill whenever you need to know what's implemented, what's in progress, or what gaps remain in the binding. Trigger when: checking binding status, planning new features, referencing what's done vs pending, or needing to understand the binding architecture layers.
---

# Binding Roadmap - kogot

Tracks what's done, what's in progress, and what's pending for the kogot GDExtension binding. Update this after each significant milestone.

## Current Status (2026-04-08)

### ✅ COMPLETE

**Class Registration** - `register()`, `freeInstance()`, `createInstance()` working, `ClassRegistrationHelpers` implemented

**MethodBind Calls** - `invoke()` with Variant conversion working, Method dispatch via `MethodBind`

**Builtin Types** - String, Array, Dictionary, Variant. `Array<T>` generic interceptor (ArrayGenericConfig), `TypeAliasGenerator` creates `VariantArray = GodotArray<Variant>`

**Engine Classes (codegen)** - Node, Node2D, Sprite2D and ~2000+ generated .kt files from Godot's JSON API

**Properties (@Export)** - Basic property binding done, `@Export` annotation works for simple types

**Virtual Methods** - `NodeVirtualCalls` and `NodeVirtualDispatcher` implemented

**Signals** - `Signal0` (0 args) works via `SignalEmitter`

---

### 🔄 IN PROGRESS

**Signals (Signal1-Signal22)** - Signal1.kt through Signal22.kt not implemented, native callback trampoline not done

**KSP Processor** - MVP landed, validation pipeline exists but limited checks

---

### ❌ NOT STARTED

**Dictionary<K, V> typed** - Planned but not implemented, `GenericBuiltinInterceptor` needs extension

**Safe cast via GD.load** - `GD.load<Texture2D>("path")` not available, blocks spritebench texture loading

**Editor viewport size** - Returns zero in editor context

**Java FFM code generation** - `JavaFfmImplGenerator.kt` not implemented

**ScriptInstance / ScriptLanguage** - Not started, needed for full Godot script integration

---

## Binding Layer Architecture

```
User Code (@Godot class)
    ↓
processor/KogotProcessor (KSP)
    ↓
codegen/ → kotlin-native/api/build/generated/
    ↓
kotlin-native/binding/ (runtime registration)
    ↓
kotlin-native/ffi/ (GDExtension FFI)
    ↓
Godot Engine (C++)
```

## Key Gaps to Address

1. **Signals 1-22**: Blocks event-driven architecture
2. **GD.load safe cast**: Blocks resource loading patterns
3. **Dictionary generic**: Blocks typed dictionary patterns
4. **Java FFM**: Blocks JVM interop layer

## References

- Roadmap docs: `docs/roadmap-22feb.md`, `docs/summary-6march.md`
- Signal design: `docs/signal-design.md`
- Philosophy: `docs/kogot_binding_philosophy.md`
- godot-rust reference: Array<T> with Element trait bound, GodotShape enum