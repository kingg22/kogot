# Conversación y notas sobre GodotSharp vs GDExtension JVM

Fecha: 2026-02-23

## Objetivo del usuario
- Crear un binding Godot para Java/Kotlin con FFM API (Java 22+), usando JNI solo para el entrypoint.
- Mantener API similar a GDScript y C#, adaptada a convenciones JVM.
- “Managed always” como C# (auto-enlace entre instancia nativa y managed).
- No escribir C/C++ adicional fuera del entrypoint.

## Resumen del flujo C# (GodotSharp)
- C# en Godot es **módulo integrado**, no GDExtension.
- Startup (C++):
  - `initialize_mono_module()` registra `CSharpLanguage` y crea `GDMono`.
  - `CSharpLanguage::init()` llama `GDMono::initialize()` si corresponde.
  - `GDMono::initialize()` carga hostfxr/coreclr y obtiene entrypoint de C# (`GodotPlugins.*.Main`).
- Entry point administrado:
  - `GodotPlugins.Main.InitializeFromEngine()` configura `NativeFuncs`, `ManagedCallbacks`, `ScriptManager`.
- Auto-enlace de instancias:
  - En C# el runtime usa APIs internas (GCHandle, TieManagedToUnmanaged) que no existen en GDExtension.

## Implicaciones para JVM + GDExtension
- **No es posible copiar 1:1 el modelo C#** porque C# es integrado en core.
- En GDExtension solo hay ABI (function pointers, callbacks). El auto-enlace debe hacerse con:
  - `classdb_register_extension_class*` + `create_instance` + `object_set_instance`.
  - `script_instance_create*` + `object_set_script_instance` para scripts.
- “Managed always” en JVM se logra haciendo **el enlace en callbacks** de creación de instancia.

## Estado actual del proyecto JVM
- Entry point C: `godot-extension-register/gdextension_init.c`
  - `minimum_initialization_level = SCENE`.
  - Inicializa JVM y llama `GodotBridge.initialize(long getProcAddress, long libraryPtr)`.
- Bridge Java:
  - `BridgeContext` mantiene FFI, StringNameCache, ClassDBBridge, ScriptInstanceBridge.
  - `ClassDBBridge` registra clases, crea instancias, enlaza `object_set_instance`.
  - `ScriptInstanceBridge` construye `GDExtensionScriptInstanceInfo3` y crea script instances.

## Problema reportado
- Las clases registradas no aparecen en el editor.

### Causa probable
- Registro solo en nivel `SCENE`.
- Para editor, hay que registrar en `EDITOR` y/o implementar ScriptLanguage.

## ScriptLanguageInfo: aclaración
- Sirve tanto para:
  - Permitir que el editor cree scripts nuevos (plantillas, extensión, listados).
  - Asociar scripts existentes con el lenguaje (abrir, parsear, autocompletar, propiedades).
- Sin `ScriptLanguage` el editor no sabe tratar `.java`/`.kt` como scripts.

## Recomendaciones sobre API pública
- **Usar C# como referencia de nombres/shape**, pero no copiar implementación.
- API core sugerida: `GodotObject`, `Node`, `Resource`, `RefCounted`, `Variant`, `StringName`, `Callable`, `GD`.
- Evitar “extensiones Kotlin” en el core; usarlas solo como sugar opcional.

## Diferencias a tener en cuenta (GodotSharp vs JVM)
- C# usa APIs internas (GCHandle, interop nativo); en JVM se requiere mapear manualmente.
- No hay `partial class` en Java; Kotlin extensions no traducen 1:1 a Java.
- GDExtension impone callbacks y ABI; no existe el mismo punto de integración profundo.

## Archivos clave referenciados
- `modules/mono/register_types.cpp`
- `modules/mono/csharp_script.cpp`
- `modules/mono/mono_gd/gd_mono.cpp`
- `modules/mono/glue/GodotSharp/GodotPlugins/Main.cs`
- `godot-java-bridge/.../BridgeContext.java`
- `godot-java-bridge/.../ClassDBBridge.java`
- `godot-java-bridge/.../ScriptInstanceBridge.java`
- `godot-extension-register/gdextension_init.c`

## Próximos pasos sugeridos
- Elevar `minimum_initialization_level` y registrar clases en `EDITOR`.
- Implementar `ScriptLanguage` via GDExtension (al menos extensión, creación de script, reconocimiento de archivos).
- Definir política de ownership y bindings para GC (por ahora manual con maps).

