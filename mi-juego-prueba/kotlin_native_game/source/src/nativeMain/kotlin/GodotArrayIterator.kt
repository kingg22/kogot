import io.github.kingg22.godot.api.builtin.GodotArray
import io.github.kingg22.godot.api.builtin.Variant

fun <T> GodotArray<T>.asList(): List<Variant> = buildList {
    for (i in 0 until size()) {
        add(get(i))
    }
}
