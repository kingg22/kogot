package io.github.kingg22.godot.internal.bridge;

import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/** Holds Godot FFI state shared across the bridge. */
public final class BridgeContext implements AutoCloseable {
    private static @Nullable BridgeContext instance;

    private final Arena arena;
    private final GodotFFI ffi;
    private final StringNameCache stringNames;
    private final ClassDBBridge classDB;
    private final ScriptInstanceBridge scriptInstances;

    private BridgeContext(final MemorySegment getProcAddress, final MemorySegment library) {
        this.arena = Arena.ofShared();
        this.ffi = new GodotFFI(getProcAddress, library, arena);
        this.stringNames = new StringNameCache(ffi, arena);
        this.classDB = new ClassDBBridge(ffi, stringNames, arena);
        this.scriptInstances = new ScriptInstanceBridge(ffi, stringNames, arena);
    }

    public static void initialize(final long getProcAddressPointer, final long libraryPointer) {
        if (instance != null) {
            return;
        }
        final var getProcAddress = MemorySegment.ofAddress(getProcAddressPointer)
                .reinterpret(ValueLayout.ADDRESS.byteSize(), Arena.global(), null);
        final var library = MemorySegment.ofAddress(libraryPointer)
                .reinterpret(ValueLayout.ADDRESS.byteSize(), Arena.global(), null);
        instance = new BridgeContext(getProcAddress, library);
        System.out.println("[Java] Running on " + instance.ffi.getGodotVersion2());
    }

    /** SCENE-level hook: place runtime registration calls (ClassDB/Script instances) here. */
    public static void onSceneInitialized() {
        if (instance == null) {
            throw new IllegalStateException("BridgeContext not initialized for SCENE");
        }
        // Intentionally lightweight in early development.
    }

    /** EDITOR-level hook: place editor-only integrations here. */
    public static void onEditorInitialized() {
        if (instance == null) {
            throw new IllegalStateException("BridgeContext not initialized for EDITOR");
        }
        // Intentionally lightweight in early development.
    }

    /** EDITOR-level cleanup hook. */
    public static void onEditorDeinitialized() {
        // Editor-specific teardown will be added when editor tooling is implemented.
    }

    /** SCENE-level cleanup hook. */
    public static void onSceneDeinitialized() {
        shutdown();
    }

    public static BridgeContext get() {
        if (instance == null) {
            throw new IllegalStateException("BridgeContext not initialized");
        }
        return instance;
    }

    public static void shutdown() {
        if (instance == null) {
            return;
        }
        instance.close();
        instance = null;
    }

    GodotFFI ffi() {
        return ffi;
    }

    StringNameCache stringNames() {
        return stringNames;
    }

    public ClassDBBridge classDB() {
        return classDB;
    }

    public ScriptInstanceBridge scriptInstances() {
        return scriptInstances;
    }

    @Override
    public void close() {
        arena.close();
    }
}
