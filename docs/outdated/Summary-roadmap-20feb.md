# Godot JVM (godot-java) — Resumen del estado actual
Fecha: 2026-02-20

## 1) Qué es y cómo está organizado

El proyecto implementa un bridge Java ↔ Godot 4 usando GDExtension + JNI + Project Panama (FFM).

Componentes principales:

* GDExtension nativo (C)

  Implementado en `godot-extension-register/gdextension_init.c.`
Se compila como librería compartida gdjava y expone el entrypoint godot_java_bridge_init (ver `gdjava.gdextension`).

* Bridge Java

  Implementado en godot-java-bridge (ej.: `godot-java-bridge/src/main/java/.../GodotBridge.java`).
Recibe punteros de Godot desde C, inicializa el lado Java y ejecuta lógica de arranque.

* Bindings FFM (Panama)

  godot-java-binding contiene miles de wrappers generados en `.../internal/ffm/` para invocar la API de GDExtension desde Java.

* Empaquetado / Addon Godot

  Los artefactos se copian a lib/ en la raíz y luego se consumen desde el addon en
`mi-juego-prueba/addons/java_gdext/` (contiene `gdjava.gdextension`, `.so` y _JARs_).

## 2) Flujo de ejecución (runtime)

Basado en `godot-extension-register/gdextension_init.c`:

1. Godot carga la GDExtension (entrypoint `godot_java_bridge_init`).
2. Se guardan `get_proc_address` y el library de Godot en globals.
3. En `initialize_jvm()`:
   * Se busca `libjvm` (primero JRE embebido, luego JAVA_HOME).
   * Se construye el classpath leyendo `addons/java_gdext/bin/lib/*.jar`.
   * Se crea la JVM con:
   - `--enable-native-access=ALL-UNNAMED`
   - `-Xms128m -Xmx1024m`
   - `-XX:+UseG1GC`
   - `-agentlib:jdwp=...` en debug
4. Se llama a `GodotBridge.initialize(long, long)` con punteros de GDExtension.
5. En `GodotBridge.initialize()`:
   * Convierte punteros a `MemorySegment`.
   * Se ejecuta la lógica de arranque (logs, configuración, managers, etc.).
6. En shutdown (`cleanup_jvm()`):
   *  Llama `GodotBridge.shutdown()`, destruye la JVM y libera la librería.

## 3) Estado actual según tus logs

Los logs indican que todo el pipeline de carga funciona correctamente:
* Godot carga el addon.
* La JVM se crea con JNI.
* Se arma classpath desde addons/java_gdext/bin/lib.
* GodotBridge.initialize() se ejecuta y finaliza sin error.
* Luego se dispara shutdown() y se destruye la JVM.

Eso confirma:
- ✅ La GDExtension se carga bien
- ✅ La JVM se crea desde JAVA_HOME
- ✅ Los JARs se encuentran correctamente
- ✅ El bridge Java se inicializa y ejecuta

## 4) Roadmap sugerido (próximos pasos)

**Corto plazo (estabilización)**
* Unificar logs en Java con una clase de logger propia o System.out.
* Revisar el branch Windows de build_classpath_from_dir() (tiene paths y realloc sin crecimiento; además hay un log_error sin ; en esa rama).

**Medio plazo (integración real con Godot)**
* Implementar bindings para registrar clases y métodos GDExtension desde Java (ClassDB, script instances).
* Exponer APIs para crear Nodes Java y recibir callbacks desde Godot.
* Incluir un sistema de “bootstrap” de scripts (ej: detectar clases Java anotadas y registrarlas).

**Largo plazo (experiencia dev / tooling)**
* Automatizar build + copia al addon (gradle task que sincronice con mi-juego-prueba/addons/java_gdext).
* Agregar gradle tasks para generar bindings desde extension_api.json.
* Soporte multi-plataforma (Linux/Windows/Mac) con empaquetado de JRE embebida.
