---
name: external-bindings-reference
description: Reference patterns from other Godot bindings (godot-rust, godot-cpp, GraphicsGD). Use when implementing Array<T>, Dictionary<K,V>, signals, virtual methods, FFI binding, or memory ownership in kogot. Trigger on: new binding feature, pattern lookup, comparing approaches.
---

# External Bindings Reference - kogot

How to explore and reference patterns from other Godot bindings.

## godot-rust (Rust)

**Source**: `https://github.com/godot-rust/gdext`

**Local**: `/home/kingg22/IdeaProjects/godot-rust`

**Key patterns to reference:**

| Pattern                | Location                                                    |
|------------------------|-------------------------------------------------------------|
| Array<T>               | `godot-core/src/collections/array.rs` - Element trait bound |
| GodotShape enum        | Distinguishes TypedArray vs Builtin                         |
| AnyArray/AnyDictionary | Type erasure for variant handling                           |
| PhantomData            | Compile-time safety without layout impact                   |

**Explore:**
Local:
```bash
cd /home/kingg22/IdeaProjects/godot-rust && ls -la
```

Missing local:
```bash
git clone https://github.com/godot-rust/gdext.git
cd gdext && ls -la
```

## godot-cpp (C++)

**Source**: Built into Godot repository at `modules/godotcpp/`

**Key patterns to reference:**

| Pattern            | Location                                 |
|--------------------|------------------------------------------|
| ClassDB binding    | `bind_method_functions.cpp`              |
| MethodBind         | Generated via CLASSDB_* macros           |
| Variant conversion | `variant.cpp` and `variant_internal.cpp` |

**Explore:**
```bash
# Inside Godot source tree
cd /home/kingg22/RiderProjects/godot
ls modules/godotcpp/bindings/
cat modules/godotcpp/bind_method_functions.cpp
```

## GraphicsGD (Go)

**Source**: `https://github.com/TreyAnto/GraphicsGD`
**Local**: `/home/kingg22/VisualStudioCodeProjects/graphics.gd`

**Key patterns to reference:**

| Pattern      | What it shows                  |
|--------------|--------------------------------|
| Contains[T]  | Generic container pattern      |
| Proxy[T]     | Storage abstraction interface  |
| IsTyped[T]() | Reflection-based type checking |

**Explore:**
Local:
```bash
cd /home/kingg22/VisualStudioCodeProjects/graphics.gd
```

Missing local:
```bash
git clone https://github.com/TreyAnto/GraphicsGD.git
cd GraphicsGD
```

## SwiftGodot (Swift)

**Source**: `https://github.com/migueldeicaza/SwiftGodot`
**Local**: `/home/kingg22/IdeaProjects/SwiftGodot`

**Explore:**
Local:
```bash
cd /home/kingg22/IdeaProjects/SwiftGodot
```

Missing local:
```bash
git clone https://github.com/migueldeicaza/SwiftGodot.git
cd SwiftGodot
```

## When to Use Each Reference

| Task                      | Best Reference                  |
|---------------------------|---------------------------------|
| Array<T> generic handling | godot-rust (Element trait)      |
| Dictionary<K,V> generic   | godot-rust OR GraphicsGD        |
| Signal emission           | godot-rust (sig::Emitter)       |
| Virtual methods           | godot-rust (derive::GodotClass) |
| FFI binding layer         | godot-cpp (CLASSDB macros)      |
| Memory ownership          | godot-rust (Unique, Shared)     |

## Key Insight from godot-rust

The `GodotShape` enum distinguishes between:
- **TypedArray**: Array with element type (e.g., Array<Int>)
- **Builtin**: Plain Godot builtin (GodotArray, GodotDictionary)

This helps understand how to model kogot's `ArrayGenericConfig` for typed arrays.

## Checking for Updates

External bindings evolve. Periodically check:
- godot-rust: `git log --oneline -5` and new release tags
- GraphicsGD: Check for new generic patterns in `proxy.go`
