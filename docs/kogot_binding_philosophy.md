# Kogot Binding Philosophy

## Objetivo

Kogot debe seguir la filosofía de `godot-rust/gdext`: el API pública debe ser una proyección mecánica, directa y verificable del contrato oficial de Godot. La diferencia intencional es una capa runtime mínima, también generada, que conserva la documentación del JSON y resuelve el acceso a símbolos del raw cinterop con _lazy loading_.

La regla principal es esta:

**el código generado de API no debe depender de helpers manuales que escondan decisiones de memoria, inicialización o dispatch que ya están descritas en el JSON o en el runtime binding generado.**

## Capas permitidas

### 1. Modelo crudo del JSON

`codegen.models.extensionapi.*` debe representar el contrato serializado de Godot con la menor interpretación posible.

Reglas:

- Reflejar el shape del JSON.
- No mezclar decisiones de empaquetado, runtime naming o ownership.
- Mantener nombres y cardinalidades cercanas a la fuente.

### 2. Índices de consulta inmutables

El equivalente actual de `Context` debe convertirse en una capa de índices y lookups precomputados.

Responsabilidades válidas:

- clasificación de tipos
- árbol de herencia
- resolución de enums por valor
- acceso rápido por nombre
- selección de configuración activa (`float_32`, `double_64`, etc.)
- acceso a metadatos estructurales ya presentes en el JSON

Responsabilidades inválidas:

- lógica específica de un generador
- nombres de funciones runtime inventados ad hoc
- decisiones de layout hardcodeadas en switches
- conocimiento de paquetes o rendering textual que no sea transversal

### 3. Modelos enriquecidos para codegen

Los generadores no deberían operar sobre `BuiltinClass` o `EngineClass` crudos si necesitan contexto adicional. Deben recibir vistas enriquecidas, construidas una vez.

Ejemplos recomendados:

- `ResolvedBuiltinModel`
- `ResolvedEngineClassModel`
- `ResolvedBuiltinConstructor`
- `ResolvedBuiltinLayout`
- `ResolvedBuiltinLifecycle`
- `ResolvedMethodBinding`

Estas vistas deben contener datos ya resueltos, por ejemplo:

- `isSingleton: Boolean`
- `singletonAccessorName`
- `apiPackage`
- `runtimeBindingClassName`
- `runtimeFunctionName`
- `activeSize`
- `memberOffsets`
- `hasDestructor`
- `destructorKind`
- `storageStrategy`
- `inheritsFrom`
- `isRefCounted`
- `isInstantiable`

La meta es que un generador renderice, no investigue.

## Runtime generado

`RuntimeFFIGenerator` ya marca la dirección correcta: bindings por grupo de símbolos, comentarios provenientes del JSON, y carga lazy por símbolo.

La filosofía a imponer es:

- cada wrapper runtime representa un símbolo real del API de Godot
- cada property lazy encapsula exactamente un `getProcAddress`
- cada wrapper inline delega casi 1:1 al puntero cargado
- las funciones de conveniencia solo existen si son derivables mecánicamente del contrato C

## `BuiltinRuntime.kt` no debe existir

`BuiltinRuntime.kt` hoy concentra lógica que ya debería estar distribuida en los cuerpos generados o en bindings runtime generados.

Problemas del enfoque actual:

- introduce una capa manual intermedia entre API y binding raw
- hardcodea tamaños (`8`) para `String`, `StringName`, `NodePath`
- hardcodea tablas de init/destroy por `when`
- obliga a que los generadores conozcan un helper global en vez del símbolo runtime real
- rompe la trazabilidad: API -> body generado -> runtime manual -> binding generado

Dirección correcta:

- inlinear en cada constructor/body la secuencia real de inicialización o destrucción
- usar metadata enriquecida para elegir constructor index, size activa y destructor
- invocar directamente el binding generado correspondiente (`StringBinding`, `VariantBinding`, etc.)
- dejar `memScoped`, `cstr` y conversiones puntuales en el body generado cuando formen parte real de la operación

Ejemplo conceptual:

- `GodotString(String)` no llama a `BuiltinRuntime.initializeStringFromUtf8(...)`
- genera directamente el body con `memScoped { StringBinding.instance.newWithUtf8CharsRaw(rawPtr, value.cstr.ptr) }`
- `close()` no llama a `BuiltinRuntime.destroyString(rawPtr)`
- genera directamente la resolución del destructor y la liberación del storage, o usa el binding directo si el símbolo ya existe generado

## Lazy loading

Kogot debe preferir lazy loading por símbolo en runtime binding, no precarga global tipo `method table`.

Razones:

- mantiene el costo alineado con el uso real
- reduce superficie inicial de fallo
- encaja mejor con runtime generado por grupos
- simplifica evolución incremental del binding

Condición:

- el símbolo lazy debe vivir lo más cerca posible de su representación raw
- la API pública no debe introducir otra caché por encima salvo necesidad demostrada

## Layout y offsets

Los tamaños y offsets no deben aparecer como literales hardcodeados en generadores de bodies.

Fuente de verdad:

- `builtin_class_sizes`
- `builtin_class_member_offsets`

Política:

- resolver la configuración activa una sola vez desde `header.precision` y plataforma objetivo
- materializar `ResolvedBuiltinLayout` por tipo builtin
- exponer `size`, `alignment` si aplica, y offsets por miembro
- usar ese modelo tanto para allocate/free como para validaciones y accessors

Si un builtin necesita storage heap temporal, el tamaño debe salir del layout resuelto, nunca de un `when` manual.

## `Context` debe adelgazar

El `Context` actual ya hace demasiado: clasificación, lookups, versionado, enums, package registry, experimental registry y soporte indirecto a decisiones de generación.

Recomendación estructural:

- mantener `Context` como root object inmutable del pipeline
- mover índices especializados a componentes dedicados
- exponer un `CodegenModelRegistry` o `ResolvedApiModel`

Propuesta de partición:

- `ApiIndex`: lookups por nombre, clases, builtins, singletons
- `TypeSystemIndex`: builtins, enums, herencia, specializations
- `BuiltinLayoutIndex`: sizes y offsets por configuración activa
- `RuntimeBindingIndex`: nombres de binding class y function names resolvidos
- `ResolvedApiModel`: vistas enriquecidas listas para render
- `PackageRegistry`: solo packaging y naming Kotlin

Esto baja el acoplamiento y evita que cada generador pregunte cinco cosas distintas al contexto global.

## Singletons

`isSingleton` no debe calcularse repetidamente en generadores. Debe llegar resuelto en el modelo de clase.

Regla:

- un `ResolvedEngineClassModel` ya sabe si es singleton
- el package target, el patrón de acceso y cualquier restricción derivada se resuelven antes del render

## Runtime naming

Los nombres de runtime function no deben deducirse dispersamente con convenciones implícitas en los generadores.

Regla:

- si un método/constructor/destructor necesita un símbolo runtime específico, ese nombre debe estar resuelto en el modelo enriquecido
- el generador consume `runtimeFunctionName`, no vuelve a inferirlo

Esto es especialmente importante para:

- constructores de builtins
- destructores
- conversores especiales (`String` <-> UTF-8)
- ptrcall / method bind helpers

## Ownership y destructores C

Un destructor de C no es automáticamente equivalente semántico a `AutoCloseable`.

### Problema

`AutoCloseable` expresa una interfaz de uso idiomático (`use {}`), pero no documenta por sí sola:

- si la instancia es dueña única del recurso
- si `close()` es obligatorio o solo oportunista
- si `close()` invalida aliases compartidos
- si el valor es movible o copiable sin duplicar ownership
- si el destructor es idempotente a nivel semántico o solo defensivo a nivel wrapper

En builtins como `String`, `StringName`, `NodePath`, `Array`, `Dictionary`, `Callable`, `Signal` y packed arrays, el destructor existe, pero la semántica real es más cercana a **owned native handle/value wrapper** que a “stream closable”.

### Recomendación

- no usar `AutoCloseable` como representación conceptual primaria del destructor C
- sí puede mantenerse como adaptación ergonómica para `use {}` cuando aporte valor
- introducir un contrato interno explícito, por ejemplo `NativeOwned`, `GodotOwnedValue`, o `CDisposable`

Ese contrato debería declarar al menos:

- que el wrapper posee un recurso/destructor nativo
- que `destroy()` o `close()` consume esa ownership
- que el objeto queda inválido después de destruirse
- si existe `copy()`/clone constructor con nueva ownership independiente

Modelo sugerido:

- interno: `interface NativeOwned { val rawPtr: COpaquePointer?; fun destroy() }`
- externo opcional: `AutoCloseable` delegando a `destroy()`

Así, `AutoCloseable` queda como azúcar de ergonomía, no como contrato semántico central.

## Regla de generación para builtins con destructor

Para cualquier builtin con destructor:

- el modelo enriquecido debe indicar si usa inline storage, heap storage o bridge externo
- la secuencia `construct -> use -> destroy` debe ser visible en el código generado
- `close()`/`destroy()` debe ser idempotente en el wrapper Kotlin
- el wrapper no debe esconder ownership compartida inexistente

## Regla de generación para métodos y constructores

Cada body generado debe poder leerse como:

1. preparar raw args
2. invocar binding raw o runtime binding generado
3. envolver retorno si aplica
4. limpiar temporales owned locales

No debe haber helpers manuales genéricos salvo que eliminen duplicación sin ocultar semántica.

## Plan recomendado de migración

1. Introducir índices dedicados para builtins layouts, singletons y lookup por nombre.
2. Crear modelos enriquecidos para builtins y engine classes consumidos por los generadores.
3. Migrar `BodyGenerator` para que deje de usar `BuiltinRuntime` y consuma metadata resuelta.
4. Generar inline los bodies de constructores/destructores especiales (`String`, `StringName`, `NodePath`, `Variant`).
5. Reemplazar tamaños hardcodeados por `ResolvedBuiltinLayout.activeSize`.
6. Introducir un contrato interno de ownership distinto de `AutoCloseable`.
7. Evaluar si `AutoCloseable` permanece solo en API pública o también desaparece de ciertos wrappers value-like.
8. Eliminar `BuiltinRuntime.kt` cuando no tenga más consumidores.

## Criterio de aceptación arquitectónica

Una parte del binding está bien diseñada si cumple esto:

- puede trazarse de JSON/API oficial a código generado sin saltos manuales opacos
- el generador no contiene tablas hardcodeadas que ya existen en metadata
- los símbolos raw se resuelven lazy y cerca de su binding
- la ownership nativa se modela explícitamente
- `Context` no concentra conocimiento accidental de demasiadas capas
- el código generado explica su propio lifecycle sin helpers mágicos
