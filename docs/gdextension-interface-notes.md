# 📘 Guía simplificada: Mapeo Kotlin ↔ Godot (GDExtension API)

## 🧠 Idea clave

El archivo:

* `gdextension_interface.json` = **API C de bajo nivel**
* Define **tipos C + funciones C**
* Kotlin NO habla C directamente → necesitas un puente (JNI/JNA/JExtract/Rust/C++)

Por tanto, siempre hay 3 capas:

```
Godot Engine (C API)
        ↑
   GDExtension C ABI
        ↑
   Binding nativo (JNI/JNA/etc.)
        ↑
     Kotlin API bonita
```

---

# 🧩 Tipos base (C → Kotlin)

Estos tipos NO aparecen en el JSON pero existen implícitamente.

## 🔢 Primitivos

| C type   | Kotlin equivalente | Notas                |
|----------|--------------------|----------------------|
| int8_t   | Byte               | firmado              |
| uint8_t  | Byte / UByte       | preferible UByte     |
| int16_t  | Short              |                      |
| uint16_t | UShort             |                      |
| int32_t  | Int                |                      |
| uint32_t | UInt               |                      |
| int64_t  | Long               |                      |
| uint64_t | ULong              |                      |
| float    | Float              |                      |
| double   | Double             |                      |
| char     | Byte               |                      |
| size_t   | Long (64-bit)      | depende arquitectura |
| void     | Unit               |                      |

👉 Recomendación:

* Usa tipos sin signo (`UInt`, `ULong`) si tu binding los soporta
* Si usas JNI puro → probablemente `Int` / `Long`

---

# 🧱 Tipos del JSON y cómo mapearlos

El JSON define 5 tipos:

```
enum
handle
alias
struct
function
```

---

# 🏷️ 1. ENUM

## En C

* Siempre 32 bits
* Puede ser bitfield

## Regla de mapeo

### Normal enum

```
C: int32_t
Kotlin: enum class + Int backing
```

```kotlin
enum class InitializationLevel(val value: Int) {
    CORE(0),
    SERVERS(1),
    SCENE(2),
    EDITOR(3)
}
```

---

### Bitfield enum

```
C: uint32_t
Kotlin: flags con UInt
```

```kotlin
@JvmInline
value class Flags(val value: UInt)
```

o estilo Android:

```kotlin
object Flags {
    const val A = 1u
    const val B = 2u
}
```

---

# 🧷 2. HANDLE (puntero opaco)

## Qué es

```
void* a algo interno de Godot
```

Ejemplo:

```
GDExtensionStringNamePtr
```

## Regla de oro

👉 En Kotlin es SIEMPRE un puntero nativo

## Mapeo recomendado

```
C pointer → Long
```

```kotlin
@JvmInline
value class GodotHandle(val ptr: Long)
```

---

## Variantes

### Const handle

Solo lectura → misma representación

### Uninitialized handle

Memoria no inicializada → se usa para constructores

👉 Kotlin no necesita diferenciar, pero puedes tiparlo:

```kotlin
value class UninitializedHandle(val ptr: Long)
```

---

# 🧬 3. ALIAS

Es un typedef.

## Ejemplo

```
GDExtensionInt → int64_t
```

## En Kotlin

Solo usa el tipo base.

Opcional:

```kotlin
typealias GDExtensionInt = Long
```

---

# 🧱 4. STRUCT

## Regla importante

Debe respetar:

* Layout
* Orden
* Padding C

⚠️ Kotlin puro NO puede garantizar layout C.

Necesitas:

* JNI struct
* JNA Structure
* Panama MemorySegment
* O código C intermedio

---

## Representación lógica en Kotlin

```kotlin
data class CallError(
    val error: Int,
    val argument: Int,
    val expected: Int
)
```

Pero internamente debes convertir a memoria nativa.

---

# 🔧 5. FUNCTION TYPES (punteros a función)

Son firmas de funciones C.

Ejemplo:

```
typedef void (*Constructor)(TypePtr base, const TypePtr* args)
```

## Kotlin

Depende del binding:

### JNI

Necesitas método nativo

### Panama/JExtract

Se mapea a `MethodHandle`

### Wrapper manual

```kotlin
typealias Constructor = (Long, Long) -> Unit
```

---

# 🧠 Modificadores de tipo

## Punteros

```
Type*
```

→ Siempre puntero

Kotlin:

```
Long
```

---

## Puntero const

```
const Type*
```

→ Igual que puntero

(No hay const en JVM)

---

## Doble puntero

```
Type**
```

→ puntero a array de punteros

Kotlin:

```
Long
```

y manejas memoria manualmente

---

# 🧰 Interface Functions

Estas son funciones reales que Godot expone.

Se obtienen con:

```
GDExtensionInterfaceGetProcAddress("function_name")
```

---

## Ejemplo conceptual Kotlin

```kotlin
external fun getGodotVersion(ptr: Long)
```

o wrapper:

```kotlin
fun getGodotVersion(): GodotVersion {
    val struct = allocateVersionStruct()
    nativeGetGodotVersion(struct.ptr)
    return struct.toKotlin()
}
```

---

# 🗺️ Mapa mental completo

## Qué representa cada cosa en Kotlin

```
C primitive     → Kotlin primitive
Enum            → enum class / UInt flags
Handle          → Long (pointer wrapper)
Alias           → typealias
Struct          → data class + native memory
Function ptr    → external/native function
```

---

# 🧪 Ejemplo completo

## JSON

```
handle: GDExtensionStringNamePtr
```

## Kotlin

```kotlin
@JvmInline
value class StringNamePtr(val ptr: Long)
```

---

## JSON

```
struct GDExtensionCallError
```

## Kotlin

```kotlin
data class CallError(
    val error: Int,
    val argument: Int,
    val expected: Int
)
```

---

## JSON

```
alias GDExtensionInt = int64_t
```

## Kotlin

```kotlin
typealias GDExtensionInt = Long
```

---

# ⚠️ Cosas IMPORTANTES que debes saber

## 🔥 Kotlin/JVM NO puede:

* Manejar memoria C directamente
* Garantizar layout de struct
* Usar punteros reales

Necesitas:

* JNI
* Panama (Java 22+)
* JNA
* O capa C/C++

---

# 🧭 Recomendación para bindings Kotlin ↔ Godot

## Diseño ideal

### Nivel bajo

Auto-generado desde JSON:

```
NativeTypes.kt
NativeFunctions.kt
NativeStructs.kt
```

### Nivel medio

Wrappers seguros:

```
StringName
Variant
Node
Object
```

### Nivel alto

API idiomática Kotlin estilo Godot C#
