package io.github.kingg22.godot.internal.bridge;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Caches StringName values created via GDExtension. */
final class StringNameCache {
    private static final long STRING_NAME_BYTES = 4;
    private static final ValueLayout.OfInt STRING_NAME_LAYOUT = ValueLayout.JAVA_INT;

    private final GodotFFI ffi;
    private final Arena arena;
    private final Map<String, MemorySegment> cache = new ConcurrentHashMap<>();

    StringNameCache(final GodotFFI ffi, final Arena arena) {
        this.ffi = ffi;
        this.arena = arena;
    }

    MemorySegment get(final String value) {
        return cache.computeIfAbsent(value, key -> {
            final var stringName = arena.allocate(STRING_NAME_BYTES);
            final var cString = arena.allocateFrom(key);
            ffi.stringNameNewWithUtf8Chars(stringName, cString);
            return stringName;
        });
    }

    int idOf(final MemorySegment stringName) {
        final var slice = stringName.reinterpret(STRING_NAME_BYTES);
        return slice.get(STRING_NAME_LAYOUT, 0);
    }
}
