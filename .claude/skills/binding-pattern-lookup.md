---
name: binding-pattern-lookup
description: Which pattern from godot-rust/gdext or godot-cpp to follow for a given kogot feature. Use when implementing Array<T>, virtual methods, signals, memory ownership, or casting. Trigger on: new feature in kotlin-native/, comparing approaches.
---

# Pattern Lookup

## Array<T> generic
- gdext uses Element trait bound — kogot equivalent: ArrayGenericConfig interceptor (already done)
- PhantomData pattern → kogot uses TypeVariableName("T") in KotlinPoet

## Virtual methods
- gdext: `get_virtual` callback dispatches to `__virtual_call` per class
- kogot: engine class wrappers generate `open` fun; derived override triggers `register_virtual_method`
- KSP vs explicit helper: still unresolved → document this gap, don't invent

## Engine class instantiation
- COpaquePointer always comes FROM Godot (classdb_construct_object, singleton lookup, or callback)
- companion object fun new() preferred over secondary constructors (secondary constructors inherit wrong class name)

## Memory ownership
- Builtins with destructor: NativeOwned contract, not just AutoCloseable
- AutoCloseable = ergonomic sugar only, not semantic contract
- Engine class wrappers: never allocate with nativeHeap — pointer owned by Godot

## Casting / object identity
- COpaquePointer → Kotlin wrapper lookup before creating a new wrapper

## Singleton access
Use `StringName(...).use { }` (high-level AutoCloseable), not direct pointer access.
