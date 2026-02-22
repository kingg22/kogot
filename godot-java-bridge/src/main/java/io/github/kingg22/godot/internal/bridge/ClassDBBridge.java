package io.github.kingg22.godot.internal.bridge;

import io.github.kingg22.godot.api.GodotClass;
import io.github.kingg22.godot.internal.ffm.GDExtensionCallError;
import io.github.kingg22.godot.internal.ffm.GDExtensionClassCreateInstance2;
import io.github.kingg22.godot.internal.ffm.GDExtensionClassCreationInfo5;
import io.github.kingg22.godot.internal.ffm.GDExtensionClassFreeInstance;
import io.github.kingg22.godot.internal.ffm.GDExtensionClassMethodCall;
import io.github.kingg22.godot.internal.ffm.GDExtensionClassMethodInfo;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static io.github.kingg22.godot.internal.ffm.GDExtensionCallError.CallErrorType.CALL_OK;
import static java.util.Objects.requireNonNull;

/** High-level ClassDB registration and instance dispatch. */
public final class ClassDBBridge {
    private final GodotFFI ffi;
    private final StringNameCache stringNames;
    private final Arena arena;
    private final AtomicLong classIdGenerator = new AtomicLong(1);
    private final AtomicLong methodIdGenerator = new AtomicLong(1);
    private final Map<Long, ClassEntry> classes = new ConcurrentHashMap<>();
    private final Map<Long, MethodHandler> methods = new ConcurrentHashMap<>();
    private final Map<Long, InstanceHandle> instances = new ConcurrentHashMap<>();

    private final MemorySegment methodCallStub;
    private final MemorySegment createInstanceStub;
    private final MemorySegment freeInstanceStub;

    ClassDBBridge(final GodotFFI ffi, final StringNameCache stringNames, final Arena arena) {
        this.ffi = ffi;
        this.stringNames = stringNames;
        this.arena = arena;
        this.methodCallStub = GDExtensionClassMethodCall.allocate(this::onMethodCall, arena);
        this.createInstanceStub = GDExtensionClassCreateInstance2.allocate(this::onCreateInstance, arena);
        this.freeInstanceStub = GDExtensionClassFreeInstance.allocate(this::onFreeInstance, arena);
    }

    public void registerClass(final ClassDefinition definition) {
        final long classId = classIdGenerator.getAndIncrement();
        final var classUserdata = arena.allocate(ValueLayout.JAVA_LONG);
        classUserdata.set(ValueLayout.JAVA_LONG, 0, classId);

        final var className = stringNames.get(definition.className());
        final var parentName = stringNames.get(definition.parentClassName());

        final var creationInfo = GDExtensionClassCreationInfo5.create(
                false,
                false,
                true,
                createInstanceStub,
                freeInstanceStub,
                "",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                classUserdata);

        classes.put(classId, new ClassEntry(definition, className, parentName, classUserdata));
        ffi.classdbRegisterExtensionClass5(className, parentName, creationInfo);
    }

    public void registerMethod(final String className, final String methodName, final MethodHandler handler) {
        final long methodId = methodIdGenerator.getAndIncrement();
        final var methodUserdata = arena.allocate(ValueLayout.JAVA_LONG);
        methodUserdata.set(ValueLayout.JAVA_LONG, 0, methodId);
        methods.put(methodId, handler);

        final var methodInfo = arena.allocate(GDExtensionClassMethodInfo.layout());
        GDExtensionClassMethodInfo.name(methodInfo, stringNames.get(methodName));
        GDExtensionClassMethodInfo.method_userdata(methodInfo, methodUserdata);
        GDExtensionClassMethodInfo.call_func(methodInfo, methodCallStub);
        GDExtensionClassMethodInfo.ptrcall_func(methodInfo, MemorySegment.NULL);
        GDExtensionClassMethodInfo.method_flags(methodInfo, 0);
        GDExtensionClassMethodInfo.has_return_value(methodInfo, (byte) 0);
        GDExtensionClassMethodInfo.return_value_info(methodInfo, MemorySegment.NULL);
        GDExtensionClassMethodInfo.return_value_metadata(methodInfo, 0);
        GDExtensionClassMethodInfo.argument_count(methodInfo, 0);
        GDExtensionClassMethodInfo.arguments_info(methodInfo, MemorySegment.NULL);
        GDExtensionClassMethodInfo.arguments_metadata(methodInfo, MemorySegment.NULL);
        GDExtensionClassMethodInfo.default_argument_count(methodInfo, 0);
        GDExtensionClassMethodInfo.default_arguments(methodInfo, MemorySegment.NULL);

        ffi.classdbRegisterExtensionClassMethod(stringNames.get(className), methodInfo);
    }

    private MemorySegment onCreateInstance(final MemorySegment classUserdata, final byte notifyPostInitialize) {
        final long classId =
                classUserdata.reinterpret(ValueLayout.JAVA_LONG.byteSize()).get(ValueLayout.JAVA_LONG, 0);
        final var entry = classes.get(classId);
        if (entry == null) {
            return MemorySegment.NULL;
        }

        final var objectPtr = ffi.classdbConstructObject2(entry.className);
        if (objectPtr == MemorySegment.NULL) {
            return MemorySegment.NULL;
        }

        final var handle = InstanceHandle.create(entry.definition.factory());
        instances.put(handle.address, handle);
        ffi.objectSetInstance(objectPtr, entry.className, handle.dataPointer);
        return objectPtr;
    }

    private void onFreeInstance(final MemorySegment classUserdata, final MemorySegment instancePtr) {
        final var address = instancePtr.address();
        final var handle = instances.remove(address);
        if (handle != null) {
            handle.close();
        }
    }

    private void onMethodCall(
            final MemorySegment methodUserdata,
            final MemorySegment instancePtr,
            final MemorySegment args,
            final long argCount,
            final MemorySegment returnValue,
            final MemorySegment error) {
        final long methodId =
                methodUserdata.reinterpret(ValueLayout.JAVA_LONG.byteSize()).get(ValueLayout.JAVA_LONG, 0);
        final var handler = methods.get(methodId);

        final var handle = instances.get(instancePtr.address());
        if (handler == null || handle == null) {
            GDExtensionCallError.setError(error, CALL_OK, 0, 0);
            ffi.variantNewNil(returnValue);
            return;
        }

        handler.invoke(handle.instance, args, argCount, returnValue, error, ffi);
    }

    /** Dispatches a ClassDB method call into Java. */
    @FunctionalInterface
    public interface MethodHandler {
        void invoke(
                GodotClass instance,
                MemorySegment args,
                long argCount,
                MemorySegment returnValue,
                MemorySegment error,
                GodotFFI ffi);
    }

    /** Defines the ClassDB entry and the factory for creating Java instances. */
    public record ClassDefinition(String className, String parentClassName, Supplier<? extends GodotClass> factory) {}

    private record ClassEntry(
            ClassDefinition definition,
            MemorySegment className,
            MemorySegment parentName,
            MemorySegment classUserdata) {}

    private static final class InstanceHandle implements AutoCloseable {
        private final GodotClass instance;
        private final Arena arena;
        private final MemorySegment dataPointer;
        private final long address;

        private InstanceHandle(final GodotClass instance, final Arena arena, final MemorySegment dataPointer) {
            this.instance = instance;
            this.arena = arena;
            this.dataPointer = dataPointer;
            this.address = dataPointer.address();
        }

        static InstanceHandle create(final Supplier<? extends GodotClass> factory) {
            final var instance = factory.get();
            requireNonNull(instance, "Instance factory returned null");
            final var arena = Arena.ofShared();
            final var dataPointer = arena.allocate(ValueLayout.JAVA_LONG);
            dataPointer.set(ValueLayout.JAVA_LONG, 0, dataPointer.address());
            return new InstanceHandle(instance, arena, dataPointer);
        }

        @Override
        public void close() {
            arena.close();
        }
    }
}
