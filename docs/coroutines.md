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

## Cómo declaran su `Dispatchers.Main` Swing y Android (kotlinx.coroutines 1.10.2, desde los sources)

Revisé `kotlinx-coroutines-swing`, `kotlinx-coroutines-android`, `kotlinx-coroutines-core` (común, nativo y JVM) y
`kotlinx-coroutines-test`.

| Pieza | Swing (`SwingDispatcher.kt`) | Android (`HandlerDispatcher.kt`) |
|---|---|---|
| Clase | `sealed class SwingDispatcher : MainCoroutineDispatcher(), Delay` | `sealed class HandlerDispatcher : MainCoroutineDispatcher(), Delay` |
| `dispatch` | `SwingUtilities.invokeLater(block)`, la cola de eventos de AWT | `handler.post(block)`, el `Looper` principal. Si se rechaza porque el looper terminó, cancela el contexto y ejecuta el bloque en `Dispatchers.IO` (`cancelOnRejection`), así no se pierde. |
| `immediate` | otro objeto, con `isDispatchNeeded = !SwingUtilities.isEventDispatchThread()` | `HandlerContext(..., invokeImmediately = true)`, con `isDispatchNeeded = Looper.myLooper() != handler.looper` |
| `Delay` nativo | `scheduleResumeAfterDelay` / `invokeOnTimeout` con un `javax.swing.Timer`; al cancelar se detiene el timer | `handler.postDelayed`; al cancelar, `removeCallbacks` |
| Registro como `Dispatchers.Main` | `SwingDispatcherFactory : MainDispatcherFactory` (`loadPriority = 0`) en `META-INF/services/kotlinx.coroutines.internal.MainDispatcherFactory` | `AndroidDispatcherFactory` (`loadPriority = Int.MAX_VALUE / 2`), mismo archivo de servicios. Su `hintOnError()` sugiere `Dispatchers.setMain` para tests. |
| Acceso con nombre propio | `val Dispatchers.Swing: SwingDispatcher` | `Handler.asCoroutineDispatcher()` |
| Extra | inicializar en headless lanza una excepción a propósito, para no usarse como fuente de `delay` | `awaitFrame()`: suspende hasta el siguiente frame de `Choreographer`, el equivalente a nuestro `nextFrame()` |

**Cómo se elige `Dispatchers.Main`:**

- **JVM** (`core-jvm/jvmMain/internal/MainDispatchers.kt`): `ServiceLoader` busca todas las `MainDispatcherFactory` del
  classpath y usa la de mayor `loadPriority`. Así es como Swing, JavaFX y Android "se instalan" con solo agregar la
  dependencia. Para el binding JVM/FFM de kogot, la vía sería esta misma: una `GodotDispatcherFactory` en
  `META-INF/services`.
- **Nativo** (`nativeMain/Dispatchers.kt`): no hay `ServiceLoader`. `Dispatchers.Main` es
  `injectedMainDispatcher ?: mainDispatcher`. Fuera de Apple, `mainDispatcher` es `MissingMainDispatcher`, y toda
  llamada lanza `NotImplementedError: Dispatchers.Main is missing on the current platform`. En Apple
  (`nativeDarwinMain`) es `DarwinMainDispatcher`, que usa `dispatch_async(dispatch_get_main_queue())` y `isMainThread()`
  vía `CFRunLoopGetCurrent() == CFRunLoopGetMain()`.
- **`Dispatchers.setMain`** (`kotlinx-coroutines-test`, `nativeMain/internal/TestMainDispatcher.kt`) envuelve el
  dispatcher en un `TestMainDispatcher` y lo instala con `Dispatchers.injectMain(...)`. `injectMain` es
  `@PublishedApi internal`, y el módulo de test lo llama con `@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")`.

### Experimento: `Dispatchers.Main = GodotMain` en Linux nativo

Llamé a `Dispatchers.injectMain(GodotMain)` igual que lo hace el módulo de test (el archivo del experimento no se
commiteó). Resultado en vivo: `Dispatchers.Main` pasa a ser `Godot.Main`, `withContext(Dispatchers.Main)` desde
`Dispatchers.Default` corre en el hilo principal, y `Dispatchers.Main.immediate` no espera un frame. `coroutines_check`
dio 16 de 16.

**Pero no sirve para producción:**

- Kotlin 2.5 avisa: *"Suppression of error 'INVISIBLE_REFERENCE' might compile and work, but the compiler behavior is
  UNSPECIFIED and WILL NOT BE PRESERVED"*. El aviso no se puede silenciar, así que con `allWarningsAsErrors` del repo
  no compila; para el experimento lo desactivé temporalmente en el módulo de ejemplo.
- `injectMain` es API interna: puede cambiar en cualquier versión de kotlinx.coroutines, y `injectedMainDispatcher` es
  un `var` sin sincronización.
- Usar `kotlinx-coroutines-test` en producción solo para `setMain` mete en el juego un dispatcher de test como
  envoltorio.

### Cómo lo resuelve el resto del ecosistema en Kotlin/Native

Busqué en GitHub proyectos de Kotlin/Native que necesitan un hilo principal propio (UI o bucle de juego) en Linux o
Windows, y revisé kotlinx.coroutines 1.11.0, la versión más reciente. Hay cuatro enfoques, y **no existe una vía
pública para que `Dispatchers.Main` sea propio en nativo**:

| Enfoque | Quién lo usa | `Dispatchers.Main` funciona | Costo / riesgo |
|---|---|---|---|
| 1. `Dispatchers.injectMain(d)` con `@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")` | `kotlinx-coroutines-test` mismo; `rachel-ylcs/ylcs-kmp` (port de Compose a Linux/Windows nativo), que lo inyecta con `@EagerInitialization` antes de que nadie capture `Dispatchers.Main` y deja intacto el de Darwin | sí (probado aquí: 16/16) | API interna; Kotlin 2.5 avisa *"compiler behavior is UNSPECIFIED and WILL NOT BE PRESERVED"* y el aviso no se puede silenciar, así que con `allWarningsAsErrors` no compila |
| 2. `Dispatchers.setMain(d)` de `kotlinx-coroutines-test` **en producción** | `bitsycore/compose-desktop-native` (Compose sobre SDL3): `implementation(libs.kotlinx.coroutines.test)` en el código principal, `setMain` al arrancar y `resetMain` al cerrar | sí (probado aquí: 16/16, y `Dispatchers.Main.immediate` delega en `Godot.Main.immediate`) | API pública (`@ExperimentalCoroutinesApi`), pero mete la biblioteca de test en el juego. Su `TestMainDispatcher` crea un `Throwable` en **cada** lectura del delegado: 100 000 `isDispatchNeeded` costaron ~500–550 ms contra ~1,2–1,6 ms de `GodotMain` directo (unos 5 µs por llamada, ~350×, en build debug). Por debajo usa el mismo `injectMain`. |
| 3. Reemplazar el runtime de coroutines (un `actual object Dispatchers` propio en el paquete `kotlinx.coroutines`) | `DrewCarlson/klio` (`KlioRuntime.kt`), `brokenhappy/kotlinhax.shadowroutines` | sí | Es un fork: choca con cualquier otra biblioteca que dependa del kotlinx.coroutines real |
| 4. Dispatcher propio con nombre, sin tocar `Main` | KorGE (`GameWindowCoroutineDispatcher`: cola por frame con presupuesto de tiempo y `Delay` propio con tareas temporizadas), `sobotkami/kotlinx-gtk` (`Dispatchers.UI` sobre `g_idle_add`) | no | Seguro y estable, pero las bibliotecas de terceros que usan `Dispatchers.Main` (Compose, `androidx.lifecycle`, `viewModelScope`, ...) no funcionan |

**Datos que aportan esos proyectos:**

- `compose-desktop-native` explica por qué `immediate` tiene que ser real: `androidx.lifecycle` (KMP) comprueba el hilo
  haciendo un viaje de ida y vuelta por `Dispatchers.Main.immediate`, y con un `Main` que solo encola, la app se queda
  colgada en un *deadlock* (0 % de CPU). También confirma que en Darwin la cola principal de GCD no se vacía si el bucle
  no procesa el run loop.
- `ylcs-kmp` encola lo que llega antes de que exista el host de UI, y después del cierre cancela la tarea y la manda a
  `Dispatchers.Default`, igual que el `cancelOnRejection` de Android.
- El pedido de una API pública ya existe:
  [Kotlin/kotlinx.coroutines#4286](https://github.com/Kotlin/kotlinx.coroutines/issues/4286) *"Provide a Public API to
  Set a Custom Main Coroutine Dispatcher"* (abierto desde 2024-11, de alguien que hace bindings de GTK). Propone
  `Dispatchers.setMain` con opt-in.
- En 1.11.0, `nativeMain/Dispatchers.kt` es igual que en 1.10.2. `MainDispatcherFactory` existe en `commonMain`, pero
  solo la JVM la carga.

**Respuesta corta:** en nativo, la **única** forma de que el `Dispatchers.Main` estándar sea el de Godot, sin forkear
coroutines, es `injectMain`: directo (frágil y sin compilar con `-Werror`) o a través de `setMain` de
`kotlinx-coroutines-test` (público, pero con costo por llamada y una dependencia de test en el juego).

### Conclusión para kogot

1. Publicar `val Dispatchers.Godot: GodotDispatcher`, como `Dispatchers.Swing`, sobre una
   `sealed class GodotDispatcher : MainCoroutineDispatcher(), Delay`.
2. Que `immediate` sea otra instancia con `isDispatchNeeded = !MainThread.isCurrent`, como en Swing y Android.
3. Implementar `Delay` con timers del motor o con una cola por frame en lugar del hilo de `DefaultDelay`. Hoy el
   prototipo no lo implementa: `delay()` funciona, pero cuenta el tiempo en un hilo de kotlinx y luego despacha.
4. Copiar el `cancelOnRejection` de Android: después de `shutdown_func`, un `dispatch` debe cancelar el contexto en
   lugar de dejar la tarea en una cola que ya nadie vacía.
5. Por defecto, no tocar `Dispatchers.Main` en nativo. Ofrecer una instalación **opt-in** (algo como
   `Dispatchers.installGodotAsMain()`) para quien use bibliotecas que dependen de `Dispatchers.Main` (Compose,
   lifecycle), con su costo documentado. Mientras #4286 no se resuelva, la opción defendible es `setMain` de
   `kotlinx-coroutines-test` en un módulo aparte de kogot, no `injectMain` con supresiones en el binding. En JVM/FFM, sí
   registrar una `MainDispatcherFactory`.
6. En macOS, `Dispatchers.Main` de kotlinx usa la cola principal de GCD. Funcionaría solo si Godot procesa el run loop
   de Cocoa, que en `--headless` no está garantizado. No lo probé.

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
