package io.github.kingg22.godot.internal.ffm;

/// Equivalent to [GDExtensionClassMethodFlags] enum
public final class GDExtensionClassMethodFlags {
    private GDExtensionClassMethodFlags() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final short GDEXTENSION_METHOD_FLAG_NORMAL = 1;
    public static final short GDEXTENSION_METHOD_FLAG_EDITOR = 2;
    public static final short GDEXTENSION_METHOD_FLAG_CONST = 4;
    public static final short GDEXTENSION_METHOD_FLAG_VIRTUAL = 8;
    public static final short GDEXTENSION_METHOD_FLAG_VARARG = 16;
    public static final short GDEXTENSION_METHOD_FLAG_STATIC = 32;
    public static final short GDEXTENSION_METHOD_FLAG_VIRTUAL_REQUIRED = 128;
    public static final short GDEXTENSION_METHOD_FLAGS_DEFAULT = GDEXTENSION_METHOD_FLAG_NORMAL;
}
