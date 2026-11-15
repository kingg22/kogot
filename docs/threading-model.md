# Modelo de threads: callbacks de GDExtension vs Kotlin/Native

Ver [issue #113](https://github.com/kingg22/kogot/issues/113). Complementa [`ownership-model.md`](ownership-model.md) (#114).

## Punto de partida

- **Kotlin/Native (nuevo memory model)** no tiene objetos congelados ni confinamiento por thread: cualquier objeto
  Kotlin se puede usar desde cualquier thread, y un thread del motor que Kotlin no creó se adjunta al runtime solo, en
  su primer callback. El riesgo no es el runtime de Kotlin, sino el **estado mutable compartido** del binding y las
  **reglas de threads del propio Godot**.
- **Godot sí llama a Kotlin desde threads que no son el principal.** No es un caso raro, es parte de su API:

| Entrada al binding | Threads que la invocan |
|---|---|
| `_process` / `_physics_process` y sus notificaciones | worker threads si el nodo usa `process_thread_group = SUB_THREAD` |
| `ResourceFormatLoader._load`, `_get_resource_type`, `_handles_type`, constructores de recursos | `WorkerThreadPool` durante `ResourceLoader.load_threaded_request` y el escaneo del editor |
| `call_func` y demás trampolines de un `Callable` propio | el thread que lo llama: `WorkerThreadPool.add_task`, `Thread.start`, una señal emitida desde un thread (conexión directa) |
| `free_instance`, `free` de un `Callable`, free del slot de identidad | el thread que suelta la última referencia o destruye el objeto |
| `get_virtual` | el primero que llama al virtual (se resuelve de forma perezosa) |
| init / deinit de la extensión | siempre el principal |

## Decisión: no confinar al thread principal

godot-rust, por defecto, prohíbe usar el binding fuera del thread principal (pánico en `ensure_main_thread`) y
exige el feature `experimental-threads` para lo demás. kogot **no** hace eso: los thread groups y la carga threaded son
API oficial de Godot, y confinar al principal prohibiría usos válidos. En su lugar:

1. **Ninguna excepción de Kotlin cruza la frontera C.** En Kotlin/Native una excepción que escapa de un
   `staticCFunction` termina el proceso entero, sin traza en Godot. Todo callback (los escritos a mano, los
   trampolines generados por codegen y por el processor, los de `Callable`) corre dentro de `guardCallback`
   (`kotlin-native/runtime/.../CallbackGuard.kt`): la excepción se reporta con `print_error_with_message` (seguro desde
   cualquier thread) y el callback devuelve un valor neutro (`FALSE`, `null`, `INVALID_METHOD` en llamadas a métodos,
   el valor por defecto del motor en un virtual).
2. **El estado interno del binding es thread-safe.** Ver la tabla de abajo.
3. **Las llamadas al motor desde Kotlin siguen las reglas de Godot, no unas propias.** Un `Node` dentro del árbol solo
   se toca desde su thread (Godot lo hace cumplir con `ERR_THREAD_GUARD`); recursos y servidores, según su
   documentación. kogot no añade ni quita restricciones a eso: el usuario de Kotlin tiene las mismas reglas que GDScript
   o C++.
4. **Las liberaciones que el binding hace por su cuenta nunca corren en el thread de los `Cleaner`.** Ver abajo.

## Estado interno compartido

| Estado | Protección |
|---|---|
| Slot de identidad (`BindingSlot`, `ObjectIdentity.kt`) | Godot crea el slot una sola vez bajo su mutex de instance bindings; el wrapper se publica con compare-and-set, **después** de asentar su referencia, así que dos primeros accesos simultáneos coinciden en un único wrapper canónico y nadie ve un wrapper `RefCounted` a medio construir |
| `PendingReleases` | spin lock propio; el vaciado espera a los encolados en vuelo |
| Caché de `WrapperResolver` | copy-on-write con compare-and-set |
| `internedNames` de `KotlinScriptInstance` | copy-on-write con compare-and-set; el perdedor de una carrera cierra su `StringName` |
| `lazy` de method binds y singletons generados | `PUBLICATION`: la inicialización puede repetirse, se publica un solo valor |

## Liberación de referencias por thread

- **`Cleaner` de un wrapper `RefCounted`**: corre en el thread finalizador de Kotlin/Native. Nunca llama al motor
  desde ahí (eso crasheó, ver `ownership-model.md`): encola la liberación con `Callable.callDeferred()`. Desde un
  thread sin cola propia, `MessageQueue::get_singleton()` es la cola principal (protegida con mutex), así que el
  `unreference()` real ocurre en el thread principal, en el siguiente flush.
- **Referencia sobrante de un retorno `Ref<T>`** (`dropReturnedReference`): se devuelve en el thread que la recibió,
  igual que haría el destructor de `Ref<T>` en C++. `unreference()` es atómico.
- **Vaciado al cerrar** (`releaseAllPendingRefCountedReferences`): en el deinit `SCENE`, siempre en el principal; si
  alguna vez se llamara desde otro, se reporta (no se omite: omitirlo filtraría todo lo pendiente).

## Saber cuál es el thread principal

La interfaz GDExtension no tiene una consulta "es el thread principal", y `Thread.is_main_thread()` es una llamada
al motor. `MainThread` (`kotlin-native/runtime/.../MainThread.kt`) hace lo mismo que el `is_main_thread` de
godot-rust: guarda en un `@ThreadLocal` el thread que ejecuta la inicialización de la extensión (Godot siempre
inicializa en el principal; se marca en cada nivel, así vale para cualquier nivel mínimo declarado). La consulta es
una lectura thread-local, gratis y válida en cualquier thread. `MainThread.check(operación)` reporta un error de
Godot si se llama fuera del principal, sin lanzar.

## Límites conocidos, no resueltos aquí

- **Destrucción concurrente.** Si un thread destruye un objeto mientras otro lo está materializando, el segundo lee un
  slot ya liberado. Es la misma regla que en C++: un objeto no se usa desde un thread mientras otro lo destruye.
- **`call_deferred` durante una carga threaded.** Godot instala una cola propia en el thread de carga y la vacía al
  terminar `_load`, en ese mismo thread: lo diferido desde ahí no corre en el principal.
- **`KotlinScriptRegistry`** se lee desde `_load` en workers y se vacía en el deinit; una carga threaded en vuelo
  durante el cierre de la extensión no está sincronizada.
- **Los `Cleaner` pueden tardar** en despachar en un proceso casi single-threaded (ver `ownership-model.md`).

## Verificación

Godot 4.7.2 oficial, headless, comparado contra `main`: mismas escenas, mismos resultados (`signals_check` 48/48,
`get_value_check` 102, `collections_check` 48, `virtual_returns_check` 4, `downcast_check` 21 + 1 en rojo a propósito
por #212, `threading_edge_cases` limpio). Tests nativos de `MainThread` en `:kotlin-native:runtime:linuxX64Test`.
