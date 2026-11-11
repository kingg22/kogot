# Issue #119 — Fase 0: colecciones tipadas anidadas en Godot 4.7.2

Experimento ejecutado con `Godot_v4.7.2-stable` (`--headless --script`), sin código de kogot.
Objetivo: saber qué admite realmente el motor antes de decidir cómo modelar
`GodotArray<GodotArray<T>>` en Kotlin.

## Resultados

| Caso | Resultado |
|---|---|
| `Array[Array]` con internos `Array[int]`, `Array[String]` y sin tipo | Admitido. Los tres conviven en el mismo array exterior. |
| `outer.get_typed_builtin()` | `28` (`TYPE_ARRAY`), clase `''`, script `null`. Un solo nivel. |
| Tipo de cada interno | `2` (int), `4` (String), `0` (sin tipo): **cada interno conserva su propio tipo**, el exterior no lo restringe. |
| `is_same_typed` entre `Array[int]` y `Array[String]` | `false` (compara el tipo de cada array, no el contenedor). |
| `Dictionary[String, Array]` | Admitido: clave `4` (String), valor `28` (Array). El valor interno sigue tipado (`is_typed == true`). |
| `Array[Dictionary]` | Admitido (`27`). |
| `Array([], TYPE_ARRAY, &"", null)` (equivale a `array_set_typed` con `ARRAY`) | Admitido; `is_same_typed` con `Array[Array]` da `true`. |
| `outer.append(5)` en `Array[Array]` | Error del motor: *Attempted to push_back a variable of type 'int' into a TypedArray of type 'Array'*. El elemento no se añade. |
| `Array[Array[int]]` en GDScript | Rechazado al parsear (según el código fuente, `gdscript_parser.cpp`: "Nested typed collections are not supported."). No ejecutado aquí. |

Nota: `set_typed` no está expuesto a GDScript; el constructor `Array(base, type, class_name, script)` es el equivalente.

## Decisión para el diseño

- El motor admite declarar `Array[Array]` / `Dictionary[K, Array]`, y acepta **cualquier** array como elemento, tipado o no.
- Por tanto, `GodotArray<GodotArray<Long>>` puede existir en Kotlin: el exterior se declara a Godot como `Array` sin subtipo y **la comprobación del tipo interno es responsabilidad de Kotlin** (conversor del elemento al leer).
- Un array interno recibido de Godot puede traer un tipo distinto al esperado: la lectura debe validar `getTypedBuiltin` del interno al envolverlo, no confiar en el exterior.
- El motor sí impone el nivel exterior: añadir un elemento que no sea `Array` falla con error en el motor (no con excepción), así que Kotlin debe validar antes de escribir.

## Script

```gdscript
extends SceneTree

func _init():
	var inner_int: Array[int] = [1]
	var inner_str: Array[String] = ["a"]
	var untyped := [1, "x"]
	var outer: Array[Array] = []
	outer.append(inner_int)
	outer.append(inner_str)
	outer.append(untyped)
	print("outer typed builtin=", outer.get_typed_builtin(), " class='", outer.get_typed_class_name(), "' script=", outer.get_typed_script())
	print("inner builtins=", outer[0].get_typed_builtin(), outer[1].get_typed_builtin(), outer[2].get_typed_builtin())
	print("is_same_typed(inner0, inner1)=", outer[0].is_same_typed(outer[1]))
	var d: Dictionary[String, Array] = {"k": inner_int}
	print("dict key=", d.get_typed_key_builtin(), " value=", d.get_typed_value_builtin(), " d[k].is_typed=", d["k"].is_typed())
	var od: Array[Dictionary] = [{"a": 1}]
	print("Array[Dictionary]=", od.get_typed_builtin())
	var raw := Array([], TYPE_ARRAY, &"", null)
	raw.append(inner_str)
	print("raw builtin=", raw.get_typed_builtin(), " is_same_typed(outer)=", raw.is_same_typed(outer))
	outer.append(5) # error del motor
	quit()
```

Ejecución:

```bash
Godot --headless --path <dir> --script nested.gd
```

## Pendiente (verificación por la API C desde kogot)

Este experimento usa el constructor de GDScript, que internamente llama a `set_typed`. La confirmación
con `setTypedRaw` (`ArrayBinding.kt`, `DictionaryBinding.kt`) desde Kotlin queda para la Fase 3, cuando
exista el factory `reified`, con un test de runtime.
