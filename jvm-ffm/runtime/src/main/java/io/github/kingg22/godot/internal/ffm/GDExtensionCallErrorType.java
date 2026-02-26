package io.github.kingg22.godot.internal.ffm;

/// Equivalent to `GDExtensionCallErrorType` enum with constant
public final class GDExtensionCallErrorType {
    private GDExtensionCallErrorType() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final short GDEXTENSION_CALL_OK = 0;
    public static final short GDEXTENSION_CALL_ERROR_INVALID_METHOD = 1;
    /// Expected a different variant type.
    public static final short GDEXTENSION_CALL_ERROR_INVALID_ARGUMENT = 2;
    /// Expected lower number of arguments.
    public static final short GDEXTENSION_CALL_ERROR_TOO_MANY_ARGUMENTS = 3;
    /// Expected higher number of arguments.
    public static final short GDEXTENSION_CALL_ERROR_TOO_FEW_ARGUMENTS = 4;
    public static final short GDEXTENSION_CALL_ERROR_INSTANCE_IS_NULL = 5;
    /// Used for const call.
    public static final short GDEXTENSION_CALL_ERROR_METHOD_NOT_CONST = 6;
}
