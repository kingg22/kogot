# Guía de Compatibilidad Backward & Forward

## Binding Godot ↔ JVM (Java/Kotlin) vía GDExtension + FFM

**Objetivo:** Garantizar que el binding y runtime funcionen de forma segura a través de múltiples versiones de Godot, del JDK (FFM) y de la API pública del proyecto, evitando roturas silenciosas, crashes nativos o corrupción de memoria.

**Alcance:** Todas las capas — ABI mirror, runtime interno, API pública, tooling y distribución.

---

## 1) Principios de diseño (reglas de oro)

1. **ABI primero, API después**
   La capa ABI (mirror del header) es la fuente de verdad. Nunca “inventar” layouts ni asumir tamaños.

2. **Compatibilidad por capas**

   * Layer 0: ABI mirror (exacto al header)
   * Layer 1: Runtime interno (wrappers seguros)
   * Layer 2: API pública (Kotlin/Java idiomática)
   * Layer 3: Tooling / Editor / Scripts

3. **Feature detection > Version detection**
   Preferir comprobar disponibilidad de funciones/campos a depender de números de versión.

4. **Fail fast, fail loud**
   Si una función requerida no existe o cambia layout → error explícito al iniciar.

5. **Nunca romper el layout binario**
   Cualquier cambio incompatible debe ir detrás de:

   * nueva interfaz
   * feature flag
   * versión mayor

---

## 2) Compatibilidad con Godot (Engine)

### 2.1 Handshake de versión al iniciar

Al cargar el GDExtension:

* Validar:

  * versión mínima soportada del engine
  * tamaño esperado de structs críticos
  * presencia de funciones obligatorias

**Estrategia:**

```text
Si falta función requerida → abortar carga con mensaje claro
Si hay funciones nuevas → habilitar feature opcional
```

### 2.2 Tabla de funciones (GDExtensionInterface)

**Riesgo:** Godot puede agregar campos al struct.

**Solución segura:**

* Nunca asumir tamaño fijo
* Leer solo campos conocidos
* Comprobar null en punteros a funciones

### 2.3 Strategy: Optional Binding

Clasificar funciones en:

* **Core (obligatorias)**
* **Optional (feature gated)**
* **Deprecated**

Ejemplo:

```text
variant_call → core
editor APIs → optional
legacy script APIs → deprecated
```

---

## 3) Compatibilidad del ABI (Layer 0)

### 3.1 Regla crítica

> ⚠️ Layer 0 es inmutable salvo sync con header oficial

No:

* renombrar símbolos
* cambiar tipos
* añadir lógica

Solo:

* regenerar con jextract
* aplicar parches mecánicos

---

### 3.2 Control de drift con upstream

Mantener:

* checksum del header usado
* diff automatizado entre versiones
* pruebas ABI

---

## 4) Compatibilidad Runtime (Layer 1)

Aquí ocurren la mayoría de los problemas.

---

### 4.1 Manejo de cambios en Variant

Los tipos de Variant pueden cambiar entre versiones.

**Estrategia:**

* Mapear por `VariantType`, no por ordinal implícito
* Manejar tipos desconocidos como `OpaqueVariant`

---

### 4.2 Object lifecycle

Godot usa refcount manual; JVM usa GC.

**Compatibilidad segura:**

* Tabla de handles nativos ↔ objetos JVM
* Finalización controlada
* Protección contra double-free

---

### 4.3 Method dispatch

Nunca cachear punteros a métodos sin invalidación por versión.

---

## 5) Compatibilidad API pública (Layer 2)

### 5.1 Regla de estabilidad

La API pública debe seguir **SemVer estricto**:

* MAJOR → cambios incompatibles
* MINOR → features nuevos
* PATCH → fixes

---

### 5.2 Strategy: Facade estable

Mantener una fachada estable incluso si internals cambian.

Ejemplo:

```kotlin
Node.call("method")
```

Internamente puede cambiar, externamente no.

---

### 5.3 Deprecation policy

1. Marcar como deprecated
2. Mantener al menos una versión mayor
3. Proveer migración automática si es posible

---

## 6) Compatibilidad FFM (JDK)

El FFM API aún evoluciona.

### Estrategias:

* Encapsular FFM en un módulo interno
* No exponer MemorySegment en API pública
* Proveer adaptadores por versión de JDK

---

## 7) Compatibilidad de Scripts JVM

### 7.1 Cambios en ScriptInstance

Usar detección de capabilities:

```text
Si existe ScriptInstanceInfo4 → usar
Si no → fallback a Info3
```

---

### 7.2 Serialización de scripts

Nunca depender del nombre de clase JVM como ID persistente.

Usar:

* UUID interno
* nombre lógico del script

---

## 8) Estrategias de Testing

### 8.1 Matriz de compatibilidad

Probar contra:

* múltiples versiones de Godot
* múltiples JDK
* múltiples OS

---

### 8.2 Tests ABI

Validar:

* tamaños de structs
* offsets
* alineación

---

## 9) Warnings obligatorios para usuarios

El proyecto debe documentar claramente:

### ⚠️ Riesgos nativos

* crashes posibles si se usa API interna
* incompatibilidad entre versiones
* necesidad de recompilar en cambios mayores

---

### ⚠️ Limitaciones

* soporte parcial del editor inicialmente
* features experimentales marcadas

---

## 10) Documentación obligatoria

Debe existir:

1. **Compatibility Matrix**
2. **Upgrade Guide**
3. **Migration Guide**
4. **Breaking Changes Log**

---

## 11) Política de versiones recomendada

### Versión del binding:

```
MAJOR.MINOR.PATCH
```

### Compatibilidad declarada:

```
Godot 4.x.y+
JDK N+
```

---

## 12) Plan de Forward Compatibility

Prepararse para:

* nuevos tipos Variant
* nuevas APIs editor
* cambios en scripting

### Técnica clave:

# 🧠 Capability-based design

En lugar de:

```kotlin
if (version >= 4.3)
```

usar:

```kotlin
if (functionExists)
```

---

## 13) Checklist antes de release

* [ ] Tests en múltiples versiones
* [ ] Verificación ABI
* [ ] Documentación actualizada
* [ ] Notas de compatibilidad
* [ ] Warnings de breaking changes

---

## 14) Conclusión

Un binding seguro Godot ↔ JVM requiere:

* disciplina estricta por capas
* detección de capacidades
* documentación clara
* fallos explícitos en incompatibilidades

Si se siguen estas reglas, el proyecto podrá evolucionar junto a Godot sin reescrituras masivas ni riesgos de memoria.

---

**Estado del documento:** Base de referencia para desarrollo y mantenimiento.
