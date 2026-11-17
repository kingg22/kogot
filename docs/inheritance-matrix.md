# Matriz de herencia: qué clases del motor puede heredar una clase `@Godot`

Medido en Godot 4.7.2 oficial (headless, Linux x86_64) sobre las 1036 clases de `godot-version/v4_7_2/extension_api.json`.
Dos preguntas distintas:

1. **¿Qué permite Godot?** Una extensión mínima en C, sin kogot ([`research/inheritance-matrix/matrix.c`](research/inheritance-matrix/matrix.c)),
   registra una clase `KX_<Padre>` por cada clase del motor y la instancia.
2. **¿Qué permite kogot?** Una clase `@Godot class Matrix<Padre>(nativePtr) : <Padre>(nativePtr)` por cada clase que la API
   generada deja extender (`InheritanceMatrix.kt`, generado por `mi-juego-prueba/tools/generate_inheritance_matrix.py`
   en el source set de tests), compilada con KSP y comprobada dentro de Godot por `InheritanceTest`.

## Resultado

| Lo que genera kogot | Clases | Godot acepta como padre | ¿Correcto? |
|---|---|---|---|
| `open` + constructor `(nativePtr)` público | 878 (811 core, 67 editor) | 875. Las 3 que no son solo de Android (`JavaClass`, `JavaObject`, `JNISingleton`) | Sí, salvo #239, #240 y #241 |
| `open` + constructor `internal` (no instanciables con hijas: `CanvasItem`, `CollisionObject2D`, `Light2D`, `Shape2D`, `InputEvent`, `Viewport`, ...) | 80 | ninguna: *"cannot extend native abstract class"* | Sí: Kotlin las rechaza al compilar (mensaje poco claro, #243) |
| `final` | 37 | solo las 7 de `Tween` / `*Tweener` | Sí: esas solo se crean con `Node.createTween()` |
| singletons | 41 | 26 (`OS`, `Engine`, `ClassDB`, ...) | Sí: heredar un singleton no tiene sentido; las extensiones usan las `*Extension` (`PhysicsServer2DExtension`, ...) |

Con kogot, `./gradlew :mi-juego-prueba:kotlin_native_game:macosArm64Test --tests '*InheritanceTest'` da hoy:

| Test | Resultado |
|---|---|
| `subclassesAreRegisteredWithTheirEngineParent` | pasa: las 802 clases soportadas están registradas con su padre |
| `subclassesAreInstantiatedThroughTheKspFactory` | pasa: 799 se instancian por la fábrica de KSP (3 extensiones de servidor no se instancian, ver el test) |
| `subclassesOfClassesRenamedInKotlinAreNotRegistered_239` | pasa **mientras el bug exista** (`assertKnownBug`): JSON, JSONRPC, Object, Range, UPNP y XRVRS no se registran |
| `subclassesOfEditorClassesAreNotRegistered_241` | pasa mientras el bug exista: EditorPlugin no se registra |

Los dos últimos fijan un bug abierto con `assertKnownBug`: el cuerpo es la aserción correcta y el test pasa porque hoy falla.
El día que se corrija, el cuerpo pasará y el test fallará avisando de quitar el envoltorio y dejar la aserción normal.

Instanciar una clase quiere decir que `ClassDB.instantiate("MatrixX")` pasa por la fábrica que genera KSP, que el objeto es la
instancia Kotlin `MatrixX` y que el motor lo sigue viendo como un `X` (`is_class`). Las clases de esos dos tests no se
registran, y Godot lo avisa al arrancar y al salir de cada proceso de test; el lanzador no lo muestra porque ocurre fuera del
runner.

## Bugs encontrados

| Issue | Qué pasa |
|---|---|
| #239 | El procesador registra el padre con el **nombre Kotlin**. Las 6 clases que el generador renombra (`Object`→`GodotObject`, `Range`→`GodotRange`, `JSON`→`Json`, `JSONRPC`→`Jsonrpc`, `UPNP`→`Upnp`, `XRVRS`→`Xrvrs`) fallan con *"using non-existing parent class 'GodotRange'"*. |
| #240 | El padre es el **primer supertipo escrito**. `class Foo(p) : GodotNotification, Node(p)` registra `"GodotNotification"` como padre y el binding generado no compila. No forma parte de la matriz, porque rompería la compilación del proyecto de prueba. |
| #241 | Las clases de **editor** (67 extensibles, entre ellas `EditorPlugin`) no existen cuando kogot registra sus clases, en `onInitScene`: Godot las crea en el nivel `EDITOR`. Fallan también con `--editor`. Registradas en el nivel `EDITOR`, Godot acepta las 67 (`results-c-editor.tsv`). La matriz solo incluye `EditorPlugin`, para no llenar el log de arranque de cada escena con unos 70 errores iguales. |
| #242 | Unas 110 clases **no instanciables** tienen un constructor público sin argumentos que siempre falla (`CanvasItem()`, `FileAccess()`, `Tween()`, ...). Además, la lista de abstractas del generador está desactualizada: le faltan `AccessibilityServer`, `ChainIK3D`, `IKModifier3D`, `IterateIK3D`, `GodotInstance`, `SocketServer` y `StreamPeerSocket`, y sobra `Shader`. |
| #243 | Falta un diagnóstico de KSP que explique por qué un padre no se puede heredar y qué usar en su lugar. |

## Lo que no es de kogot

- **Instanciar un singleton o una `*ServerExtension` reemplaza el singleton del motor.** Al liberar esa instancia, el
  singleton queda colgando, y la siguiente clase que lo usa crashea. Le pasa a `PinJoint2D` después de
  `PhysicsServer2DExtension`, a `CameraTexture` después de `CameraServer`, a `AudioStreamPlayer` después de
  `AudioServer`. Pasa igual con clases nativas puras, sin ninguna extensión cargada. El check solo registra
  `PhysicsServer2DExtension`, `PhysicsServer3DExtension` y `TextServerExtension`, sin instanciarlas; la corrida en C
  salta además los singletons. Con eso, ninguna de las dos corridas crashea.
- `ERROR: Parameter "render_model_extension" is null.` (×3): sale de construir las clases OpenXR sin un runtime XR. Pasa
  igual en la corrida en C.
- `NOTIFICATION_POSTINITIALIZE` es necesaria. Sin ella, algunos nodos (por ejemplo `AudioStreamPlayer`) crashean al
  instanciarse por la extensión. kogot la envía (`createInstanceFunc`), y la extensión en C también con `KX_POSTINIT=1`.

## Costo de la matriz en el proyecto de prueba

- 809 clases `@Godot` más sus bindings: unos 2,5 minutos más de compilación, y la biblioteca debug pasa de unos 190 MB a
  unos 250 MB.
- Cada escena del proyecto registra las 809 clases al arrancar (el arranque headless de `forced_free_check` tarda
  unos 0,27 s en total). Además imprime 14 líneas `ERROR`, el registro y desregistro de los 7 casos rojos a propósito,
  hasta que se corrijan #239 y #241.

## Cómo regenerarla

Después de compilar la API generada (para otra versión de Godot, pasa su directorio):

```bash
python3 mi-juego-prueba/tools/generate_inheritance_matrix.py godot-version/v4_7_2
```

Para la pregunta "¿qué permite Godot?" (la extensión en C, en un proyecto Godot vacío con `bin/libkx.so`, un
`kx.gdextension` con `entry_symbol = "kx_init"` y `.godot/extension_list.cfg` apuntando a él):

```bash
gcc -shared -fPIC -I godot-version/v4_7_2 -o <proyecto>/bin/libkx.so docs/research/inheritance-matrix/matrix.c
```

```bash
GODOT=<godot> PROJ=<proyecto> LST=<lista de clases> KX_SKIP=<singletons y *ServerExtension> python3 docs/research/inheritance-matrix/drive.py
```

Para las clases de editor, `KX_LEVEL=3` y `godot --headless --editor --path <proyecto> --quit`. Los datos crudos están
en `docs/research/inheritance-matrix/`:

- `results-c-scene.tsv`: las 954 clases de núcleo, en nivel `SCENE`.
- `results-c-editor.tsv`: las 82 clases de editor, en nivel `EDITOR`.
- `results-kogot-875.tsv`: la primera corrida con kogot, con las 67 clases de editor. En esa corrida, `PinJoint2D`
  aparece como `CRASH` porque se instanció `PhysicsServer2DExtension` antes; ver arriba.
