# Typed Arrays v2 - Implementación Corregida

## ❌ Errores Previos Identificados

1. **Duplicación de código**: Creé `ArrayTypedClassGenerator` en lugar de interceptor
2. **Type parameter obligatorio**: `GodotArray` sin `<T>` no compila
3. **Constructores inventados**: Añadí constructors no presentes en JSON
4. **Falta de typealias**: No implementé `VariantArray = GodotArray<Variant>`
5. **PackageRegistry**: No distinguía entre typed/untyped arrays

---

## ✅ Solución Correcta - Arquitectura

### 🏗️ Patrón Decorator (Interceptor)

En lugar de duplicar código, usamos **GenericBuiltinInterceptor** que modifica la generación existente:

```kotlin
// ANTES (INCORRECTO):
fun generateArrayClass(...) { /* duplicar todo */
}

// DESPUÉS (CORRECTO):
class GenericBuiltinInterceptor {
  fun requiresGenerics(cls: BuiltinClass): Boolean
  fun getGenericConfig(cls: BuiltinClass): GenericConfig?

  interface GenericConfig {
    val typeVariables: List<TypeVariableName>
    val untypedAlias: Pair<String, TypeName>?
    fun transformReturnType(...)
    fun transformParameterType(...)
    fun transformOperatorReturnType(...)
  }
}
```

**Ventajas**:

- ✅ No duplica código
- ✅ Reutiliza `NativeBuiltinClassGenerator` existente
- ✅ Extensible para `Dictionary<K, V>` futuro
- ✅ Respeta constructors del JSON

---

## 📦 Archivos Creados/Modificados

### 1. **GenericBuiltinInterceptor.kt** ✅ (NUEVO)

```kotlin
class GenericBuiltinInterceptor(private val typeResolver: TypeResolver) {
  fun requiresGenerics(builtinClass: BuiltinClass): Boolean = when (builtinClass.name) {
    "Array" -> true
    else -> false
  }

  fun getGenericConfig(builtinClass: BuiltinClass): GenericConfig? = when (builtinClass.name) {
    "Array" -> ArrayGenericConfig(builtinClass, typeResolver)
    else -> null
  }
}
```

**ArrayGenericConfig**:

```kotlin
private class ArrayGenericConfig(...) : GenericConfig {
  private val T = TypeVariableName("T")

  override val typeVariables = listOf(T)

  override val untypedAlias: Pair<String, TypeName>?
    get() = "VariantArray" to GodotArray.parameterizedBy(Variant)

  override fun transformReturnType(method, originalType): TypeName? {
    // get() → T (indexing_return_type)
    if (method.name == "get" && indexingReturnType == originalType) return T

    // append(arr: Array) → append(arr: Array<T>)
    if (method.returnType == "Array") return GodotArray<T>

    return originalType
  }

  override fun transformParameterType(method, argIndex, originalType): TypeName {
    // set(index, value: Variant) → set(index, value: T)
    if (method.name == "set" && argIndex == 1) return T

    // push_back(value: Variant) → push_back(value: T)
    if (indexingReturnType == originalType) return T

    return originalType
  }
}
```

---

### 2. **NativeBuiltinClassGenerator.kt** ✅ (MODIFICADO)

```kotlin
class NativeBuiltinClassGenerator(...) {
  private val genericInterceptor = GenericBuiltinInterceptor(typeResolver)

  context(context: Context)
  fun generate(builtinClass: BuiltinClass): TypeSpec? {
    // ... código existente ...

    // ── GENERIC INTERCEPTION ──────────────────────────────────
    val genericConfig = if (genericInterceptor.requiresGenerics(builtinClass)) {
      val config = genericInterceptor.getGenericConfig(builtinClass)
      config?.typeVariables?.forEach { typeVar ->
        classBuilder.addTypeVariable(typeVar)
      }
      config
    } else {
      null
    }

    // ── Members, Constructors (SIN CAMBIOS) ──────────────────
    // ...

    // ── Operators (CON TRANSFORMACIÓN) ────────────────────────
    classBuilder.addFunctions(generateOperators(builtinClass, genericConfig))

    // ── Methods (CON TRANSFORMACIÓN) ──────────────────────────
    instanceMethods.forEach { method ->
      var methodSpec = buildMethodWithGenericTransform(
        method, builtinClass.name, genericConfig
      )
      classBuilder.addFunction(methodSpec)
    }
  }

  private fun buildMethodWithGenericTransform(
    method, className, genericConfig
  ): FunSpec {
    if (genericConfig == null) return methodGen.buildMethod(method, className)

    // Transformar return type
    val originalReturnType = method.returnType?.let { typeResolver.resolve(it) }
    val transformedReturnType = genericConfig.transformReturnType(method, originalReturnType)

    // Transformar parámetros
    return methodGen.buildMethod(method, className) {
      if (transformedReturnType != null) returns(transformedReturnType)

      parameters.clear()
      method.arguments.forEachIndexed { index, arg ->
        val originalType = typeResolver.resolve(arg)
        val transformedType = genericConfig.transformParameterType(method, index, originalType)
        addParameter(buildParameter(arg).toBuilder().type(transformedType).build())
      }
    }
  }
}
```

**Cambios clave**:

1. ✅ Detecta clases genéricas con interceptor
2. ✅ Añade `TypeVariableName("T")` automáticamente
3. ✅ Transforma tipos en métodos/operators existentes
4. ✅ **NO inventa constructors** - usa los del JSON

---

### 3. **TypeAliasGenerator.kt** ✅ (NUEVO)

```kotlin
class TypeAliasGenerator(private val genericInterceptor: GenericBuiltinInterceptor) {

  context(context: Context)
  fun generateTypeAlias(builtinClass: BuiltinClass): FileSpec? {
    if (!genericInterceptor.requiresGenerics(builtinClass)) return null

    val genericConfig = genericInterceptor.getGenericConfig(builtinClass) ?: return null
    val (aliasName, aliasType) = genericConfig.untypedAlias ?: return null

    // typealias VariantArray = GodotArray<Variant>
    val typeAliasSpec = TypeAliasSpec.builder(aliasName, aliasType)
      .addKdoc(
        """
                Untyped array, equivalent to `Array` in GDScript.

                For typed arrays, use `GodotArray<ElementType>` instead.
                """
      )
      .build()

    return FileSpec.builder(packageName, aliasName)
      .commonConfiguration()
      .addTypeAlias(typeAliasSpec)
      .build()
  }
}
```

**Output**:

```kotlin
// File: builtin/VariantArray.kt
package io.github.kingg22.godot.api.builtin

/**
 * Untyped array, equivalent to `Array` in GDScript.
 *
 * For typed arrays, use `GodotArray<ElementType>` instead.
 */
typealias VariantArray = GodotArray<Variant>
```

---

### 4. **NativePackageRegistry** ✅ (PARCHE)

```kotlin
// En factory:
context.builtinTypes.forEach { cls ->
  if (cls in NativeBuiltinClassGenerator.SKIPPED_TYPES) {
    // ... manejo de primitivos
  } else {
    register(cls, "$rootPackage.api.builtin")

    // NUEVO: Registrar typealiases
    when (cls) {
      "Array" -> register("VariantArray", "$rootPackage.api.builtin")
      // Futuro: "Dictionary" -> register("VariantDictionary", ...)
    }
  }
}
```

---

### 5. **KotlinNativeImplGenerator** ✅ (PARCHE)

```kotlin
class KotlinNativeImplGenerator(...) {
  // NUEVO
  private val genericInterceptor = GenericBuiltinInterceptor(typeResolver)
  private val typeAliasGen = TypeAliasGenerator(genericInterceptor)

  context(context: Context)
  override fun generate(api: ExtensionApi): Sequence<FileSpec> = sequence {
    val builtinClassesPaths = api.builtinClasses.asSequence().mapNotNull {
      builtinClass.generateFile(it)
    }
    yieldAll(builtinClassesPaths)

    // NUEVO: Generar typealiases
    val typeAliasesPaths = api.builtinClasses.asSequence().mapNotNull {
      typeAliasGen.generateTypeAlias(it)
    }
    yieldAll(typeAliasesPaths)

    // ... resto sin cambios
  }
}
```

---

## 🔄 Flujo de Trabajo Completo

### Input JSON (Godot):

```json
{
  "name": "Array",
  "indexing_return_type": "Variant",
  "is_keyed": false,
  "methods": [
    {
      "name": "get",
      "return_type": "Variant",
      "arguments": [
        {
          "name": "index",
          "type": "int"
        }
      ]
    },
    {
      "name": "set",
      "return_type": null,
      "arguments": [
        {
          "name": "index",
          "type": "int"
        },
        {
          "name": "value",
          "type": "Variant"
        }
      ]
    }
  ]
}
```

### Procesamiento:

1. **GenericBuiltinInterceptor.requiresGenerics("Array")** → `true`
2. **ArrayGenericConfig** crea:

- `typeVariables = [T]`
- `untypedAlias = ("VariantArray", GodotArray<Variant>)`

3. **NativeBuiltinClassGenerator**:

- Añade `<T>` a la clase
- Transforma `get(): Variant` → `get(): T`
- Transforma `set(value: Variant)` → `set(value: T)`

4. **TypeAliasGenerator**: Genera `VariantArray.kt`

### Output Kotlin:

```kotlin
// builtin/GodotArray.kt
class GodotArray<T> : AutoCloseable {
  operator fun get(index: Int): T = TODO()
  operator fun set(index: Int, value: T) = TODO()

  fun append(value: T) = TODO()
  fun push_back(value: T) = TODO()

  // ... resto de métodos del JSON
}

// builtin/VariantArray.kt
typealias VariantArray = GodotArray<Variant>
```

---

## 📊 Casos de Uso

| Código Kotlin        | Equivalente GDScript | Tipo Generado         |
|----------------------|----------------------|-----------------------|
| `VariantArray()`     | `Array()`            | `GodotArray<Variant>` |
| `GodotArray<Node>()` | `Array[Node]()`      | `GodotArray<Node>`    |
| `GodotArray<Int>()`  | `Array[int]()`       | `GodotArray<Int>`     |

---

## 🎯 `indexing_return_type` - Uso Correcto

```json
{
  "name": "Array",
  "indexing_return_type": "Variant"
  // ← Tipo de array[index]
}
```

**Antes (INCORRECTO)**:

```kotlin
operator fun get(index: Int): T = TODO()  // ❌ Inventé T
```

**Después (CORRECTO)**:

```kotlin
// 1. Sin genéricos (otras clases):
operator fun get(index: Int): Variant = TODO()  // ✅ Usa indexing_return_type

// 2. Con genéricos (Array):
// ArrayGenericConfig detecta: indexingReturnType == "Variant" → transforma a T
operator fun get(index: Int): T = TODO()  // ✅ T representa Variant
```

---

## ✅ Validación de Correcciones

### Compilación Kotlin

```kotlin
// ✅ Compila
val untyped: VariantArray = VariantArray()
val typed: GodotArray<Node> = GodotArray()

// ❌ NO compila (requiere type parameter)
val error: GodotArray = GodotArray()  // Error: One type argument expected
```

### Type Safety

```kotlin
val arr: GodotArray<Node> = GodotArray()
arr.append(node)      // ✅ OK
arr.append(123)       // ❌ Type mismatch
```

### JSON Fidelity

```kotlin
// ✅ Solo constructors del JSON, NO inventados
class GodotArray<T> {
  constructor()  // ✅ Del JSON (index 0)
  // NO hay: constructor(vararg elements: T)  ❌ Inventado
}
```

---

## 🚀 Próximos Pasos (TODO)

1. ✅ **Typed Arrays** - CORREGIDO
2. ⬜ **Typed Dictionaries** - `Dictionary<K, V>`
3. ⬜ **Bitfield Masks** - Value class con operadores
4. ⬜ Primary constructors en builtin classes
5. ⬜ Runtime FFI layer

---

## 🔧 Integración Completa

### Archivos a Modificar en el Proyecto Real:

1. **NativeBuiltinClassGenerator.kt**: Añadir interceptor (30 líneas)
2. **KotlinNativeImplGenerator.kt**: Añadir typeAliasGen (3 líneas)
3. **NativePackageRegistry.kt**: Registrar VariantArray (1 línea)

### Archivos Nuevos:

1. **GenericBuiltinInterceptor.kt** (150 líneas)
2. **TypeAliasGenerator.kt** (50 líneas)

**Total**: ~200 líneas nuevas + ~35 líneas modificadas

---

## 📝 Diferencias Clave vs v1

| Aspecto         | v1 (Incorrecto)  | v2 (Correcto)         |
|-----------------|------------------|-----------------------|
| Arquitectura    | Función separada | Interceptor/Decorator |
| Type Parameter  | Opcional         | **Obligatorio**       |
| Constructors    | Inventados       | **Del JSON**          |
| TypeAlias       | ❌ Faltante       | ✅ `VariantArray`      |
| PackageRegistry | Sin cambios      | ✅ Registra alias      |
| Reutilización   | Duplica código   | ✅ Reutiliza generador |
| Extensibilidad  | Difícil          | ✅ Fácil (Dictionary)  |
