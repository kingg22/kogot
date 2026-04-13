import io.github.kingg22.godot.api.builtin.GodotArray
import io.github.kingg22.godot.api.builtin.Variant

fun <T> GodotArray<T>.asList(): List<Variant> = List(size().toInt()) { get(it.toLong()) }
