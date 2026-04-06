# Draft: Signals for Kotlin

Propuesta de diseño con:

* ✅ Tipado fuerte (`Signal1<P1>`, `Signal2<P1, P2>`, …)
* ✅ Idiomático Kotlin
* ✅ Compatible con GDExtension (registro explícito)
* ✅ Extensible a interfaces (muy importante para tu caso)

---

# 🧠 1. Problema real (bien definido)

Necesitas mapear esto:

```cpp
add_signal("vida_cambio", [("nueva_vida", INT)])
```

A algo como:

```kotlin
val vidaCambio: Signal1<Int> = signal("vida_cambio", param<Int>("nueva_vida"))
```

👉 El problema:

* Kotlin sabe el tipo (`Int`)
* Godot **requiere nombre + tipo**

---

# 🧱 2. Núcleo del diseño

## 🔹 Interfaz base

```kotlin
interface Signal {
    val name: String
    fun register(owner: GodotClassBuilder)
}
```

---

## 🔹 Interfaces tipadas

```kotlin
class Signal0 : Signal {
    fun emit()
    fun connect(callback: () -> Unit)
}

class Signal1<P1> : Signal {
    fun emit(p1: P1)
    fun connect(callback: (P1) -> Unit)
}

class Signal2<P1, P2> : Signal {
    fun emit(p1: P1, p2: P2)
    fun connect(callback: (P1, P2) -> Unit)
}
```

👉 Esto te permite:

* compartir signals entre clases
* usar interfaces como contrato

---

# 🧩 3. DSL para nombres de parámetros

Aquí está la clave.

## 🔹 Definición de parámetros

```kotlin
class SignalParamDescriptor<Type>(kClass: KClass<Type>, name: String)

inline fun <reified Type> param(name: String): SignalParamDescriptor<Type> =
  SignalParamDescriptor(Type::class, name)

```

---

## 🔹 Ejemplo de uso

```kotlin
val vidaCambio = signal<Int>("vida_cambio", param("nueva_vida"))
```

---

# 🏗️ 4. Top-level factory functions

Aquí aplicas el patrón estilo `FunctionN`.

---

## 🔹 Signal0

```kotlin
fun signal(name: String): Signal0 = Signal0(name)
```

---

## 🔹 Signal1

```kotlin
fun <P1> signal(name: String, param: ParamDescriptor<P1>): Signal1<P1> = Signal1(name, param)
```

---

## 🔹 Signal2

```kotlin
fun <P1, P2> signal(
    name: String,
    param1: ParamDescriptor<P1>,
    param2: ParamDescriptor<P2>,
): Signal2<P1, P2> {
    return Signal2(name, param1, param2)
}
```

---

# ⚙️ 5. Implementación interna

Ejemplo para `Signal1`:

```kotlin
class Signal1<P1>(
    override val name: String,
    private val param1: ParamDescriptor<P1>,
) {

    private lateinit var owner: GodotObject

    override fun register(owner: GodotClassBuilder) {
        owner.addSignal(
            name,
            listOf(param1.name to param1.kClass) // conceptual
        )
    }

    override fun emit(p1: P1) {
        owner.emitSignal(name, p1.asVariant())
    }

    override fun connect(callback: (P1) -> Unit) {
        val callable = createCallable { args -> // conceptual
            callback(args[0] as P1)
        }
        owner.connect(name, callable)
    }
}
```

# ⚠️ 6. Validaciones clave (muy importantes)

Debes validar:

### ✔ Nombres únicos

```kotlin
require(params.toSet().size == params.size)
```

---

### ✔ Orden consistente

El orden del DSL = orden en Godot


## 🧠 Cosas IMPORTANTES que debes considerar

### 🔹 1. Signals son strings en Godot

* Usa constantes internas o hashing opcional

---

### 🔹 2. Orden importa

* No puedes cambiarlo después del registro

---

### 🔹 3. Registro ocurre antes de instancias

* Necesitas fase tipo `ClassBuilder`

---

### 🔹 4. Threading

* Signals pueden emitirse fuera del main thread (cuidado)

---

### 🔹 5. Desconexión

Añade:

```kotlin
fun disconnect(callback: ...)
```

---

### 🔹 6. Weak references

Evita leaks en callbacks

---

# 🧪 7. Diseño final resumido

Tu sistema tendría:

### ✔ Interfaces

* `Signal0..SignalN` hasta 22 parametros tipados, de resto `SignalN` con Array, similar a `FunctionN` de kotlin

### ✔ DSL de parámetros

* `param("name")`

### ✔ Top-level factories

* `signal(...)`

---

# 🧩 8. TL;DR

Tu diseño correcto es:

* Modelar como `FunctionN` ✔
* DSL para nombres ✔
* Interfaces para compartir ✔
* Factory functions ✔

👉 La única pieza crítica es:
**cómo capturas nombres + tipos en runtime → DSL + reified/KClass**
