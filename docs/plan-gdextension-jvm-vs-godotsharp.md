# Plan y contexto: adaptación GodotSharp → GDExtension JVM (Java/Kotlin)

Fecha: 2026-02-23

## Contexto
- GodotSharp (C#) es un **módulo oficial integrado** en el engine.
- El binding JVM es **GDExtension** y solo puede usar ABI + callbacks.
- El objetivo es una API “parecida a GDScript/C#” pero con convención JVM.
- No se desea escribir C/C++ adicional fuera del entrypoint.

## Diferencia central
- C# puede ligar automáticamente managed ↔ native mediante APIs internas.
- GDExtension requiere que el binding implemente explícitamente:
  - Registro de clases.
  - Creación de instancias.
  - Enlace managed con `object_set_instance`.
  - Script instances con `object_set_script_instance`.

## Repositorios y referencias útiles
- **godot-rust**: referencia principal para ver flujo real de GDExtension en lenguaje no oficial.
  - Ruta: `/home/kingg22/IdeaProjects/godot-rust`.
- **godot-docs**: referencias sobre ScriptLanguageExtension y GDExtension.
  - Ruta: `/home/kingg22/IdeaProjects/godot-docs`.

## Objetivo técnico (mínimo funcional)
1) Godot puede registrar clases Java/Kotlin en ClassDB.
2) Godot crea instancias y se enlazan a objetos JVM (“managed always”).
3) El editor lista las clases y permite crear nodos.
4) Scripts JVM pueden ser creados/abiertos en editor (mínimo).

## Componentes mínimos
### 1) GDExtension init
- `minimum_initialization_level` ≥ `EDITOR`.
- En `EDITOR`: registrar clases para visibilidad en editor.
- En `SCENE`: habilitar runtime (si se desea separar).

### 2) ClassDB bridge (Java)
- `classdb_register_extension_class*` + `create_instance` + `free_instance`.
- `object_set_instance` para enlazar pointer ↔ objeto JVM.
- Mapa `instance_ptr → Java instance` con limpieza en `free_instance`.

### 3) Script instance bridge (Java)
- `script_instance_create*` con `GDExtensionScriptInstanceInfo3`.
- `object_set_script_instance` en cada objeto que use script.
- Callbacks mínimos: `has_method`, `call`, `free`.

### 4) ScriptLanguage (GDExtension)
- Registrar ScriptLanguage para que el editor:
  - reconozca `.java/.kt`.
  - cree archivos de script.
  - asocie scripts a clases.
- Implementación mínima de `ScriptLanguageExtension` (por ABI si aplica) o funciones equivalentes vía GDExtension.

## API pública: estrategia recomendada
- **Nombres y shape**: basados en C#/GDScript.
- **Implementación**: generada desde `extension_api.json` para coherencia y completitud.
- **Kotlin extensions**: solo para sugar opcional, no para API core.

## Lo que NO se puede copiar de GodotSharp
- `GCHandle`, `TieManagedToUnmanaged`, `InteropUtils` (internos, C# específico).
- Hooks nativos internos del engine.
- `partial`/`extension methods` como base de API.

## Deliverables sugeridos (fases)
### Fase 0 (core runtime)
- Registrar clases y métodos (ClassDB).
- Instancias y `object_set_instance`.
- Llamadas a métodos y retorno de `Variant`.

### Fase 1 (editor visibility)
- Ajustar init level para editor.
- ScriptLanguage mínimo: extensión + create script + reconocimiento.

### Fase 2 (paridad API)
- Construir API de `GD`, `Variant`, `StringName`, `Callable`, etc.
- Documentar convenciones JVM.

## Riesgos
- Incompatibilidades ABI entre versiones Godot.
- Registro en niveles de init incorrectos.
- Falta de ScriptLanguage → editor no reconoce scripts.
- Ciclo de vida y ownership (GC vs nativo) sin hooks internos.

## Datos del proyecto actual
- Entry point C: `godot-extension-register/gdextension_init.c` (init level `SCENE`).
- Entry point Java: `godot-java-bridge/.../GodotBridge.java`.
- FFM bridges: `BridgeContext`, `ClassDBBridge`, `ScriptInstanceBridge`.

