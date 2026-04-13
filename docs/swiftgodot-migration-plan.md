# Plan: Migrating SwiftGodot Patterns to Kogot

## Context

Kogot es un binding de Kotlin Native a Godot GDExtension. Inspirado en SwiftGodot, necesita implementar sistemas similares de casting, strings, callables y signals. El objetivo es replicar la funcionalidad de forma idiomática a Kotlin, no copiar literalmente el código.

## Issues Conocidos

### 1. Casting fallido (SpriteBench.kt línea 33, 64)
```kotlin
// PROBLEMA: Crea Texture2D sin verificar que el objeto Godot real es Texture2D
val icon = Texture2D(ResourceLoader.instance.load("res://icon.svg".asGodotString()).rawPtr)
```

### 2. GodotString API sucia
```kotlin
// ACTUAL: Necesita conversiones explícitas
GD.print("hello".asGodotString())

// DESEADO: API transparente con kotlin.String
GD.print("hello")
```

### 3. Callable placeholder (Utils.kt línea 22-25)
```kotlin
// Currently returns empty Callable()
internal inline fun createCallable(callback: (Array<Any?>) -> R): Callable {
    return Callable()  // PLACEHOLDER - no funciona
}
```

### 4. Signal connect/disconnect no implementan callable real

---

## SwiftGodot Reference Findings

### Casting System (Wrapped.swift, getOrInitSwiftObject)
- **Identity table**: `liveFrameworkObjects[handle] = reference` - mapa de puntero a instancia
- **getOrInitSwiftObject**: Busca handle en tabla, si existe retorna instancia existente, si no crea nueva del tipo correcto
- Todos los objetos Godot se mapean via `handle: UnsafeMutableRawPointer`
- Jerarquía: `Wrapped → Object → Resource → Texture2D`

### String Conversion (StringExtensions.swift)
- `GDExtensionStringPtr` = opaque pointer a string Godot
- Conversiones van directo a FFI: `gi.string_new_with_utf8_chars`, `gi.string_to_utf8_chars`

### Callable System (FastFunctionBridging.swift)
- `BridgedFunction = (UnsafeRawPointer?, Arguments) -> FastVariant?`
- Registration via `gi.classdb_register_extension_class_method`

---

## Implementation Plan

### Phase 1: Object Identity System (Casting)

**Archivos a modificar/crear:**
1. `kotlin-native/api/src/nativeMain/kotlin/io/github/kingg22/godot/internal/ObjectRegistry.kt` - Nueva clase

**Implementación:**
```kotlin
// ObjectRegistry.kt - identity table como SwiftGodot
object ObjectRegistry {
    private val liveObjects = mutableMapOf<COpaquePointer, WeakReference<GodotObject>>()

    fun register(obj: GodotObject) {
        liveObjects[obj.rawPtr] = WeakReference(obj)
    }

    fun getOrCreate<T : GodotObject>(handle: COpaquePointer, type: KClass<T>): T {
        // 1. Check existing wrapper
        liveObjects[handle]?.get()?.let { return it as T }

        // 2. Get actual Godot class name via FFI
        val actualClassName = getGodotClassName(handle)

        // 3. Verify type compatibility or throw
        if (!isAssignableFrom(type, actualClassName)) {
            throw ClassCastException("Godot object is $actualClassName, not ${type.simpleName}")
        }

        // 4. Create new wrapper of correct type
        return createWrapper(type, handle).also { register(it) }
    }
}
```

**Test:**
- SpriteBench línea 33 debería funcionar: `Texture2D(ResourceLoader.load(path).rawPtr)`
- Verificar que si Godot retorna `Image` pero se castea a `Texture2D` → error claro

---

### Phase 2: String Internalization

**Archivos a modificar:**
1. `kotlin-native/api/src/nativeMain/kotlin/io/github/kingg22/godot/api/builtin/Utils.kt`

**Cambios:**
```kotlin
// OLD
public fun String.asGodotString(): GodotString = GodotString(this)

// NEW: Implicit conversions via extension functions
// Los métodos que esperan GodotString aceptarán String via estas extensiones

// Plus: hacer GodotString internal, no exponer en API publica
@InternalApi
class GodotString internal constructor(...) { ... }

// Extension que hace String -> GodotString transparent
internal fun String.toGodotString(): GodotString = GodotString(this)
```

**Estrategia:**
- Mantener `asGodotString()` para código interno
- En API pública, permitir `String` directamente donde se espera `GodotString`
- GodotString manejo interno, no leak a usuario

---

### Phase 3: Callable System

**Archivos a crear:**
1. `kotlin-native/api/src/nativeMain/kotlin/io/github/kingg22/godot/internal/callback/CallbackTrampoline.kt`
2. `kotlin-native/api/src/nativeMain/kotlin/io/github/kingg22/godot/internal/callback/CallableFactory.kt`

**Implementación inspirada en SwiftGodot FastFunctionBridging:**

```kotlin
// CallbackTrampoline.kt
@CFunction
fun signalCallbackTrampoline(
    pInstance: COpaquePointer?,
    args: CPointer<CPointerRef>?,
    argCount: Int,
    ret: CPointer<GDExtensionVariantPtr>?,
): Unit {
    // 1. Extraer argumentos de Variant[]
    // 2. Buscar closure en global map por instance
    // 3. Invocar closure con args
    // 4. Escribir resultado en ret
}

// CallableFactory.kt
fun <R> createCallable(callback: (Array<Any?>) -> R): Callable {
    val trampoline = staticCallback(callback)  // C function pointer
    val id = registerCallback(trampoline, callback)  // guardar closure

    return Callable.ofMethod(instancePtr, methodName, trampoline, id)
}
```

**Test:**
- CustomSignalClass.connect debería invoking callback real

---

### Phase 4: Signal Binding Completion

**Archivos a modificar:**
1. `kotlin-native/api/src/nativeMain/kotlin/io/github/kingg22/godot/api/signal/Utils.kt` - implementar createCallable real
2. `kotlin-native/api/src/nativeMain/kotlin/io/github/kingg22/godot/api/signal/Signal0.kt` - usar createCallable real
3. `kotlin-native/api/src/nativeMain/kotlin/io/github/kingg22/godot/api/signal/Signal1.kt` - igual

**Implementación:**
```kotlin
// Utils.kt
internal inline fun <R : Any> createCallable(callback: (Array<Any?>) -> R): Callable {
    return CallableFactory.create(callback)  // usar implementation real
}
```

---

## Verification

### Phase 1 Tests:
```bash
cd ~/IdeaProjects/kogot
./gradlew :kotlin-native:sample:linkDebugTestKotlinNative
# Run in Godot, check SpriteBench output
```

### Phase 2 Tests:
```bash
# Test String transparency
GD.print("test")  // should work without .asGodotString()
```

### Phase 3-4 Tests:
```kotlin
// CustomSignalClass.kt
val mySignal = signal("my_signal", param<Int>("value"))

mySignal.connect { value ->
    println("Signal received: $value")  // Should print
}

mySignal.emit(42)  // Should trigger callback
```

---

## Order of Implementation

1. **ObjectRegistry** (Phase 1) - foundation para todo
2. **String internalization** (Phase 2) - cambio simple con alto impacto
3. **Callable trampoline** (Phase 3) - complejo, requiere testing
4. **Signal binding** (Phase 4) -依赖于 Callable

---

## Key Differences from SwiftGodot

| SwiftGodot | Kogot | Notes |
|------------|-------|-------|
| `UnsafeMutableRawPointer` | `COpaquePointer` | Kotlin equivalent |
| `liveFrameworkObjects` | `ObjectRegistry` | Identity map |
| `getOrInitSwiftObject` | `ObjectRegistry.getOrCreate` | Same pattern |
| `_GodotBridgeable` | Extension functions | Kotlin style |
| `@Published` | `@PublishedApi internal` | Kotlin equivalent |
| Swift tuples for variants | Array<Any?> | Simpler but less type-safe |