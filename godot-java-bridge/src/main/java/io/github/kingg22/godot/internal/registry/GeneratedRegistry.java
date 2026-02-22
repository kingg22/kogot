package io.github.kingg22.godot.internal.registry;

import io.github.kingg22.godot.api.GodotClass;
import io.github.kingg22.godot.api.GodotRegistry;

/** Placeholder registry. Overwritten by build task. */
public final class GeneratedRegistry {
    private GeneratedRegistry() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void registerAll(final GodotRegistry registry) {
        registry.register("JvmTestNode", "Node", JvmTestNode::new);
    }

    public static final class JvmTestNode implements GodotClass {
        public JvmTestNode() {
            System.out.println("JvmTestNode created");
        }
    }
}
