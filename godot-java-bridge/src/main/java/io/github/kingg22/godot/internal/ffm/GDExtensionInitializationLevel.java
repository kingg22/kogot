package io.github.kingg22.godot.internal.ffm;

/// Equivalent to `GDExtensionInitializationLevel` enum with constant
public final class GDExtensionInitializationLevel {
    private GDExtensionInitializationLevel() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final short GDEXTENSION_INITIALIZATION_CORE = 0;
    public static final short GDEXTENSION_INITIALIZATION_SERVERS = 1;
    public static final short GDEXTENSION_INITIALIZATION_SCENE = 2;
    public static final short GDEXTENSION_INITIALIZATION_EDITOR = 3;
    public static final short GDEXTENSION_MAX_INITIALIZATION_LEVEL = 4;
}
