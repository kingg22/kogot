package io.github.kingg22.godot.internal.ffm;

/// ```c++
/// typedef enum {
/// 	GDEXTENSION_METHOD_FLAG_NORMAL = 1,
/// 	GDEXTENSION_METHOD_FLAG_EDITOR = 2,
/// 	GDEXTENSION_METHOD_FLAG_CONST = 4,
/// 	GDEXTENSION_METHOD_FLAG_VIRTUAL = 8,
/// 	GDEXTENSION_METHOD_FLAG_VARARG = 16,
/// 	GDEXTENSION_METHOD_FLAG_STATIC = 32,
/// 	GDEXTENSION_METHOD_FLAGS_DEFAULT = GDEXTENSION_METHOD_FLAG_NORMAL,
/// };
/// ```
public class GDExtensionClassMethodFlags {
    private GDExtensionClassMethodFlags() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final short NORMAL = 1;
    public static final short EDITOR = 2;
    public static final short CONST = 4;
    public static final short VIRTUAL = 8;
    public static final short VARARG = 16;
    public static final short STATIC = 32;
    public static final short DEFAULT = NORMAL;
}
