package io.github.kingg22.godot.internal.ffm;

/// Equivalent to `GDExtensionClassMethodArgumentMetadata` enum with constants
public final class GDExtensionClassMethodArgumentMetadata {
    private GDExtensionClassMethodArgumentMetadata() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_NONE = 0;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_INT8 = 1;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_INT16 = 2;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_INT32 = 3;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_INT64 = 4;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_UINT8 = 5;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_UINT16 = 6;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_UINT32 = 7;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_UINT64 = 8;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_REAL_IS_FLOAT = 9;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_REAL_IS_DOUBLE = 10;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_CHAR16 = 11;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_INT_IS_CHAR32 = 12;
    public static final short GDEXTENSION_METHOD_ARGUMENT_METADATA_OBJECT_IS_REQUIRED = 13;
}
