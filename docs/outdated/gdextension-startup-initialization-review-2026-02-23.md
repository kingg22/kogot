# Revisión de Startup/Initialization GDExtension (Godot Java)

Fecha: 2026-02-23

## 1) Fuentes analizadas

- Documentación del repo `godot-java` (MDs en `docs/` y `CONTRIBUTING.md`).
- Implementación actual:
  - `godot-extension-register/gdextension_init.c`
  - `godot-java-bridge/src/main/java/io/github/kingg22/godot/internal/initialization/GodotBridge.java`
  - `godot-java-bridge/src/main/java/io/github/kingg22/godot/internal/bridge/BridgeContext.java`
- Referencia técnica `godot-rust` (flujo real por niveles):
  - `godot-core/src/init/mod.rs`
  - `godot-ffi/src/init_level.rs`
  - `godot-ffi/src/lib.rs`

## 2) Modelo correcto y rápido de initialization (derivado de godot-rust + GDExtension)

## Orden y criterio

Godot inicializa en niveles: `CORE -> SERVERS -> SCENE -> EDITOR` (y en Godot 4.5+ puede existir etapa `MainLoop` para post-init). En `godot-rust` esto está explícito en:

- `godot-ffi/src/init_level.rs` (definición de niveles)
- `godot-core/src/init/mod.rs:136-240` (init por nivel)
- `godot-core/src/init/mod.rs:243-264` (deinit en orden inverso)

Regla principal: cada fase debe ejecutar solo lo que ese nivel garantiza, y todo lo pesado debe diferirse al punto más tardío seguro (idealmente `MainLoop`/primer frame).

## Qué API ejecutar en cada fase

1. `Entry point` (antes de niveles)
- Guardar `get_proc_address` y `library_ptr`.
- Resolver funciones mínimas de logging/compatibilidad (`print_error`, versión runtime).
- Evitar trabajo pesado aquí.

2. `CORE`
- Inicializar tabla base de interfaz (function pointers globales).
- Inicializar builtins/utility mínimos (Variant, StringName lifecycle, etc.).
- Validar compatibilidad ABI/versiones temprano (fail-fast).
- No tocar APIs de escena ni singletons no garantizados.

3. `SERVERS`
- Cargar method tables de server classes.
- Inicializaciones de runtime que no dependan del árbol de escena/editor.

4. `SCENE`
- Cargar method tables de clases de escena.
- Registrar clases runtime en ClassDB (`classdb_register_extension_class*`, métodos, propiedades).
- Habilitar bridge de instancias (`object_set_instance`) y script instance (`script_instance_create3`, `object_set_script_instance`).
- Checks de features que requieren Scene (ejemplo en `godot-rust`: validación de precisión en Scene).

5. `EDITOR` (solo editor)
- Registro editor-only: integración de lenguaje/script, templates, docs, tooling.
- No depender de esta fase para que el juego exportado funcione.

6. `MainLoop` / post-init (4.5+)
- Trabajo pesado diferible: escaneo grande, warm-up, I/O de configuración no crítica, cacheado agresivo.
- Objetivo: minimizar tiempo bloqueante durante callbacks de niveles.

7. `Deinit` (inverso)
- `EDITOR -> SCENE -> SERVERS -> CORE`.
- Soltar recursos del nivel correspondiente.
- Deinit global únicamente al final (`CORE`).

## 3) Hallazgos en tu implementación actual

## Hallazgos críticos

1. `minimum_initialization_level = EDITOR` acopla el runtime al editor.
- Archivo: `godot-extension-register/gdextension_init.c:415`
- Impacto: en builds de juego (sin nivel editor), la extensión puede no inicializarse.
- Recomendación: usar `SCENE` como mínimo para runtime, y usar callback `EDITOR` solo para extras de editor.

2. Inicialización y cleanup de JVM solo en `EDITOR`.
- Archivos:
  - `godot-extension-register/gdextension_init.c:382-397`
  - `godot-extension-register/gdextension_init.c:340-344`
- Impacto: mismo problema de disponibilidad en runtime exportado.

3. `BridgeContext` referencia `ClassDBBridge`, pero no existe clase en el árbol actual.
- Archivo: `godot-java-bridge/src/main/java/io/github/kingg22/godot/internal/bridge/BridgeContext.java:16,23,62`
- Impacto: inconsistencia de código (compilación rota o archivo faltante).

## Hallazgos altos

4. Error de compilación en rama Windows.
- Archivo: `godot-extension-register/gdextension_init.c:94`
- Problema: falta `;` en `log_error("Invalid directory, can't find any jars")`.

5. Uso de `uintptr_t` sin incluir `<stdint.h>`.
- Archivo: `godot-extension-register/gdextension_init.c:321-322`
- Riesgo: compilación dependiente de toolchain.

6. `build_classpath_from_dir` no valida `NULL` al usar `jars`.
- Archivo: `godot-extension-register/gdextension_init.c:223-230`
- Riesgo: UB/crash si `malloc` falla.

7. Java startup realiza tareas no esenciales durante init de extensión.
- Archivo: `godot-java-bridge/src/main/java/io/github/kingg22/godot/internal/initialization/GodotBridge.java:22-34`
- Impacto: aumento de latencia de arranque y posibles freezes en editor.

## Hallazgos medios

8. Manejo de excepciones JNI incompleto.
- Archivo: `godot-extension-register/gdextension_init.c:299-301,315-316,328-329`
- Problema: se imprime excepción pero no se limpia (`ExceptionClear`), pudiendo contaminar llamadas JNI siguientes.

9. `initialize()` en Java traga excepciones y no comunica fallo al bootstrap nativo.
- Archivo: `godot-java-bridge/src/main/java/io/github/kingg22/godot/internal/initialization/GodotBridge.java:37-40`
- Impacto: estado parcial difícil de diagnosticar.

10. `BridgeContext.instance` sin sincronización explícita.
- Archivo: `godot-java-bridge/src/main/java/io/github/kingg22/godot/internal/bridge/BridgeContext.java:11,27-36`
- Riesgo bajo si siempre main thread, pero conviene formalizar contrato.

## 4) Correcciones recomendadas

## A. Estructura por fases (prioridad máxima)

1. Cambiar `minimum_initialization_level` a `GDEXTENSION_INITIALIZATION_SCENE`.
2. En callback C, manejar explícitamente niveles:
- `CORE`: validaciones mínimas + logging API.
- `SCENE`: levantar JVM y `GodotBridge.initialize(...)` runtime.
- `EDITOR`: registrar integración/editor extras.
3. Deinit en espejo por nivel; destruir JVM al nivel donde se creó (o en `CORE` si es singleton de toda la extensión).

## B. Hardening C

1. Añadir `#include <stdint.h>`.
2. Corregir el `;` faltante en rama Windows.
3. Validar `jars != NULL` antes de `snprintf`.
4. Limpiar excepciones JNI tras `ExceptionDescribe`.
5. Separar rutas:
- error fatal de bootstrap (debe abortar init de extensión),
- warning recuperable (defer/log y seguir).

## C. Hardening Java

1. Reemplazar init monolítico por init por etapa:
- `bootstrap(getProc, lib)`
- `initRuntimeScene()`
- `initEditorTools()`
- `postMainLoopWarmup()`
2. Mover `loadConfigurations()` / managers pesados fuera del callback inicial (post-init).
3. Definir contrato de thread (main thread) y hacerlo explícito en `BridgeContext`.
4. Si `initialize` falla, propagar error al lado nativo (no solo imprimir stacktrace).

## D. API objetivo por fase para tu bridge

1. `SCENE` (mínimo funcional runtime)
- `classdb_register_extension_class5`
- `classdb_register_extension_class_method`
- `object_set_instance`
- `script_instance_create3`
- `object_set_script_instance`

2. `EDITOR` (visibilidad y tooling)
- Registro de lenguaje/script (`GDExtensionScriptLanguageInfo` o equivalente usado por tu diseño)
- Plantillas/creación de scripts
- Registro de docs/metadatos editor

3. `MainLoop` (si Godot 4.5+)
- Warm-up y trabajo costoso no crítico

## 5) Secuencia propuesta concreta para godot-java

1. `godot_java_bridge_init(...)`
- guardar punteros
- set callbacks
- `minimum_initialization_level = SCENE`

2. `initialize_callback(level)`
- `CORE`: bind logging + checks ABI/version
- `SERVERS`: no-op o prep liviana
- `SCENE`: start JVM + `GodotBridge.initializeRuntime(getProc, lib)` + registro ClassDB/ScriptInstance
- `EDITOR`: `GodotBridge.initializeEditor(getProc, lib)`

3. `deinitialize_callback(level)`
- `EDITOR`: liberar recursos editor-only
- `SCENE`: desmontar runtime de escena
- `CORE`: shutdown JVM global (si aplica) + limpiar tablas globales

## 6) Resumen ejecutivo

Tu implementación actual ya demuestra que el pipeline JNI + FFM funciona, pero está centrada en `EDITOR` y mezcla trabajo de arranque pesado en el callback de init. Para un GDExtension correcto, portable y rápido:

- El runtime debe vivir al menos desde `SCENE`.
- `EDITOR` debe ser adicional, no requisito.
- El init por niveles debe separar APIs por disponibilidad.
- El trabajo pesado debe diferirse a post-init/main loop.
- Hay detalles de robustez C/JNI que conviene corregir antes de escalar.
