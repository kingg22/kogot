# Plan técnico: registro ClassDB + Script Instances con FFM (Java 25)

## Objetivo
Mover el **registro de clases, métodos y script instances** completamente a Java usando FFM, sin más C adicional
que el entrypoint ya existente. El bridge Java será responsable de:

- Registrar clases y métodos en ClassDB.
- Crear/administrar instancias y su lifecycle.
- Crear y asociar script instances cuando aplique.

El módulo **binding** sigue siendo 1:1 con la API GDExtension (FFM); toda lógica de alto nivel vive en **bridge**.

## Punto de partida
Actualmente el entrypoint C ya:
- Inicializa la JVM.
- Llama `GodotBridge.initialize(long, long)` con `get_proc_address` y `library_ptr`.

La migración de lógica ocurre **desde `GodotBridge` hacia Java** usando FFM con upcalls/downcalls.

## Diseño propuesto

### 1) Capa FFM (binding)
Se mantiene sin cambios:
- Wrappers jextract (GDExtensionInterface*).
- Struct layouts (GDExtensionClassMethodInfo, GDExtensionClassCreationInfo5, etc.).

### 2) Bridge Java (alto nivel)
Se agregan componentes en `godot-java-bridge`:

- **BridgeContext**
  - Guarda `getProcAddress`, `libraryPtr`.
  - Arena compartida para stubs (upcalls).
  - Cache de punteros a funciones.

- **StringNameCache**
  - Crea `StringName` con `string_name_new_with_utf8_chars`.
  - Cache por string para reutilizar.

- **ClassDBBridge**
  - Registra clases con `classdb_register_extension_class5`.
  - Registra métodos con `classdb_register_extension_class_method`.
  - Maneja callbacks `create_instance` y `free_instance`.
  - Mapea `GDExtensionClassInstancePtr` ↔ Java instance.

- **ScriptInstanceBridge**
  - Construye `GDExtensionScriptInstanceInfo3`.
  - Provee callbacks mínimos (`has_method`, `call`, `free`).
  - Crea y asocia instancias con `script_instance_create3` y `object_set_script_instance`.

### 3) Flujo de inicialización
```
Godot → C entrypoint → GodotBridge.initialize()
  → BridgeContext.initialize()
  → ClassDBBridge.register(...)
  → (opcional) ScriptInstanceBridge.register(...)
```

### 4) Memoria / lifecycle
- Stubs FFM y `StringName` viven en una Arena compartida (hasta shutdown).
- Instancias usan arenas por instancia; se liberan en callbacks `free_instance` / `free`.
- Method userdata y class userdata se guardan en memoria nativa (long IDs).

### 5) Compatibilidad JVM HotSpot / GraalVM
El bridge trabaja solo con FFM y punteros:
- Funciona en **HotSpot** (JVM estándar).
- Funciona en **GraalVM Native Image** (mismo path de FFM, sin JNI).

## Roadmap corto
1) Añadir `BridgeContext` + `GodotFFI` + `StringNameCache`.
2) Implementar `ClassDBBridge` con registro de clase y método.
3) Implementar `ScriptInstanceBridge` con callbacks mínimos.
4) Añadir API de registro desde Java (anotaciones o registry manual).

## Riesgos / dudas abiertas
- Tamaño real de `StringName` y `Variant` (usar tamaños de `extension_api.json` para 4.5.1).
- Notificación `NOTIFICATION_POSTINITIALIZE` en `create_instance` (si se requiere).
- Validar qué callbacks mínimos exige Godot para script instances (incrementar cobertura si falla).

## Criterio de éxito (MVP)
- Godot registra una clase Java en ClassDB.
- Godot puede invocar métodos Java registrados.
- Script instance puede responder a `has_method` y `call` básicos.
