# Corrutinas: dispatcher del hilo principal de Godot y adaptadores `suspend`

Prototipo en el proyecto de prueba (`mi-juego-prueba/kotlin_native_game/src/nativeMain/kotlin/GodotCoroutines.kt`),
comprobado en vivo por `coroutines_check.tscn`. Todavía no es API del binding.

## Qué hace el binding de Go (graphics.gd), como referencia

- **`Extension`** es su manera de declarar una clase propia que extiende una clase del motor. Go no tiene herencia, así
  que se embebe: `type Player struct { CharacterBody2D.Extension[Player] }`, y se llega a la "superclase" con
  `AsNode()`, `AsCharacterBody2D()`, etc. (`internal/gdclass/extension.go`). Es el equivalente de nuestro
  `@Godot class Player(p) : CharacterBody2D(p)`. No tiene relación con las clases `*Extension` del motor
  (`TextServerExtension`, ...), que en Go se usan de la misma forma.
- **Hilos.** Una goroutine que no está en el hilo principal no llama al motor: **encola** la llamada en un anillo MPSC
  (`internal/ring`), y el hilo principal lo vacía en cada frame (`EveryFrame`), en orden FIFO.
- **`main()` como corrutina.** `startup.LoadingScene()` / `startup.Scene()` / `startup.Rendering()` convierten el
  `main` de Go en una corrutina que el motor reanuda en cada frame (`iter.Pull`). El código de usuario se escribe de
  forma secuencial y bloquea "hasta el siguiente frame".

El prototipo de kogot aplica la misma idea con las corrutinas de Kotlin.

## El prototipo

| Pieza | Qué es |
|---|---|
| `GodotMain` | Un `MainCoroutineDispatcher`. `dispatch()` es un push sin locks (pila de Treiber), seguro desde cualquier hilo, y el hilo principal lo vacía en orden FIFO una vez por frame. `GodotMain.immediate` ejecuta en el mismo frame si ya se está en el hilo principal. |
| `GodotMainLoop.register()` | Registra `frame_func` y `shutdown_func` con `register_main_loop_callbacks`, de GDExtension 4.5+. `frame_func` corre en cada frame, después de todos los `_process()`. No hace falta ningún nodo ni `call_deferred`. |
| `GodotScope` | `SupervisorJob() + GodotMain + CoroutineExceptionHandler`. `shutdown_func` lo cancela mientras el motor sigue vivo, así las corrutinas suspendidas sueltan sus conexiones. |
| `TypedSignal<() -> Unit>.await()`, `TypedSignal<(P1) -> Unit>.await(): P1` | Equivalen al `await signal` de GDScript. Conexión `ONE_SHOT`, siempre liberada: al emitirse la señal, o en el hilo principal si la corrutina se cancela antes. |
| `SceneTree.nextFrame()`, `SceneTree.wait(seconds)` | Equivalen a `await get_tree().process_frame` y a `await get_tree().create_timer(s).timeout`. |

```kotlin
override fun _ready() {
    GodotScope.launch {
        val data = withContext(Dispatchers.Default) { loadHeavyData() } // fuera del hilo principal
        label.text = data.title                                        // de vuelta en el hilo principal
        getTree().wait(0.5)
        val hit = enemy.hit.await()                                    // espera la señal y su argumento
        playEffect(hit)
    }
}
```

## Resultados (Godot 4.7.2 oficial, headless, 3 de 3 corridas iguales)

`coroutines_check`: **15 de 15 PASS**, exit 0, sin `ERROR`, sin fugas ni huérfanos al salir (`--verbose`), ~7,5 s.

| Comprobación | Resultado |
|---|---|
| `frame_func` registrado después del arranque (desde un `_ready`) corre igual en cada frame | sí (se pierde `startup_func`, que aquí no importa) |
| `Dispatchers.Main` de kotlinx en Linux nativo | no existe: `NotImplementedError: Dispatchers.Main is missing on the current platform` |
| Una corrutina de `GodotScope` corre en el hilo principal (`MainThread.isCurrent`) | sí |
| `withContext(Dispatchers.Default)` corre fuera del hilo principal y vuelve a él; después se puede tocar un nodo del árbol | sí |
| `nextFrame()` reanuda exactamente en el frame siguiente | sí |
| `GodotMain.immediate` no espera un frame | sí |
| `wait(0.1)` suspende unos 100 ms | sí |
| `await` de `Timer.timeout` y de una señal propia con argumento | sí, con el valor emitido |
| Cancelar una corrutina suspendida en una señal la desconecta | sí (`hasConnections` vuelve a `false`) |
| 1000 corrutinas lanzadas en `GodotMain` corren en orden FIFO | sí |
| 8 hilos × 1000 saltos a `GodotMain`: ninguna actualización perdida, sin locks | sí (confinamiento al hilo principal) |
| `delay()` reanuda en el hilo principal | sí |
| Una excepción se reporta y no cancela el scope | sí |
| Una corrutina suspendida al salir se cancela en `shutdown_func` y suelta su conexión | sí, sin errores al salir |

## Sobre "setMain"

- En Linux, Windows y Android nativos, kotlinx.coroutines **no tiene** `Dispatchers.Main`. En Apple, `Dispatchers.Main`
  es la cola principal de GCD; no lo probé, y depende de que Godot procese el run loop de Cocoa.
- `Dispatchers.setMain(...)` existe solo en `kotlinx-coroutines-test` y está pensado para tests. No está en la caché de
  dependencias del repo, así que no lo probé. Sirve para que el código que usa `Dispatchers.Main` funcione en tests
  (`Dispatchers.setMain(GodotMain)`), pero no conviene en producción.
- Por eso la propuesta es que kogot publique su propio `MainCoroutineDispatcher` (algo como `Dispatchers.Godot`), como
  hace `kotlinx-coroutines-swing` con `Dispatchers.Swing`. El usuario escribe `withContext(Dispatchers.Godot)`.

## Costos y límites medidos

- **Cada salto desde otro hilo espera un frame**, igual que `call_deferred`. 8000 saltos secuenciales en 8 hilos
  tardaron ~1000 frames. Para trabajo seguido en el hilo principal conviene agrupar en un solo `withContext`, o usar
  `GodotMain.immediate` cuando ya se está en el hilo principal.
- Solo hay **un** `register_main_loop_callbacks` por biblioteca. Si kogot lo adopta, debe registrarlo el binding
  durante la inicialización, no el usuario.
- **Nodos fuera del hilo principal** (`process_thread_group = SUB_THREAD`): `GodotMain` siempre los reanuda en el hilo
  principal. Un dispatcher por grupo de hilos queda fuera del prototipo.
- **El motor no espera a las corrutinas**: un `_ready` que lanza una corrutina vuelve enseguida, igual que en GDScript
  con `await`.

## Próximos pasos propuestos

Llevar el prototipo al binding (#252):

1. `Dispatchers.Godot` con su `immediate`, registrado en la inicialización.
2. Un scope con el ciclo de vida de la extensión, y uno por nodo (`Node.coroutineScope`) que se cancela en
   `NOTIFICATION_PREDELETE`.
3. Adaptadores `await` para todas las aridades de `TypedSignal`, generados como los `connect`.
4. `ResourceLoader.loadThreaded` como `suspend`, y otras APIs asíncronas del motor.
