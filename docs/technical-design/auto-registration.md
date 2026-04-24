# Auto-registro de clases (Java/Kotlin) — diseño

## Objetivo
Auto-registrar clases Godot sin registro manual, sin reflexión en runtime, compatible con:
- JVM HotSpot
- GraalVM Native Image
- Minificación/obfuscación (con reglas reproducibles)

## Enfoque propuesto
1) **Regla de elegibilidad**
   - Una clase es registrable si **extiende** `io.github.kingg22.godot.api.GodotNode`.
   - Debe tener **constructor público sin argumentos**.
   - No se usan anotaciones ni reflexión en runtime.

2) **Índice generado en build**
   - Un task Gradle analiza bytecode de `main` (Java + Kotlin) usando ASM.
   - Se genera un archivo Java fuente con referencias directas:
     - `io.github.kingg22.godot.internal.registry.GeneratedRegistry`
     - Contiene `registerAll(Registry registry)` con llamadas explícitas:
       - `registry.register(new ClassInfo(MyNode.class, "Node"));`
   - Se infiere el parent a partir del super tipo (`GodotNode` => `"Node"`).
   - Se generan referencias directas (`MyNode::new`), sin `Class.forName`.

3) **Runtime**
   - `GodotBridge.initialize()` invoca `GeneratedRegistry.registerAll(...)`.
   - No hay reflexión ni descubrimiento dinámico.

## Reglas para no romper Graal/Minify
- El registry generado referencia clases concretas => el linker las mantiene.
- Se puede añadir una regla de keep para el package `io.github.kingg22.godot.registry`.
- Para minificación: el class literal sigue siendo válido si la clase no es removida.

## Compatibilidad Kotlin
- El análisis de bytecode no distingue origen, detecta clases Kotlin igual que Java.
- Se recomienda evitar clases `object` si no se desea registrar singletons.

## Riesgos
- La detección requiere heredar de `GodotNode` y un no-args constructor.
- ASM añade dependencia build-time (no runtime).

## Estado
- Pendiente de confirmación: nombre de la interfaz marker y estrategia de detección.
