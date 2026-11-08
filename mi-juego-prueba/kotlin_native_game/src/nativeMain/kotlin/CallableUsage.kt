import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.builtin.Vector2

/** Exercises the typed `Callable { ... }` factories: Godot arguments are converted to the declared types. */
internal fun typedCallableSamples(): List<Callable> = listOf(
    Callable { -> println("no arguments") },
    // Godot passes INT as Long and STRING as GodotString: both are converted for the lambda.
    Callable { count: Int, name: String -> name.repeat(count) },
    Callable { position: Vector2, scale: Float -> position * scale.toDouble() },
)
