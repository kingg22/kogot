package io.github.kingg22.godot.api;

import java.util.function.Supplier;

/** Public registration API used by the generated registry. */
public interface GodotRegistry {
    void register(String className, String parentClassName, Supplier<? extends GodotClass> factory);
}
