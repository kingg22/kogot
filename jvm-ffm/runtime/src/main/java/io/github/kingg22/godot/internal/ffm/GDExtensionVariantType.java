package io.github.kingg22.godot.internal.ffm;

/// Equivalent to `GDExtensionVariantType` enum
public final class GDExtensionVariantType {

    private GDExtensionVariantType() {
        throw new UnsupportedOperationException("Utility class");
    }

    /* VARIANT TYPES */

    public static final short GDEXTENSION_VARIANT_TYPE_NIL = 0;

    /*  atomic types */
    public static final short GDEXTENSION_VARIANT_TYPE_BOOL = 1;
    public static final short GDEXTENSION_VARIANT_TYPE_INT = 2;
    public static final short GDEXTENSION_VARIANT_TYPE_FLOAT = 3;
    public static final short GDEXTENSION_VARIANT_TYPE_STRING = 4;

    /* math types */
    public static final short GDEXTENSION_VARIANT_TYPE_VECTOR2 = 5;
    public static final short GDEXTENSION_VARIANT_TYPE_VECTOR2I = 6;
    public static final short GDEXTENSION_VARIANT_TYPE_RECT2 = 7;
    public static final short GDEXTENSION_VARIANT_TYPE_RECT2I = 8;
    public static final short GDEXTENSION_VARIANT_TYPE_VECTOR3 = 9;
    public static final short GDEXTENSION_VARIANT_TYPE_VECTOR3I = 10;
    public static final short GDEXTENSION_VARIANT_TYPE_TRANSFORM2D = 11;
    public static final short GDEXTENSION_VARIANT_TYPE_VECTOR4 = 12;
    public static final short GDEXTENSION_VARIANT_TYPE_VECTOR4I = 13;
    public static final short GDEXTENSION_VARIANT_TYPE_PLANE = 14;
    public static final short GDEXTENSION_VARIANT_TYPE_QUATERNION = 15;
    public static final short GDEXTENSION_VARIANT_TYPE_AABB = 16;
    public static final short GDEXTENSION_VARIANT_TYPE_BASIS = 17;
    public static final short GDEXTENSION_VARIANT_TYPE_TRANSFORM3D = 18;
    public static final short GDEXTENSION_VARIANT_TYPE_PROJECTION = 19;

    /* misc types */
    public static final short GDEXTENSION_VARIANT_TYPE_COLOR = 20;
    public static final short GDEXTENSION_VARIANT_TYPE_STRING_NAME = 21;
    public static final short GDEXTENSION_VARIANT_TYPE_NODE_PATH = 22;
    public static final short GDEXTENSION_VARIANT_TYPE_RID = 23;
    public static final short GDEXTENSION_VARIANT_TYPE_OBJECT = 24;
    public static final short GDEXTENSION_VARIANT_TYPE_CALLABLE = 25;
    public static final short GDEXTENSION_VARIANT_TYPE_SIGNAL = 26;
    public static final short GDEXTENSION_VARIANT_TYPE_DICTIONARY = 27;
    public static final short GDEXTENSION_VARIANT_TYPE_ARRAY = 28;

    /* typed arrays */
    public static final short GDEXTENSION_VARIANT_TYPE_PACKED_BYTE_ARRAY = 29;
    public static final short GDEXTENSION_VARIANT_TYPE_PACKED_INT32_ARRAY = 30;
    public static final short GDEXTENSION_VARIANT_TYPE_PACKED_INT64_ARRAY = 31;
    public static final short GDEXTENSION_VARIANT_TYPE_PACKED_FLOAT32_ARRAY = 32;
    public static final short GDEXTENSION_VARIANT_TYPE_PACKED_FLOAT64_ARRAY = 33;
    public static final short GDEXTENSION_VARIANT_TYPE_PACKED_STRING_ARRAY = 34;
    public static final short GDEXTENSION_VARIANT_TYPE_PACKED_VECTOR2_ARRAY = 35;
    public static final short GDEXTENSION_VARIANT_TYPE_PACKED_VECTOR3_ARRAY = 36;
    public static final short GDEXTENSION_VARIANT_TYPE_PACKED_COLOR_ARRAY = 37;
    public static final short GDEXTENSION_VARIANT_TYPE_PACKED_VECTOR4_ARRAY = 38;

    public static final short GDEXTENSION_VARIANT_TYPE_VARIANT_MAX = 39;
}
