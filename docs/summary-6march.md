# Resumen del Proyecto: Godot Kotlin/Native Binding - Estado Actual

## 📋 Contexto General

**Objetivo**: Generar bindings Kotlin/Native para Godot 4.x GDExtension desde `extension_api.json` y
`gdextension_interface.json`

**Arquitectura de módulos**:

- `ffi`: cinterop definitions (generado por cinterop desde .def)
- `api`: código generado (clases, enums, estructuras) - OUTPUT del codegen
- `runtime`: entry point + FFI bindings generados - OUTPUT del codegen
- `codegen`: generador de código (este proyecto)

---

## ✅ Completado Hasta Ahora

### 1. **Code Generation Completo**

**Generadores implementados**:

- ✅ `NativeEnumGenerator` - Enums como `enum class` con `Long value`
- ✅ `NativeVariantGenerator` - `sealed class Variant` con subclases
- ✅ `NativeBuiltinClassGenerator` - Builtin classes (Vector2, Array, etc.)
- ✅ `NativeEngineClassGenerator` - Engine classes (Node, Resource, etc.)
- ✅ `NativeMethodGenerator` - Métodos compartidos builtin/engine
- ✅ `KNativeStructureGenerator` - Native structures (GDExtension C structs o wrappers con delegación)
- ✅ `NativeUtilityFunctionGenerator` - Utility functions en object GD
- ✅ `KDocFormatter` - Conversión BBCode → KDoc/Markdown con line wrapping

**Features implementadas**:

- ✅ Properties indexadas con enum constant resolution
- ✅ Operators (plus, minus, equals, compareTo, etc.)
- ✅ Constructors (normales y especiales como Transform3D)
- ✅ Static methods en companion object
- ✅ Singletons con lazy instance
- ✅ Herencia y modificadores correctos
- ✅ Package organization (core/node, core/resource, editor, singleton, global)

### 2. **Default Value Generation** ⭐ NUEVO

**Implementado en `DefaultValueGenerator`**:

- ✅ Primitivos: `0`, `1.5`, `1e-05` → `0f`, `1.5f`, `1e-05f` (según tipo esperado)
- ✅ Strings: `"text"` → `GodotString("text")` o `kotlin.String` según contexto
- ✅ StringName: `&"text"` → `StringName("text")`
- ✅ NodePath: `^"path"` o `"path"` → `NodePath("path")`
- ✅ Enums desde valor numérico: `0` → `Key.NONE` (usando EnumConstantResolver)
- ✅ Variant desde valor: `0` → `Variant.INT(0L)`, `nil` → `Variant.NIL`
- ✅ Constructores simples: `Vector2(0, 0)` → `Vector2(0f, 0f)`
- ✅ Constructores recursivos: `Transform2D(Vector2(1, 0), ...)` → conversión recursiva
- ✅ Constructores especiales: `Transform3D(1,0,0,0,1,0,...)` → `Transform3D(Vector3(...), Vector3(...), ...)`
- ✅ Bitfields: `FLAG_A | FLAG_B` → `Flags.A.value or Flags.B.value` O valor número directamente (temporalmente sin tipado)
- ✅ Arrays: `[]`, `PackedStringArray()` → constructores vacíos
- ✅ Infinity: `inf`, `-inf` → `Float.POSITIVE_INFINITY`, `Float.NEGATIVE_INFINITY`
- ✅ NaN: `nan` → `Float.NaN`
- ✅ TypedArrays: `Array[RID]([])` → `Array()` (temporalmente sin tipado)

**Constructor helpers agregados**:

```kotlin
// Generados en builtin classes
GodotString(value: kotlin.String)
StringName(value: kotlin.String)
NodePath(value: kotlin.String)
```

### 3. **Type Resolution Strategy**

**Punteros**:

- Punteros a primitivos → `CPointer<IntVar>`, `CPointer<FloatVar>`, etc.
- Estructuras C nativas → `CPointer<NativeStruct>`

**Context extendido**:

- ✅ Almacena `ExtensionApi` completa para lookups
- ✅ `findBuiltinClass()`, `findEngineClass()`, `findConstructor()`
- ✅ Constructor resolution para default values

---

## 🚧 Siguiente Paso: FFI Binding Layer

### **Input Disponible**:

1. ✅ `extension_api.json` - Clases, métodos, enums (ya usado)
2. ✅ `gdextension_interface.json` - **Funciones FFI** (NO usado aún) ⭐

### **Qué Generar**:

**En módulo `runtime/`**:

```text
// runtime/src/nativeMain/kotlin/io/github/kingg22/godot/ffi/

├── VariantBindings.kt         // ← GENERADO (lazy loading)
├── StringBindings.kt          // ← GENERADO
├── StringNameBindings.kt      // ← GENERADO
├── ObjectBindings.kt          // ← GENERADO
├── ClassDBBindings.kt         // ← GENERADO
└── BuiltinClassBindings.kt    // ← GENERADO

// Manual (hardcoded para empezar):
GodotEntry.kt                  // @CName("godot_kotlin_init")
Conversions.kt                 // Kotlin ↔ Godot conversions
```

**Estructura del generador**:

```kotlin
class GDExtensionInterfaceGenerator {
  fun generate(interfaceJson: GDExtensionInterface): Sequence<FileSpec>

  // Genera clases por categoría:
  // VariantBindings (variant_new_copy, variant_destroy, etc.)
  // StringBindings (string_new_with_utf8_chars, etc.)
  // ObjectBindings (object_method_bind_call, etc.)
  // ClassDBBindings (classdb_get_method_bind, etc.)

  // Cada función es lazy:
  val newCopy: VariantNewCopy by lazy(LazyThreadSafetyMode.PUBLICATION) {
    memScoped {
      val variantNewCopy: GDExtensionInterfaceVariantNewCopy = getProcAddress("variant_new_copy".cstr.ptr)
        ?.reinterpret() ?: error("Failed to load variant_new_copy")
      VariantNewCopy(variantNewCopy)
    }
  }
}
```

**En módulo `api/`**:

- Reemplazar todos los `TODO()` con implementaciones reales usando `GDExtensionInterface` binding.

---

## 🎯 Plan propuesto

1. **Crear `GDExtensionInterfaceGenerator`**:

   - Leer `gdextension_interface.json`
   - Generar clases de bindings por categoría
   - Output: `runtime/src/nativeMain/kotlin/ffi/`

2. **Implementar 1 clase completa** (proof of concept):

   - Vector2: constructor, plus, minus
   - Usando los bindings generados

3. **Hardcoded entry point** (temporal):

   - Manual en `runtime/Main.kt`
   - Inicializa `GDExtensionInterface`

---

## 🔑 Decisiones Clave Confirmadas

| Decisión                                                             | Razón                                                                                                     |
|----------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Default values totalmente funcionales                                | ✅ Parsing recursivo de constructores                                                                      |
| Punteros = COpaquePointer, CPointer\<VarOf>, CPointer\<NativeStruct> | Clases generadas no son CStruct todas, delegan a pointer                                                  |
| FFI bindings lazy                                                    | Performance (evitar cargar todo al inicio, evitar clases grandes o con muchas referencias a otras clases) |
| Hardcoded entry point (temporal)                                     | Probar binding antes de hacer plugin                                                                      |
| gdextension_interface.json disponible                                | Godot lo provee, no hay que parsear el C header file                                                      |
| godot-kt-cinterop-clean.md disponible                                | Provee las firmas del código generado por kotlin native cinterop del C header file                        |
