package io.github.kingg22.godot.internal.ffm;

/// Refers to `PropertyUsageFlags` defined in `extension_api.json`
public final class GDExtensionPropertyUsageFlags {

    private GDExtensionPropertyUsageFlags() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final short PROPERTY_USAGE_NONE = 0;
    public static final short PROPERTY_USAGE_STORAGE = 2;
    public static final short PROPERTY_USAGE_EDITOR = 4;
    public static final short PROPERTY_USAGE_INTERNAL = 8;
    public static final short PROPERTY_USAGE_CHECKABLE = 16;
    public static final short PROPERTY_USAGE_CHECKED = 32;
    public static final short PROPERTY_USAGE_GROUP = 64;
    public static final short PROPERTY_USAGE_CATEGORY = 128;
    public static final short PROPERTY_USAGE_SUBGROUP = 256;
    public static final short PROPERTY_USAGE_CLASS_IS_BITFIELD = 512;
    public static final short PROPERTY_USAGE_NO_INSTANCE_STATE = 1024;
    public static final short PROPERTY_USAGE_RESTART_IF_CHANGED = 2048;
    public static final short PROPERTY_USAGE_SCRIPT_VARIABLE = 4096;
    public static final short PROPERTY_USAGE_STORE_IF_NULL = 8192;
    public static final short PROPERTY_USAGE_UPDATE_ALL_IF_MODIFIED = 16384;
    public static final int PROPERTY_USAGE_SCRIPT_DEFAULT_VALUE = 32768;
    public static final int PROPERTY_USAGE_CLASS_IS_ENUM = 65536;
    public static final int PROPERTY_USAGE_NIL_IS_VARIANT = 131072;
    public static final int PROPERTY_USAGE_ARRAY = 262144;
    public static final int PROPERTY_USAGE_ALWAYS_DUPLICATE = 524288;
    public static final int PROPERTY_USAGE_NEVER_DUPLICATE = 1048576;
    public static final int PROPERTY_USAGE_HIGH_END_GFX = 2097152;
    public static final int PROPERTY_USAGE_NODE_PATH_FROM_SCENE_ROOT = 4194304;
    public static final int PROPERTY_USAGE_RESOURCE_NOT_PERSISTENT = 8388608;
    public static final int PROPERTY_USAGE_KEYING_INCREMENTS = 16777216;
    public static final int PROPERTY_USAGE_DEFERRED_SET_RESOURCE = 33554432;
    public static final int PROPERTY_USAGE_EDITOR_INSTANTIATE_OBJECT = 67108864;
    public static final int PROPERTY_USAGE_EDITOR_BASIC_SETTING = 134217728;
    public static final int PROPERTY_USAGE_READ_ONLY = 268435456;
    public static final int PROPERTY_USAGE_SECRET = 536870912;
    // OR operator
    public static final short PROPERTY_USAGE_DEFAULT = PROPERTY_USAGE_STORAGE | PROPERTY_USAGE_EDITOR;
    public static final short PROPERTY_USAGE_NO_EDITOR = PROPERTY_USAGE_STORAGE;
}
