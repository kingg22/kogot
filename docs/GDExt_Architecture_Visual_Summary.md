# Godot-Rust/GDExt - Resumen Visual de Arquitectura

## Flujo de Compilación y Ejecución

```
┌─────────────────────────────────────────────────────────────────┐
│                    Godot Engine (C++)                           │
│         Exporta: extension_api.json (especificación)            │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              ↓
                    ┌─────────────────────┐
                    │  extension_api.json │
                    │  (JSON specification)│
                    └──────────┬──────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ↓                             ↓
        ┌──────────────┐          ┌──────────────────┐
        │  godot-ffi   │          │ godot-codegen    │
        │  (FFI layer) │          │ (Code generator) │
        └──────────────┘          └────────┬─────────┘
                │                          │
                │ Parsea JSON              │ Lee JSON
                │                          │
                ├──────────────────────────┘
                │
                ↓
        ┌─────────────────────────────────────┐
        │    Tiempo de Compilación:           │
        │    - Genera godot::gen::classes.rs  │
        │    - Genera godot::gen::builtin.rs  │
        │    - Genera method_tables.rs        │
        │    - Genera conversión de tipos     │
        └────────────┬────────────────────────┘
                     │
                     ↓
        ┌─────────────────────────────────────┐
        │   Runtime Initialization:           │
        │   ClassSceneMethodTable::load()     │
        │   Precarga todos los punteros FFI   │
        │   (hacia C de Godot)                │
        └────────────┬────────────────────────┘
                     │
                     ↓
        ┌─────────────────────────────────────┐
        │   Ejecución del Juego               │
        │   node.set_position(Vector3::new()) │
        │   → FFI call → C++ de Godot         │
        └─────────────────────────────────────┘
```

---

## Arquitectura de 3 Capas

```
┌─────────────────────────────────────────────────────────────────┐
│  CAPA 3: User Code (tu juego en Rust)                           │
│                                                                 │
│  let node: Gd<Node3D> = ...;                                   │
│  node.set_position(Vector3::new(10.0, 20.0, 30.0));            │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│  CAPA 2: API Layer GENERADA (godot::classes::Node3D)            │
│                                                                 │
│  pub fn set_position(&mut self, position: Vector3) {           │
│      unsafe {                                                   │
│          // 1. Lookup puntero (precargado)                      │
│          let method_bind = METHOD_TABLE.node3d_set_position;   │
│          // 2. Convertir Rust → FFI representation              │
│          let args = [Variant::from(position)];                 │
│          // 3. Llamada FFI pura                                 │
│          godot_method_bind_call(method_bind, self_ptr, ...)    │
│      }                                                          │
│  }                                                              │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│  CAPA 1: FFI Layer (godot-ffi)                                  │
│                                                                 │
│  extern "C" {                                                   │
│      fn godot_method_bind_call(                                 │
│          p_method_bind: *mut GDExtensionMethodBind,            │
│          p_instance: *mut GDExtensionClassInstancePtr,         │
│          p_args: *const *const GDExtensionVariantPtr,          │
│          p_arg_count: i32,                                      │
│          r_return: *mut GDExtensionVariantPtr,                 │
│          r_error: *mut GDExtensionCallError,                   │
│      ) -> ();                                                   │
│  }                                                              │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ↓
                  ┌──────────────┐
                  │  GDExtension │
                  │  C Interface │
                  │  (Godot 4.x) │
                  └──────────────┘
```

---

## Generación de Métodos: De JSON a FFI

```
JSON Input (extension_api.json)
├─ class: "Node3D"
├─ method: "set_position"
├─ return_type: "void"
├─ hash: 1234567890
└─ arguments:
   └─ name: "position", type: "Vector3"

         ↓ [godot-codegen parsea]

Código Rust Generado
┌──────────────────────────────────────┐
│ impl Node3D {                        │
│   pub fn set_position(               │
│       &mut self,                     │
│       position: Vector3              │
│   ) {                                │
│       unsafe {                       │
│           // FFI call directo        │
│           godot_method_bind_call(    │
│               METHOD_TABLE           │
│                 .node3d_set_position,│
│               self.opaque_ptr(),     │
│               &args,                 │
│               1,                     │
│               null_mut(),            │
│               null_mut()             │
│           )                          │
│       }                              │
│   }                                  │
│ }                                    │
└──────────────────────────────────────┘

         ↓ [En runtime, al inicializar]

Precarga de Punteros
classdb_get_method_bind(
    "Node3D",
    "set_position",
    1234567890  ← hash del JSON
) → *mut GDExtensionMethodBind

         ↓ [Usuario llama]

Ejecución
node.set_position(Vector3::new(10, 20, 30))
  → FFI call
    → C++ Godot
```

---

## Manejo de Offsets en Native Structs

### Ejemplo: Vector3

**JSON de Godot:**
```json
{
  "type": "builtin",
  "name": "Vector3",
  "members": [
    { "name": "x", "type": "float", "offset": 0 },
    { "name": "y", "type": "float", "offset": 4 },
    { "name": "z", "type": "float", "offset": 8 }
  ],
  "size": 12,
  "alignment": 4
}
```

**Codegen genera:**
```rust
#[repr(C, align(4))]
pub struct Vector3 {
    pub x: f32,  // offset 0
    pub y: f32,  // offset 4
    pub z: f32,  // offset 8
}

// Validaciones en compilación
const_assert_eq!(size_of::<Vector3>(), 12);
const_assert_eq!(align_of::<Vector3>(), 4);
const_assert_eq!(offset_of!(Vector3, x), 0);
const_assert_eq!(offset_of!(Vector3, y), 4);
const_assert_eq!(offset_of!(Vector3, z), 8);
```

**Layout en Memoria:**
```
Vector3 @ 0x1000 [12 bytes]
┌────────────┬────────────┬────────────┐
│   x (f32)  │   y (f32)  │   z (f32)  │
├────────────┼────────────┼────────────┤
│ 0x1000-004 │ 0x1004-008 │ 0x1008-012 │
└────────────┴────────────┴────────────┘
```

---

## Flujo Completo: Un Método

```
Usuario escribe:
    node.set_position(Vector3::new(10, 20, 30))

    ↓

Rust resuelve sobrecarga:
    Node3D::set_position(&mut self, position: Vector3)

    ↓

El método GENERADO ejecuta:
    1. Busca METHOD_TABLE.node3d_set_position
       (Ya cargado en init, es un raw *mut GDExtensionMethodBind)
    
    2. Convierte argumentos:
       Vector3 → Variant
       (usa ToGodot trait)
    
    3. Prepara stack FFI:
       [puntero_método, self_ptr, args, arg_count, return_ptr, error_ptr]
    
    4. Llama extern "C" godot_method_bind_call(...)
    
    5. Godot C++ ejecuta Node3D::set_position(Vector3)
    
    6. Retorna (void, sin retorno)

    ↓

Retorna a Rust
```

---

## Arquitectura de Archivos

```
gdext/
│
├── godot-ffi/                    ← Capa 1: FFI Raw
│   ├── src/
│   │   ├── lib.rs               # GodotFFI interface
│   │   ├── method_tables.rs      # Precarga punteros
│   │   └── interface.rs          # GDExtensionInterface bindings
│   └── Cargo.toml
│
├── godot-codegen/                ← Generador
│   ├── src/
│   │   ├── main.rs              # Entry point
│   │   ├── class_gen.rs         # Genera clases.rs
│   │   ├── builtin_gen.rs       # Genera tipos built-in
│   │   └── method_gen.rs        # Genera métodos
│   └── Cargo.toml
│
├── godot-core/                   ← Capa 2 + 3: API + Framework
│   ├── src/
│   │   ├── gen/                 # [GENERADO]
│   │   │   ├── classes.rs       # Todas las clases ← GENERADO
│   │   │   ├── builtin.rs       # Vector3, etc. ← GENERADO
│   │   │   ├── global.rs        # Funciones globales ← GENERADO
│   │   │   └── method_tables.rs # Tablas punteros ← GENERADO
│   │   │
│   │   ├── builtin/             # Wrappers MANUALES
│   │   │   ├── vector3.rs       # Vector3 helpers (pocos)
│   │   │   └── transform.rs     # Transform3D helpers
│   │   │
│   │   ├── obj/                 # Framework MANUAL
│   │   │   ├── gd.rs            # Smart pointer Gd<T>
│   │   │   ├── base.rs          # Base<T> para herencia
│   │   │   └── native.rs        # Traits para objetos nativos
│   │   │
│   │   ├── meta/                # Framework MANUAL
│   │   │   ├── traits.rs        # ToGodot, FromGodot
│   │   │   ├── signature.rs     # Firma de métodos
│   │   │   └── varargs.rs       # Varargs handling
│   │   │
│   │   └── lib.rs               # Reexportaciones públicas
│   │
│   └── Cargo.toml
│
└── build.rs                      # Script que ejecuta codegen
```

---

## Secuencia de Inicialización

```
1. Godot carga la DLL/SO de gdext
   │
   └─→ Llama gdextension_init()

2. gdext inicializa:
   ├─ Lee GDExtensionInterface
   ├─ Crea GodotFFI struct
   └─ Llama ClassSceneMethodTable::load()

3. ClassSceneMethodTable::load():
   │
   └─ Para cada clase en extension_api.json:
      └─ Para cada método en esa clase:
         └─ classdb_get_method_bind(class, method, hash)
            └─ Almacena puntero en tabla

4. Extension lista para usar
   └─ Todos los method pointers están precargados
      y validados

5. Usuario código llama métodos
   ├─ node.set_position(...)
   ├─ Lookup en METHOD_TABLE (O(1))
   ├─ FFI call
   └─ Ejecución en C++
```

---

## Características de Memoria

### Copy-on-Write (GString)
```
let s1 = GString::from("hello");
let s2 = s1.clone();  ← Cheap, no copy yet

s2.push_str("!");     ← Now copies, s1 remains unchanged
```

### Reference Counting (Gd<RefCounted>)
```
let r1: Gd<RefCounted> = create_resource();
{
    let r2 = r1.clone();  ← Increments refcount
}                         ← r2 drops, refcount decreases

// Si refcount llega a 0, Godot dealoca
```

### Manual Management (Gd<Node>)
```
let node: Gd<Node> = Node::new_alloc();
// Usar...
node.queue_free();  ← Marca para eliminación en próximo frame

// NO se dealoca automáticamente, Rust no puede automatizar esto
```

---

## Validación de Seguridad

### En Compilación:
```
✓ #[repr(C)] garantiza layouts correctos
✓ const_assert! valida offsets y tamaños
✓ Macros de generación validan conversiones de tipos
```

### En Runtime:
```
✓ ClassSceneMethodTable::load() valida punteros
✓ Gd<T> detecta borrow violations (con safeguard level)
✓ ToGodot/FromGodot validan conversiones
```

### Safeguard Levels:
```
strict (default):
  ✓ Costoso, mucha validación
  ✓ Detecta casi todos los bugs

balanced (recomendado):
  ◐ Balance entre seguridad y rendimiento
  ◐ Detecta la mayoría de bugs

disengaged (solo release, si es necesario):
  ✗ Mínimas validaciones, riesgo de UB
  ✗ Solo si profiling demuestra necesidad
```

---

## Conclusión: Flujo Simplificado

```
JSON (Godot)
    ↓
Codegen (Build time)
    ↓
Rust Code Generated
    ↓
Compile (Validates offsets)
    ↓
Runtime Init (Precarga FFI pointers)
    ↓
User Code
    ↓
Method Call
    ↓
FFI → C++ Godot
```

**Puntos clave:**
- TODO automático del JSON
- Directness máximo: cada método es wrapper mínimo
- Seguridad en compilación y runtime
- No hay adaptadores manuales
- Offsets vienen del JSON, validados en compilación
