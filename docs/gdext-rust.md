**Análisis de Arquitectura de Binding Layer**

_Godot-Rust/GDExt_

# 1\. Introducción a la Arquitectura

Godot-rust (gdext) es un binding de Rust para Godot 4 que utiliza el **GDExtension API** (la interfaz C que proporciona Godot para integraciones de terceros). La arquitectura se divide en tres componentes principales:

- FFI Layer (godot-ffi): Capa C pura que maneja llamadas FFI
- API Layer (godot-codegen): Código Rust generado a partir del JSON de Godot
- High-level API Framework (godot-core): Marco que envuelve y abstrae el código generado

# 2\. Flujo de Generación de Código

## 2.1 Origen: extension_api.json

El proceso comienza con el archivo **extension_api.json** que Godot 4.x genera. Este JSON contiene:

- Definiciones de clases (propiedades, métodos)
- Tipos built-in (Vector3, Color, Transform3D, etc.)
- Firmas de funciones globales
- Metadatos de tipos (tamaños, offsets, alineación)

## 2.2 Generación de Código en Tiempo de Compilación

El proceso ocurre mediante build scripts de Cargo que ejecutan **godot-codegen**:

Read: GODOT4_GDEXTENSION_JSON → extension_api.json

Parse: Deserializar JSON en estructuras Rust

Generate: Generar código para FFI, native structs y class wrappers

Write: Exportar a src/gen/classes.rs, src/gen/builtin.rs, etc.

# 3\. Arquitectura del Binding Layer

## 3.1 FFI Layer (godot-ffi)

Es la capa más baja y pura. Contiene funciones FFI raw que comunican directamente con Godot. Componentes clave:

**GodotFFI struct:** Contiene todos los punteros a funciones del API de Godot

**method_tables:** Tablas que mapean (ClassName, MethodName) → FunctionPointer

**ClassSceneMethodTable::load():** Precarga TODOS los punteros al iniciar

### 3.1.1 Estrategia de Precarga de Punteros

La decisión de gdext es precargarlo TODO al iniciar, en lugar de lazy loading. Ventajas: acceso rápido (O(1)), detecta incompatibilidades inmediatamente. Desventajas: consume más RAM, rompe si API cambia incluso en métodos no usados.

## 3.2 Generación de Estructuras Native

Para tipos built-in (Vector3, Transform3D, etc.), gdext genera structs Rust que mapean exactamente al layout de C:

_#\[repr(C)\]_ garantiza que el layout es idéntico al C y que los offsets corresponden exactamente a lo especificado en el JSON de Godot.

### 3.2.1 ¿Cómo se Conocen los Offsets?

El JSON de Godot incluye explícitamente los offsets de cada miembro. El generador lee estos valores y construye structs Rust con los campos en el orden exacto, validando tamaños en tiempo de compilación.

# 4\. Cómo se Conectan API y FFI Layer

## 4.1 Patrón de Generación

Cada método generado sigue un patrón directo: _JSON definition → Generated Rust function → Direct FFI call_. No hay código manual de adaptación.

## 4.2 ¿Hay un Adapter Layer Manual?

**NO.** Todo es automáticamente generado del JSON. El modelo es 1:1:

- Entrada JSON de Godot
- Generador produce función Rust segura
- Función contiene conversión de tipos + FFI call directo

### 4.3 Directness: API → FFI

En el nivel más bajo, es prácticamente directo: Firma Rust → _unsafe { godot_method_bind_call(ptr, ...) }_ → Llamada C. No hay intermediate utility layer.

# 5\. Manejo de Offsets y Sizes de Memoria

## 5.1 Fuente: extension_api.json

Godot proporciona explícitamente para cada tipo built-in:

| Campo | Tipo  | Offset | Alineación |
|-------|-------|--------|------------|
| x     | float | 0      | 4          |
| y     | float | 4      | 4          |
| z     | float | 8      | 4          |

## 5.2 Native Structs con #\[repr(C)\]

El generador produce structs con _#\[repr(C)\]_, garantizando que el layout de memoria sea idéntico al C y que los offsets correspondan exactamente. Valida tamaños y alineación en compilación.

## 5.3 Accessors y Raw Pointers

Para estructuras complejas, gdext genera accessors seguros que encapsulan el acceso por offset. Ejemplo para Transform3D::origin (offset 36 bytes):

# \[inline\]
pub fn get_origin(&self) -> Vector3 {
unsafe {
let ptr = (self as \*const_ as \*const u8).add(36);
\*(ptr as \*const Vector3)
}
}

# 6\. Ejemplo Concreto: Node3D::set_position()

**Paso 1: JSON de Godot**

Godot especifica que Node3D tiene un método set_position que acepta Vector3, con hash 1234567890.

**Paso 2: Codegen Genera**

Crea una función Rust segura con conversión automática de tipos y FFI call directo.

**Paso 3: Precarga**

Al iniciar gdext, _ClassSceneMethodTable::load()_ obtiene el puntero a la función real desde Godot.

**Paso 4: Llamada del Usuario**

node.set_position(Vector3::new(10.0, 20.0, 30.0));

**Paso 5: Ejecución**

Node3D::set_position() → godot_method_bind_call() → \[FFI a C\] → Ejecución en C++ de Godot

# 7\. Estructura de Archivos Clave

El repositorio tiene estos componentes principales:

**godot-ffi/** \- FFI bindings raw C

**godot-codegen/** \- Generador de código

**godot-core/** \- API de alto nivel + código generado

## 7.1 Archivos Generados vs. Manuales

**GENERADOS automáticamente:**

- src/gen/classes.rs - Todas las clases de Godot
- src/gen/builtin.rs - Todos los tipos built-in
- src/gen/global.rs - Funciones globales
- src/gen/method_tables.rs - Tablas de punteros FFI

**MANUALES (escritos a mano):**

- src/obj/gd.rs - Smart pointer Gd&lt;T&gt;
- src/obj/native.rs - Traits base
- src/meta/traits.rs - ToGodot, FromGodot

# 8\. Caracterización de Offsets en Native Structs

## 8.1 Obtención Automática del JSON

El generador lee cada tipo built-in del JSON, extrae los offsets de sus miembros, y produce una struct Rust con los campos en orden exacto, utilizando _#\[repr(C)\]_ para garantizar el layout correcto.

## 8.2 Validación en Compilación

Gdext valida que los offsets sean correctos usando _const_assert_eq!_ para verificar que size_of y align_of coincidan con el JSON.

# 9\. Características Clave de Memoria

## 9.1 Copy-on-Write para Strings

GString usa copy-on-write: clone() es barato hasta que se modifica. Solo en ese momento se copia realmente.

## 9.2 Reference Counting para RefCounted

Gd&lt;RefCounted&gt; incrementa refcount en clone(), decrementa en drop(). Última referencia dealoca el objeto.

## 9.3 Manual Memory Management para Node

Node NO es RefCounted. Requiere queue_free() para eliminación. Rust no puede automatizar esto.

# 10\. Conclusiones

Resumen de puntos clave:

**1\. Generación Automática:** TODO el código API se genera del JSON de Godot, sin código manual de adaptación

**2\. Directness:** API → FFI es casi directo, cada método generado es un wrapper mínimo

**3\. Offsets Precargados:** Vienen del JSON de Godot, validados en compilación

**4\. Precarga de Punteros:** Todos los method pointers se cargan al iniciar, no lazy

**5\. Arquitectura de 3 Capas:** FFI raw → API generada → Framework manual

**6\. Consistencia Garantizada:** El código es generado mecánicamente del JSON
