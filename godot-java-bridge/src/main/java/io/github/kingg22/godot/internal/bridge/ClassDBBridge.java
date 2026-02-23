package io.github.kingg22.godot.internal.bridge;

import io.github.kingg22.godot.internal.ffm.GDExtensionCallError;
import io.github.kingg22.godot.internal.ffm.GDExtensionClassCreateInstance2;
import io.github.kingg22.godot.internal.ffm.GDExtensionClassFreeInstance;
import io.github.kingg22.godot.internal.ffm.GDExtensionClassMethodCall;
import io.github.kingg22.godot.internal.ffm.GDExtensionClassToString;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static io.github.kingg22.godot.internal.ffm.GDExtensionCallErrorType.GDEXTENSION_CALL_OK;
import static io.github.kingg22.godot.internal.wrapper.StructWrapper.GDExtensionClassCreationInfo5;
import static io.github.kingg22.godot.internal.wrapper.StructWrapper.GDExtensionClassMethodInfo;
import static java.util.Objects.requireNonNull;

/** High-level ClassDB registration and instance dispatch. */
public final class ClassDBBridge implements AutoCloseable {
    private final GodotFFI ffi;
    private final StringNameCache stringNames;
    private final Arena arena;
    private final AtomicLong classIdGenerator = new AtomicLong(1);
    private final AtomicLong methodIdGenerator = new AtomicLong(1);
    private final Map<Long, ClassEntry<?>> classes = new ConcurrentHashMap<>();
    private final Map<Long, MethodHandler> methods = new ConcurrentHashMap<>();
    private final Map<Long, InstanceHandle<?>> instances = new ConcurrentHashMap<>();

    private final MemorySegment methodCallStub;
    private final MemorySegment createInstanceStub;
    private final MemorySegment freeInstanceStub;
    private final MemorySegment instanceToString;

    ClassDBBridge(final GodotFFI ffi, final StringNameCache stringNames, final Arena arena) {
        this.ffi = ffi;
        this.stringNames = stringNames;
        this.arena = arena;
        this.methodCallStub = GDExtensionClassMethodCall.allocate(this::onMethodCall, arena);
        this.createInstanceStub = GDExtensionClassCreateInstance2.allocate(this::onCreateInstance, arena);
        this.freeInstanceStub = GDExtensionClassFreeInstance.allocate(this::onFreeInstance, arena);
        this.instanceToString = GDExtensionClassToString.allocate(
                (instance, _, out) -> {
                    final var handle = instances.get(instance.address());
                    if (handle == null) {
                        System.err.println(
                                "Not found instance with address: '" + instance.address() + "' for toString");
                        return;
                    }

                    try {
                        final var message = handle.instance.toString();
                        ffi.stringNameNewWithUtf8Chars(out, arena.allocateFrom(message));
                    } catch (Exception e) {
                        System.err.println("Catch exception during toString: " + e.getMessage());
                    }
                },
                arena);
    }

    @Override
    public void close() {
        instances.values().forEach(InstanceHandle::close);
        instances.clear();
        classes.clear();
        methods.clear();
    }

    public <T> void registerClass(final ClassDefinition<T> definition) {
        System.out.println("Registering class named : '" + definition.className() + "' with parent: "
                + definition.parentClassName());
        final long classId = classIdGenerator.getAndIncrement();
        final var classUserdata = arena.allocate(ValueLayout.JAVA_LONG);
        classUserdata.set(ValueLayout.JAVA_LONG, 0, classId);

        final var className = stringNames.get(definition.className());
        final var parentName = stringNames.get(definition.parentClassName());

        final var creationInfo = GDExtensionClassCreationInfo5(
                false, false, true, true, createInstanceStub, freeInstanceStub, classUserdata);

        classes.put(classId, new ClassEntry<>(definition, className, parentName, classUserdata));
        ffi.classdbRegisterExtensionClass5(className, parentName, creationInfo);
    }

    public void registerMethod(final String className, final String methodName, final MethodHandler handler) {
        final long methodId = methodIdGenerator.getAndIncrement();
        final var methodUserdata = arena.allocate(ValueLayout.JAVA_LONG);
        methodUserdata.set(ValueLayout.JAVA_LONG, 0, methodId);
        methods.put(methodId, handler);

        final var methodInfo = GDExtensionClassMethodInfo(
                stringNames.get(methodName), methodCallStub, 0, false, 0, 0, 0, methodUserdata);

        ffi.classdbRegisterExtensionClassMethod(stringNames.get(className), methodInfo);
    }

    private MemorySegment onCreateInstance(final MemorySegment classUserdata, final byte notifyPostInitialize) {
        final long classId =
                classUserdata.reinterpret(ValueLayout.JAVA_LONG.byteSize()).get(ValueLayout.JAVA_LONG, 0);
        final var entry = classes.get(classId);
        if (entry == null) {
            return MemorySegment.NULL;
        }

        final var objectPtr = ffi.classdbConstructObject2(entry.parentName);
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
            GDExtensionCallError.setError(error, GDEXTENSION_CALL_OK, 0, 0);
            ffi.variantNewNil(returnValue);
            return;
        }

        handler.invoke(handle.instance, args, argCount, returnValue, error, ffi);
    }

    /** Dispatches a ClassDB method call into Java. */
    public interface MethodHandler {
        <T> void invoke(
                T instance,
                MemorySegment args,
                long argCount,
                MemorySegment returnValue,
                MemorySegment error,
                GodotFFI ffi);
    }

    /** Defines the ClassDB entry and the factory for creating Java instances. */
    public record ClassDefinition<T>(String className, String parentClassName, Supplier<? extends T> factory) {}

    private record ClassEntry<T>(
            ClassDefinition<T> definition,
            MemorySegment className,
            MemorySegment parentName,
            MemorySegment classUserdata) {}

    private static final class InstanceHandle<T> implements AutoCloseable {
        private final T instance;
        private final Arena arena;
        private final MemorySegment dataPointer;
        private final long address;

        private InstanceHandle(final T instance, final Arena arena, final MemorySegment dataPointer) {
            this.instance = instance;
            this.arena = arena;
            this.dataPointer = dataPointer;
            this.address = dataPointer.address();
        }

        static <T> InstanceHandle<T> create(final Supplier<? extends T> factory) {
            final var instance = factory.get();
            requireNonNull(instance, "Instance factory returned null");
            final var arena = Arena.ofShared();
            final var dataPointer = arena.allocate(ValueLayout.JAVA_LONG);
            dataPointer.set(ValueLayout.JAVA_LONG, 0, dataPointer.address());
            return new InstanceHandle<>(instance, arena, dataPointer);
        }

        @Override
        public void close() {
            arena.close();
        }
    }
}
