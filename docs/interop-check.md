# Interoperabilidad Kotlin ↔ GDScript y clases `*Extension` del motor

Medido en Godot 4.7.2 oficial (headless, Linux x86_64) con `InteropTest`
(`mi-juego-prueba/kotlin_native_game/src/nativeTest/kotlin/interop/`). El GDScript va dentro del test: llama a las
clases Kotlin de `InteropFixtures.kt` y comprueba, desde GDScript, cada valor que cruza la frontera en las dos
direcciones.

Resultado actual, igual en 3 de 3 corridas: **45 PASS y 3 FAIL a propósito** (#246 ×2 y #249). No hay crash.

## Qué funciona

| Dirección | Mecanismo | Verificado |
|---|---|---|
| GDScript → Kotlin | `@ExportMethod` | `int`, `float`, `bool`, `String` (con texto no ASCII), `Vector2`, `Vector3` como parámetros. Retornos de `int` de 64 bits, `float`, `Color`, `Array`, `Dictionary`, `PackedInt32Array` y `String`, con el tipo de Variant correcto (`typeof`). |
| GDScript → Kotlin | parámetro `Variant` | Llegan `Color`, `Array` (con o sin tipo), `Dictionary`, `PackedInt32Array`, objetos y `null`, con su tipo y valor. Un `Array` se pasa por referencia: lo que Kotlin agrega, GDScript lo ve. |
| GDScript → Kotlin | objetos | Kotlin recibe **la misma instancia** (`===`) que ya conoce: identidad canónica a través de la frontera. |
| GDScript ↔ Kotlin | `@Export` | `Int`, `String` y `Float`: valor por defecto, set y get desde GDScript, también con tipos estáticos. |
| Kotlin → GDScript | `Object.call` | Kotlin llama a un método GDScript y recibe su retorno. |
| Kotlin → GDScript | `@RegisterSignal` | La señal emitida en Kotlin llega a un handler GDScript con sus argumentos. |
| Herencia cruzada | `class GdChild extends InteropTarget` | Una clase GDScript hereda de una clase Kotlin, llama a sus métodos y sigue siendo `InteropTarget` para el motor. |

## Clases `*Extension` del motor

No son GDExtension. Son clases del motor cuyos virtuales `_xxx` se implementan para reemplazar una pieza del motor. Hay
31 en 4.7.2. Probadas en vivo, desde GDScript:

| Clase | Prueba | Resultado |
|---|---|---|
| `ImageFormatLoaderExtension` | `add_format_loader()` y luego `Image.load_from_file("*.kimg")`. Kotlin lee el archivo con el `FileAccess` que le pasa el motor y escribe los píxeles en el `Image`. | Funciona: tamaño, píxeles y número de llamadas correctos. |
| `MultiplayerAPIExtension` | `get_tree().set_multiplayer(...)`. `multiplayer.get_unique_id()`, `get_peers()`, `get_remote_sender_id()`, `Node.rpc()` y `rpc_id()`. | Funciona. Kotlin recibe el RPC con sus argumentos y lo reenvía a GDScript (`callv`). El `Error` que devuelve Kotlin llega a GDScript, y el motor llama a `_poll` en cada frame. |
| `StreamPeerExtension` | `get_available_bytes()` y `get_data()` | `_get_available_bytes` funciona. **`_get_data` nunca se llama** (#249). Godot avisa `StreamPeerExtension::_get_data is unimplemented!`. |

Hay **37 virtuales** de las `*Extension` que kogot no despacha, porque reciben punteros crudos (`const uint8_t*`), structs
nativos (`PhysicsServer2DExtensionRayResult*`, `Glyph*`) o arrays tipados de builtins (`typedarray::RID`). Se generan
igual como `open fun`, así que sobrescribirlos compila pero no hace nada (#249). Por eso, hoy no se pueden usar:

- los datos de `StreamPeerExtension`, `PacketPeerExtension`, `MultiplayerPeerExtension` y `WebRTCDataChannelExtension`;
- las consultas de `PhysicsDirectSpaceState2D/3DExtension`;
- partes de `TextServerExtension`.

El resto de las `*Extension` se despacha por completo.

## Bugs encontrados

| Issue | Severidad | Qué pasa |
|---|---|---|
| #245 | **crash** | Una llamada **tipada** de GDScript a un `@ExportMethod` (`var t := TestOne.new(); var r: int = t.addTwo(2, 3)`) crashea el motor con SIGSEGV: kogot registra los métodos con `ptrcall_func = null`. Pasa también con el `TestOne.addTwo` que ya existía, y dentro de una clase GDScript que hereda de Kotlin si sus parámetros tienen tipo. Las propiedades `@Export` y las señales no se ven afectadas. |
| #246 | alta | La metadata de los métodos pone `class_name = <clase dueña>` en todos los argumentos y el retorno, y registra un parámetro `Variant` como `OBJECT`. GDScript **no parsea** un script que pase un `int`, `Color` o `Array` a un parámetro `Variant` si conoce el tipo del receptor, aunque sea inferido (`var t = InteropTarget.new()`). |
| #247 | alta | Un parámetro `Long`, `Double`, `Color`, `Vector2i`, colección, `Dictionary` o `Packed*Array`, o un retorno `Variant`, genera un binding que no compila. Hoy los únicos tipos de parámetro que funcionan son `Int`, `Float`, `Boolean`, `String`, `Vector2`, `Vector3` y `Variant`. |
| #248 | media | No se puede escribir un `@ExportMethod` que no devuelva nada (`Unit`), ni uno con parámetros o retorno de objeto. |
| #249 | media | Los 37 virtuales de `*Extension` que no se despachan, en silencio. |
| #250 | baja | `getValue<GodotArray<Long>>()` o `getValue<Dictionary<String, Long>>()` sobre un contenedor sin tipo lanza *"Expected class GodotArray but got class GodotArray"*, y GDScript lo reporta como *"Nonexistent function"*. |

## Cómo está escrita la prueba, por los bugs de arriba

- Los objetos Kotlin se guardan en variables **sin tipo** y el objetivo principal se crea con `ClassDB.instantiate`. Con
  tipos, la escena no parsea (#246) o crashea (#245).
- Los tipos que no compilan como parámetro entran por un parámetro `Variant` (#247), y en lugar de devolver objetos se
  compara la identidad del lado Kotlin (#248).
- Las clases `*ServerExtension` no se instancian aquí: instanciar una reemplaza el singleton del motor (ver
  `docs/inheritance-matrix.md`).

Cuando se corrijan #245–#248, esta escena debería pasar a usar tipos estáticos y los tipos directos.
