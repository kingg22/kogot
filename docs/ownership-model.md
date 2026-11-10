# Modelo de Ownership: Kotlin/Native GC vs Godot RefCounted

Ver [issue #114](https://github.com/kingg22/kogot/issues/114).

## Problema confirmado (no solo teórico)

Con el binding tal como estaba antes de este cambio, se verificaron empíricamente dos bugs contra el
binario dev de Godot (`godot-version` v4.7.1, `mi-juego-prueba/kotlin_native_game`):

1. **Identidad rota**: `castTo`/`GD.load` construían una instancia Kotlin nueva en cada llamada para el
   mismo puntero nativo (`sameIdentity=false` en la prueba, dos `GD.load("res://icon.svg")` seguidos
   devolvían wrappers Kotlin distintos para el mismo `Texture2D`). Para una clase de usuario con estado
   mutable, esto significa perder ese estado silenciosamente cada vez que el mismo objeto vuelve a
   pasar por un `castTo`.
2. **Fuga de referencia por cada `GD.load`/lectura de propiedad**: cada retorno de método/lectura de
   propiedad que entrega un `RefCounted` transfiere una referencia +1 que nadie liberaba nunca
   (confirmado viendo `get_reference_count()` subir de 1 a 9 sin nunca bajar, en una prueba trivial).
3. **Double-dispose real en `freeInstanceFunc`**: `p_class_userdata` (el `StableRef<KClass>` *por
   clase*, compartido por todas las instancias) se disponía en el free de **cada instancia**. La
   segunda instancia de una misma clase (`Sprite`, `SpriteBench`, ...) que se liberara ya estaba
   re-disponiendo un `StableRef` muerto — undefined behavior garantizado en cualquier proyecto con más
   de una instancia viva de una clase que llega a liberar dos.

## Lo que se arregló en esta fase

Un registro único de identidad (`kotlin-native/binding/.../internal/binding/ObjectIdentity.kt`) sobre
`object_get_instance_binding`, con `create_callback`/`free_callback` reales (antes los tres callbacks
eran `null` en todos los call sites):

- **Identidad**: `castTo`/`castToOrNull`/`GD.load`/`instantiate<T>()` resuelven todos el mismo slot;
  nunca se fabrica una segunda instancia Kotlin para el mismo puntero nativo. La referencia guardada es
  `WeakReference` — para una clase de usuario el StableRef real ya lo sostiene `object_set_instance`
  (obligatorio para ClassDB), así que el mirror puede ser débil sin riesgo; para un wrapper puramente de
  motor (sin `create_instance_func`) es simplemente lo correcto: si nada en Kotlin lo referencia, se
  puede reconstruir en el próximo acceso.
- **Double-dispose**: `freeInstanceFunc` ya no toca `p_class_userdata`, solo el `StableRef` de la
  instancia.

Verificado corriendo `mi-juego-prueba` contra el binario dev real: misma identidad en cargas repetidas,
y las 5 instancias de `Sprite` (todas comparten `class_userdata`) se liberan sin crash.

## Fuga de referencia de `RefCounted`: resuelta para el caso común

El primer intento de resolver esto liberaba la referencia implícita de un wrapper `RefCounted`
materializado (no creado por nosotros) directo desde un `kotlin.native.ref.Cleaner`, ejecutando
`unreference()` (+ `object_destroy` si correspondía) en cuanto el wrapper se volvía inalcanzable desde
Kotlin. **Esto crasheó de forma reproducible**:

```
ERROR: The caller thread can't call the function `propagate_notification()` on this node.
Use `call_deferred()` or `call_deferred_thread_group()` instead.
handle_crash: Program crashed with signal 11
```

Los `Cleaner` de Kotlin/Native corren en un thread finalizador dedicado, separado del thread principal
donde vive Godot. Las llamadas al motor no son en general thread-safe fuera del hilo principal — esto
se disparó con un `Resource` compartido cuyo wrapper se recolectó mientras el hilo principal todavía lo
usaba.

**Lo que se implementó en esta fase** (`kotlin-native/binding/.../internal/binding/RefCountedRelease.kt`
+ una propiedad `kogotReleaseCleaner: Cleaner?` generada solo en `RefCounted`, ver
`EngineClassImplGen.kt`): el `Cleaner` que `materialize()` adjunta ya **nunca** llama al motor
directamente. Solo arma un `Callable` alrededor de `unreference()`/`object_destroy()` y lo agenda con
`Callable.callDeferred()` — que empuja a `MessageQueue`, thread-safe para encolar desde cualquier hilo
(mutex-protegido) — así que la llamada real al motor siempre termina ejecutándose en el hilo principal,
en el próximo idle frame.

Verificado corriendo `mi-juego-prueba` (`test_diag.tscn`) contra el binario dev real, incluyendo
`kotlinx.coroutines` (`Dispatchers.Default`, threads de SO reales bajo el nuevo memory model) para forzar
la colección desde un hilo creado por Kotlin, no solo el principal — **cero crashes en 5 corridas**,
incluyendo con la colección disparada concurrentemente desde un hilo de coroutine mientras el hilo
principal seguía en su loop de frames.

### Un solo modelo: cada wrapper vivo posee exactamente una referencia del motor

Lo anterior liberaba la referencia de un wrapper al recogerlo, pero solo cubría los wrappers que pasaban por
`castTo`. Las tres piezas que faltaban se resolvieron con la misma regla: **un wrapper vivo posee exactamente
una referencia del motor y la devuelve al recogerse** (`attachRefCountedRelease`, que además la registra en
`PendingReleases`, ver abajo).

- **Los métodos generados que devuelven `Ref<T>` adoptan la referencia que reciben.** Un ptrcall que devuelve
  un `Ref<T>` entrega al caller un +1 que nadie más soltará. El generador (`buildReturnRead`) envuelve el
  wrapper de todo retorno de una clase que hereda de `RefCounted` en `adoptReturnedReference(...)`, que le
  asigna ese +1 al wrapper. Antes, cada `ResourceLoader.load`, getter de propiedad, `Image.createEmpty`, etc.
  filtraba una referencia por llamada. La liberación vive ahora en `api:generated` (junto al código
  generado que la llama), no en `binding`; eso exige añadir `InternalBinding` a los `optIn` de ese módulo.
- **`castTo` (`materialize`) trata el puntero como prestado.** No puede distinguir un puntero que llegó con una
  referencia propia de uno leído de un `Variant` o recibido como argumento de un callback, así que el wrapper
  nuevo hace `RefCounted::reference()` por su cuenta (`takeOwnReference = true`). Antes asumía propiedad y
  su `Cleaner` hacía `unreference()` de una referencia que nunca se tomó, con lo que el motor liberaba el
  objeto bajo su dueño real.
- **Adquisiciones repetidas del mismo puntero vivo.** El caché de identidad colapsa `GD.load` repetidos en la
  misma instancia Kotlin, pero cada llamada trae un +1. Ya no hay nada especial que hacer: el `Resource` que
  devuelve `ResourceLoader.load` posee el suyo y el wrapper del cast posee el otro, y cada uno se devuelve
  al recogerse. (Un intento anterior de resolverlo en `GD.load` con una variante `materializeOwned` era más
  rápido de liberar pero dependía de que el call site supiera lo que recibía; se eliminó en favor de esta
  regla única.) `GD.load` y `GD.loadOrNull` son ahora un `castTo` simple.
- **Los constructores públicos de clases `RefCounted` (`Image()`, `JSON()`, ...).** Crean el objeto con
  `ClassDB.instantiate`, que devuelve una `Variant` con la única referencia; esa `Variant` no se cerraba
  nunca y el wrapper no poseía nada, así que cada `Image()` quedaba vivo hasta el final del proceso. El
  constructor generado ahora toma una referencia propia (`takeConstructedReference`), cierra la `Variant` y
  hace que el wrapper adopte esa referencia (`attachRefCountedRelease`).

Consecuencia a tener presente: las referencias se devuelven **cuando el GC recoge los wrappers** y el
`Cleaner` encola la liberación, no en el instante en que el wrapper deja de usarse. Ver el límite de
timing del `Cleaner` más abajo.

Verificado contra el binario dev real (`test_diag.tscn`), 5 de 5 corridas, sin crashes:

- `[RepeatedAcquire]`: con un wrapper ancla vivo, 9 `GD.load` repetidos + 1 `loadOrNull` fallido llevan el
  refcount de 6 a 16 y, tras recolectar, vuelve a **6**.
- `[BorrowedCast]`: un `Image` cuyo único dueño es una `Variant` pasa de refcount 1 a 2 mientras vive el
  wrapper del cast y vuelve a 1 al recogerse; la `Variant` lo libera al cerrarse.
- `[GeneratedReturn]`: 50 `Image.createEmpty()` descartados suben `Performance.OBJECT_COUNT` de 1502 a 1552 y,
  tras recolectar, vuelve a **1502** (0 de 50 vivos).
- `[Constructor]`: lo mismo con 50 `Image()`. Antes del arreglo daba `FAIL` (50 de 50 vivos al recolectar y
  `50 ObjectDB instances were leaked at exit`); después, 1502 → 1552 → **1502**.

### Referencias que Kotlin aún posee al cerrar la extensión: vaciado síncrono en `onDeInitScene`

Un wrapper que sigue alcanzable al salir nunca dispara su `Cleaner`, y una liberación diferida encolada
tarde correría cuando el motor ya apagó los servidores. En `Main::cleanup` la cola de mensajes se vacía
**antes** del deinit `SCENE` y otra vez al final, **después** de apagar los servidores, así que lo que Kotlin
aún debe tiene que saldarse de forma síncrona en el deinit `SCENE`, con todo el motor vivo.

`PendingReleases` (`kotlin-native/api/generated/.../internal/binding/PendingReleases.kt`) lleva, por puntero, cuántas
liberaciones están pendientes: una por cada wrapper vivo (`attachRefCountedRelease`). Cada unidad se consume
**exactamente una vez**, por quien llegue primero: la liberación diferida en el hilo principal (`settle`) o
el vaciado al cerrar (`drain`). Eso hace que una liberación diferida que sigue en cola cuando corre el vaciado sea un no-op y no
un `unreference()` doble. El procesador genera `releaseAllPendingRefCountedReferences()` como **primera**
línea de `onDeInitScene` (antes de desregistrar clases propias: Godot se niega a desregistrar una clase con
instancias vivas). Después del vaciado, los `Cleaner` y lo que quede en la cola no tocan el motor; `drain`
espera a que termine cualquier encolado del hilo de cleaners que ya estuviera en vuelo.

Medido contra el binario real: `threading_edge_cases.tscn` pasó de `2 ObjectDB instances were leaked` / `1
resources still in use` / `1 RID allocations ... leaked` a **cero** avisos de leak, y `test_diag.tscn` sigue
limpio, 3 de 3 corridas cada una, sin crashes. Un punto de entrada escrito a mano (como
`kotlin-native/sample`) debe llamar a esa función él mismo; y un proyecto sin ninguna clase `@Godot` no recibe
el `GeneratedBindings` generado, así que tampoco el vaciado.


### Abierto, no resuelto aquí

1. **Se asume que todo retorno de una clase `RefCounted` es un `Ref<T>`.** Es la convención de Godot y lo que
   observan los tests, pero no se verificó método por método (hay 543 en la API): un método que devolviera un
   `T*` crudo de una clase `RefCounted` haría que el wrapper devolviera una referencia que nunca recibió.
2. **`Orphan StringName: Environment` al salir.** Aparece con la extensión cargada incluso en una escena
   vacía (sin el binario de la extensión no aparece), así que sale de la inicialización de código generado,
   no del ciclo de vida de `RefCounted`. Sin localizar todavía.
3. **La liberación necesita actividad de otro thread para despachar a tiempo.** Con `mi-juego-prueba`
   corriendo prácticamente single-threaded, un `GC.collect()` síncrono en el hilo principal seguido de
   ~30 frames de espera **no** alcanza para que el `Cleaner` dispare — pero forzar la MISMA colección
   desde un hilo lanzado con `kotlinx.coroutines` (`Dispatchers.Default`) sí, de forma consistente y
   reproducible en 10 muestras seguidas (patrón exacto: `13,14,14,15,15,16,16,17,17,18` — cada paso cuya
   colección corrió desde el hilo de coroutine se libera antes del siguiente checkpoint; cada paso con
   colección puramente main-thread no). No es que la liberación falle — nunca se pierde una referencia
   para siempre, y nunca crashea — parece que el thread despachador de `Cleaner`s de Kotlin/Native
   necesita que *algo* más cree actividad de threading para que el SO le dé tiempo de CPU con prontitud en
   un proceso que si no es enteramente single-threaded. Un juego real rara vez es puramente
   single-threaded, así que se deja documentado, no perseguido más en esta pasada.

## Referencia: cómo lo resuelve Godot mismo

`modules/mono/csharp_script.cpp` (`CSharpLanguage::_instance_binding_reference_callback`) implementa
exactamente este problema para un lenguaje con GC de trazado: alterna el `GCHandle` del wrapper entre
`weak` y `strong` según el refcount nativo, usando `reference_callback` (que este fix todavía no usa —
solo hace falta para ese caso de ownership completo, no para la identidad). Es la referencia a seguir
para la Fase 2.

## Comparación con otros bindings

Investigación de código (clones locales en `/Users/kingg/IdeaProjects/`, sin ejecutar binario de Godot —
esto es lectura de fuente comparativa, no verificación empírica nueva) sobre cómo cinco bindings de
otros lenguajes resuelven los mismos tres problemas que kogot tiene abiertos: identidad de objeto,
liberación de `RefCounted` a través de threads, y construcción del wrapper concreto a partir de un
puntero de tipo dinámico desconocido en compile-time.

### godot-rust (Rust) — `godot-rust/gdext`

`Gd<T>::cast()` es una reinterpretación de puntero validada con el `is_class()` del motor
(`godot-core/src/obj/raw_gd.rs`), sin registro y sin construcción real: `Gd<T>` tiene el mismo layout
para cualquier `T`, así que "castear" nunca fabrica un objeto nuevo. No hay problema de "factory" que
resolver — es estructural, no una solución transportable a Kotlin.

### godot-dlang (D) — `godot-dlang/godot-dlang`

La configuración por defecto (no la opcional `USE_CLASSES`) representa cada clase de motor como un
**struct** de valor, no una clase con GC. `Ref!T` (`src/godot/api/reference.d`) libera el `RefCounted`
mediante un **destructor RAII determinístico** (`~this()` llama `unreference()` y, si el refcount llega
a 0, `object_destroy()`) — corre en scope-exit, en el mismo thread que lo posee, nunca en un hilo
finalizador de GC. La identidad para instancias de script se delega enteramente al
`object_get_instance_binding` del motor (sin tabla propia en D), y el casteo usa el
`classdb_get_class_tag`/`object_cast_to` del motor más una cadena de RTTI propia y minimalista — nunca
reflection nativa de D. No es transportable a Kotlin: no existe un destructor determinístico en
scope-exit para clases con GC sin forzar un bloque `use {}`, lo que rompe la ergonomía de "el objeto
vuelve de cualquier llamada a la API sin que el usuario gestione su ciclo de vida" que se busca.

### grow-graphics/gd (Go) — el precedente más directamente aplicable

Go **no** usa `runtime.SetFinalizer` para el caso común. Usa `runtime.AddCleanup`
(`internal/gdreference/object.go`), y el callback de limpieza **nunca toca el motor directamente** —
solo encola un closure en un ring buffer MPSC (`internal/ring/mpsc.go`) que se drena exclusivamente en
el hilo principal, una vez por frame (`FlushFrame`, invocado desde `startup/garbage_collector.go`). El
código cita explícitamente un crash real (issue #260) como la razón de nunca llamar al motor
directamente desde un hilo en background — la misma clase de bug que forzó revertir el intento de
`Cleaner` de kogot. Un detalle que el borrador anterior de este documento no capturaba: la cola debe
preservar **orden** — un free pendiente debe quedar encolado *detrás* de cualquier otra llamada
diferida que todavía toque ese mismo objeto, no solo "eventualmente" ejecutarse en el hilo principal.
Como Godot's `MessageQueue`/`call_deferred()` ya es una única cola FIFO drenada cada frame, encolar la
liberación como un `call_deferred()` (el camino ya propuesto arriba) hereda ese orden gratis, sin
necesitar un ring buffer propio como el de Go. Para `RefCounted`, Go además chequea el valor de retorno
de `Unreference()` antes de decidir destruir — igual que la referencia de Mono citada arriba. La
identidad vía caché solo existe para instancias *autoría-Go* (clases registradas con
`classdb.RegisterClass`), indexada por el puntero de instancia de GDExtension — para clases de motor
planas no hay caché, son structs de valor libremente reconstruibles. El casteo tampoco usa un registro
por nombre: `T` lo aporta el call site (generics), verificado con el mismo chequeo de class-tag del
motor.

### godot_dart (Dart) — mismo tipo de bug que kogot ya revirtió, no un modelo a copiar

`godot_dart` usa `NativeFinalizer`/`Dart_WeakPersistentHandle`, pero para `RefCounted` llama
`unreference()`/`object_destroy()` **directo y sincrónico desde el finalizador**, sin ningún salto de
hilo — exactamente lo que kogot intentó y revirtió por el crash. Detecta contexto de finalizador
(`Dart_CurrentIsolate() == nullptr`) pero solo lo usa para diferir la contabilidad de fuerza de handle
Dart (weak/strong) a un paso de mantenimiento por frame, nunca la llamada real al motor. El propio
código tiene un `TODO`/`assert(false)` sin resolver para el caso en que esto falla. Esto **no** es
evidencia de que llamar al motor desde un finalizador sea seguro — es el mismo riesgo latente, solo que
no lo han disparado (o lo relativizan asumiendo que el refcount es atómico). No se recomienda como
modelo.

Donde Dart sí es relevante: para castear un puntero de tipo dinámico desconocido a un wrapper concreto,
usa un `TypeResolver` (`lib/src/core/type_resolver.dart`) generado en compile-time —
`Map<String, TypeInfo>` con un tear-off de constructor por clase
(`constructFromGodotObject: (ptr) => ClassName.withNonNullOwner(ptr)`) — la misma forma que el
`TypeManager` de godot-kotlin-jvm (utopia-rise) investigado antes. Dos bindings diseñados de forma
independiente, ambos para lenguajes con objetos reales de heap y constructores reales (igual que
Kotlin/Native), convergieron en el mismo patrón de registro nombre→constructor. Go, D y Rust no lo
necesitan por una razón estructural que no aplica a Kotlin: sus wrappers son de layout uniforme (un
struct que es solo puntero + tipo fantasma, o un reinterpret-cast), así que una única función genérica
construye cualquier `T` sin tabla de búsqueda. Kotlin/Native no tiene ese truco disponible (clases reales,
sin invocación de constructor por tipo en runtime sin `kotlin-reflect`), así que su situación es la de
Dart/JVM, no la de Go/D/Rust.

### Conclusiones para kogot

1. **Liberación de `RefCounted` (issue #114, parte 2, implementada y validada arriba)**: se siguió el
   patrón de Go, no el de Dart. Nunca llamar al motor directamente desde el `Cleaner`; encolar la liberación
   (vía `Callable.callDeferred()`/`MessageQueue`, que ya da orden FIFO gratis) y dejar que el hilo principal
   la ejecute. Dart demuestra que la alternativa directa-desde-finalizador es el mismo riesgo que ya se
   revirtió aquí, no una prueba de que sea segura.
2. **Eliminar el parámetro `factory`**: la convergencia independiente de Dart y godot-kotlin-jvm en un
   registro nombre→constructor generado en compile-time (no reflection) es la señal más fuerte de que
   ese es el diseño correcto para Kotlin específicamente — no es solo copiar a utopia, es el patrón que
   emerge en cualquier binding con objetos reales de heap y constructores reales.
3. La conclusión 1 sí se validó contra el binario real de Godot (ver las secciones de arriba). La 2 sigue
   siendo investigación comparativa de código fuente, pendiente de implementación y prueba empírica.
