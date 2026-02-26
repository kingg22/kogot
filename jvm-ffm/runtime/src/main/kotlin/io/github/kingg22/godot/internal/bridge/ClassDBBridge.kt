package io.github.kingg22.godot.internal.bridge

import io.github.kingg22.godot.internal.ffm.GDExtensionCallErrorType
import io.github.kingg22.godot.internal.ffm.GDExtensionClassCreateInstance2
import io.github.kingg22.godot.internal.ffm.GDExtensionClassFreeInstance
import io.github.kingg22.godot.internal.ffm.GDExtensionClassMethodCall
import io.github.kingg22.godot.internal.ffm.GDExtensionClassToString
import io.github.kingg22.godot.internal.wrapper.GDExtensionCallError
import io.github.kingg22.godot.internal.wrapper.GDExtensionClassCreationInfo5
import io.github.kingg22.godot.internal.wrapper.GDExtensionClassMethodInfo
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier

/** High-level ClassDB registration and instance dispatch.  */
class ClassDBBridge internal constructor(
    private val ffi: GodotFFI,
    private val stringNames: StringNameCache,
    private val arena: Arena,
) : AutoCloseable {
    private val classIdGenerator = AtomicLong(1)
    private val methodIdGenerator = AtomicLong(1)
    private val classes: MutableMap<Long, ClassEntry<Any>> = ConcurrentHashMap()
    private val methods: MutableMap<Long, MethodHandler> = ConcurrentHashMap()
    private val instances: MutableMap<Long, InstanceHandle<Any>> = ConcurrentHashMap()

    private val methodCallStub: MemorySegment
    private val createInstanceStub: MemorySegment
    private val freeInstanceStub: MemorySegment
    private val instanceToString: MemorySegment

    private val onMethodCall = GDExtensionClassMethodCall.Function {
            methodUserdata: MemorySegment,
            instancePtr: MemorySegment,
            args: MemorySegment,
            argCount: Long,
            returnValue: MemorySegment,
            error: MemorySegment,
        ->
        val methodId = methodUserdata.reinterpret(ValueLayout.JAVA_LONG.byteSize())
            .get(ValueLayout.JAVA_LONG, 0)
        val handler = methods[methodId]

        val handle = instances[instancePtr.address()]
        if (handler == null || handle == null) {
            GDExtensionCallError(error, GDExtensionCallErrorType.GDEXTENSION_CALL_OK, 0, 0)
            ffi.variantNewNil(returnValue)
            return@Function
        }

        handler.invoke(handle.instance, args, argCount, returnValue, error, ffi)
    }

    private val onCreateInstance = GDExtensionClassCreateInstance2.Function {
            classUserdata: MemorySegment,
            notifyPostInitialize: Byte,
        ->
        val classId =
            classUserdata.reinterpret(ValueLayout.JAVA_LONG.byteSize()).get(ValueLayout.JAVA_LONG, 0)
        val entry = classes[classId] ?: return@Function MemorySegment.NULL

        val objectPtr = ffi.classdbConstructObject2(entry.parentName)
        if (objectPtr == MemorySegment.NULL) {
            return@Function MemorySegment.NULL
        }

        val handle = InstanceHandle.create(entry.definition.factory)
        instances[handle.address] = handle
        ffi.objectSetInstance(objectPtr, entry.className, handle.dataPointer)
        return@Function objectPtr
    }

    private val onFreeInstance = GDExtensionClassFreeInstance.Function {
            classUserdata: MemorySegment,
            instancePtr: MemorySegment,
        ->
        val address = instancePtr.address()
        instances.remove(address)?.close()
    }

    private val onToString = GDExtensionClassToString.Function {
            instance: MemorySegment?,
            isValid: MemorySegment?,
            out: MemorySegment?,
        ->
        val handle = instances[instance!!.address()]
        if (handle == null) {
            System.err.println("Not found instance with address: '${instance.address()}' for toString")
            return@Function
        }
        try {
            val message = handle.instance.toString()
            ffi.stringNameNewWithUtf8Chars(out!!, arena.allocateFrom(message))
        } catch (e: Exception) {
            System.err.println("Catch exception during toString: " + e.message)
        }
    }

    init {
        this.methodCallStub = GDExtensionClassMethodCall.allocate(this.onMethodCall, arena)
        this.createInstanceStub = GDExtensionClassCreateInstance2.allocate(this.onCreateInstance, arena)
        this.freeInstanceStub = GDExtensionClassFreeInstance.allocate(this.onFreeInstance, arena)
        this.instanceToString = GDExtensionClassToString.allocate(this.onToString, arena)
    }

    override fun close() {
        instances.values.forEach { obj -> obj.close() }
        instances.clear()
        classes.clear()
        methods.clear()
    }

    fun <T : Any> registerClass(definition: ClassDefinition<T>) {
        println("Registering class named : '${definition.className}' with parent: ${definition.parentClassName}")
        val classId = classIdGenerator.getAndIncrement()
        val classUserdata: MemorySegment = arena.allocate(ValueLayout.JAVA_LONG)
        classUserdata.set(ValueLayout.JAVA_LONG, 0, classId)

        val className = stringNames.get(definition.className)
        val parentName = stringNames.get(definition.parentClassName)

        val creationInfo = GDExtensionClassCreationInfo5(
            isVirtual = false,
            isAbstract = false,
            isExposed = true,
            isRuntime = true,
            createInstanceFunc = createInstanceStub,
            freeInstanceFunc = freeInstanceStub,
            classUserdata = classUserdata,
        )

        classes[classId] = ClassEntry(definition, className, parentName, classUserdata)
        ffi.classdbRegisterExtensionClass5(className, parentName, creationInfo)
    }

    fun registerMethod(className: String, methodName: String, handler: MethodHandler) {
        val methodId = methodIdGenerator.getAndIncrement()
        val methodUserdata = arena.allocate(ValueLayout.JAVA_LONG)
        methodUserdata.set(ValueLayout.JAVA_LONG, 0, methodId)
        methods[methodId] = handler

        val methodInfo = GDExtensionClassMethodInfo(
            name = stringNames.get(methodName),
            callFunc = methodCallStub,
            methodFlags = 0,
            hasReturnValue = false,
            returnValueMetadata = 0,
            argumentCount = 0,
            defaultArgumentCount = 0,
            methodUserdata = methodUserdata,
        )

        ffi.classdbRegisterExtensionClassMethod(stringNames.get(className), methodInfo)
    }

    /** Dispatches a ClassDB method call into Java.  */
    interface MethodHandler {
        fun <T> invoke(
            instance: T,
            args: MemorySegment,
            argCount: Long,
            returnValue: MemorySegment,
            error: MemorySegment,
            ffi: GodotFFI,
        )
    }

    /** Defines the ClassDB entry and the factory for creating Java instances.  */
    @JvmRecord
    data class ClassDefinition<out T>(val className: String, val parentClassName: String, val factory: Supplier<out T>)

    @JvmRecord
    private data class ClassEntry<out T>(
        val definition: ClassDefinition<T>,
        val className: MemorySegment,
        val parentName: MemorySegment,
        val classUserdata: MemorySegment,
    )

    @ConsistentCopyVisibility
    private data class InstanceHandle<out T> private constructor(
        val instance: T?,
        val arena: Arena,
        val dataPointer: MemorySegment,
    ) : AutoCloseable by arena {
        val address = dataPointer.address()
        companion object {
            fun <T> create(factory: Supplier<out T>): InstanceHandle<T> {
                val instance: T? = factory.get()
                requireNotNull(instance) { "Instance factory returned null" }
                val arena: Arena = Arena.ofShared()
                val dataPointer: MemorySegment = arena.allocate(ValueLayout.JAVA_LONG)
                dataPointer.set(ValueLayout.JAVA_LONG, 0, dataPointer.address())
                return InstanceHandle(instance, arena, dataPointer)
            }
        }
    }
}
