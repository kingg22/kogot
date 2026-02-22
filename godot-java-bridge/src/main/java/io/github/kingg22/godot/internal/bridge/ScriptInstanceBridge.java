package io.github.kingg22.godot.internal.bridge;

import io.github.kingg22.godot.internal.ffm.GDExtensionCallError;
import io.github.kingg22.godot.internal.ffm.GDExtensionScriptInstanceCall;
import io.github.kingg22.godot.internal.ffm.GDExtensionScriptInstanceFree;
import io.github.kingg22.godot.internal.ffm.GDExtensionScriptInstanceHasMethod;
import io.github.kingg22.godot.internal.ffm.GDExtensionScriptInstanceInfo3;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.kingg22.godot.internal.ffm.GDExtensionCallError.CallErrorType.CALL_OK;

/** Handles script instance callbacks for Java-backed scripts. */
public final class ScriptInstanceBridge {
    /** Callback interface used by script instances for method dispatch. */
    @FunctionalInterface
    public interface ScriptInstance {
        void call(int methodId, MemorySegment args, long argCount, MemorySegment returnValue, MemorySegment error);

        default boolean hasMethod(final int methodId) {
            return false;
        }
    }

    private final GodotFFI ffi;
    private final StringNameCache stringNames;
    private final Arena arena;
    private final AtomicLong instanceIdGenerator = new AtomicLong(1);
    private final Map<Long, ScriptHandle> instances = new ConcurrentHashMap<>();

    private final MemorySegment hasMethodStub;
    private final MemorySegment callStub;
    private final MemorySegment freeStub;
    private final MemorySegment infoStruct;

    ScriptInstanceBridge(final GodotFFI ffi, final StringNameCache stringNames, final Arena arena) {
        this.ffi = ffi;
        this.stringNames = stringNames;
        this.arena = arena;
        this.hasMethodStub = GDExtensionScriptInstanceHasMethod.allocate(this::onHasMethod, arena);
        this.callStub = GDExtensionScriptInstanceCall.allocate(this::onCall, arena);
        this.freeStub = GDExtensionScriptInstanceFree.allocate(this::onFree, arena);
        this.infoStruct = buildInfoStruct();
    }

    public MemorySegment create(final ScriptInstance instance) {
        final var handle = ScriptHandle.create(instance, instanceIdGenerator.getAndIncrement());
        instances.put(handle.dataAddress, handle);
        return ffi.scriptInstanceCreate3(infoStruct, handle.dataPointer);
    }

    public void attachToObject(final MemorySegment objectPtr, final MemorySegment scriptInstancePtr) {
        ffi.objectSetScriptInstance(objectPtr, scriptInstancePtr);
    }

    private MemorySegment buildInfoStruct() {
        final var info = arena.allocate(GDExtensionScriptInstanceInfo3.layout());
        GDExtensionScriptInstanceInfo3.has_method_func(info, hasMethodStub);
        GDExtensionScriptInstanceInfo3.call_func(info, callStub);
        GDExtensionScriptInstanceInfo3.free_func(info, freeStub);
        return info;
    }

    private byte onHasMethod(final MemorySegment self, final MemorySegment method) {
        final var handle = instances.get(self.address());
        if (handle == null) {
            return 0;
        }
        final int methodId = stringNames.idOf(method);
        return (byte) (handle.instance.hasMethod(methodId) ? 1 : 0);
    }

    private void onCall(
            final MemorySegment self,
            final MemorySegment method,
            final MemorySegment args,
            final long argCount,
            final MemorySegment returnValue,
            final MemorySegment error) {
        final var handle = instances.get(self.address());
        if (handle == null) {
            GDExtensionCallError.setError(error, CALL_OK, 0, 0);
            ffi.variantNewNil(returnValue);
            return;
        }
        final int methodId = stringNames.idOf(method);
        handle.instance.call(methodId, args, argCount, returnValue, error);
    }

    private void onFree(final MemorySegment self) {
        final var handle = instances.remove(self.address());
        if (handle != null) {
            handle.close();
        }
    }

    private static final class ScriptHandle implements AutoCloseable {
        private final ScriptInstance instance;
        private final Arena arena;
        private final MemorySegment dataPointer;
        private final long dataAddress;

        private ScriptHandle(final ScriptInstance instance, final Arena arena, final MemorySegment dataPointer) {
            this.instance = instance;
            this.arena = arena;
            this.dataPointer = dataPointer;
            this.dataAddress = dataPointer.address();
        }

        static ScriptHandle create(final ScriptInstance instance, final long id) {
            final var arena = Arena.ofShared();
            final var dataPointer = arena.allocate(ValueLayout.JAVA_LONG);
            dataPointer.set(ValueLayout.JAVA_LONG, 0, id);
            return new ScriptHandle(instance, arena, dataPointer);
        }

        @Override
        public void close() {
            arena.close();
        }
    }
}
