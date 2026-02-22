package io.github.kingg22.godot.internal.bridge;

import io.github.kingg22.godot.internal.ffm.GDExtensionGodotVersion2;
import io.github.kingg22.godot.internal.ffm.GDExtensionInterfaceClassdbConstructObject2;
import io.github.kingg22.godot.internal.ffm.GDExtensionInterfaceClassdbRegisterExtensionClass5;
import io.github.kingg22.godot.internal.ffm.GDExtensionInterfaceClassdbRegisterExtensionClassMethod;
import io.github.kingg22.godot.internal.ffm.GDExtensionInterfaceGetGodotVersion2;
import io.github.kingg22.godot.internal.ffm.GDExtensionInterfaceGetProcAddress;
import io.github.kingg22.godot.internal.ffm.GDExtensionInterfaceObjectSetInstance;
import io.github.kingg22.godot.internal.ffm.GDExtensionInterfaceObjectSetScriptInstance;
import io.github.kingg22.godot.internal.ffm.GDExtensionInterfaceScriptInstanceCreate3;
import io.github.kingg22.godot.internal.ffm.GDExtensionInterfaceStringNameNewWithUtf8Chars;
import io.github.kingg22.godot.internal.ffm.GDExtensionInterfaceVariantNewNil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Low-level GDExtension function accessors. */
final class GodotFFI {
    private final MemorySegment getProcAddress;
    private final MemorySegment library;
    private final Arena arena;
    private final Map<String, MemorySegment> cache = new ConcurrentHashMap<>();

    private final MemorySegment fnStringNameNewWithUtf8Chars;
    private final MemorySegment fnClassdbRegisterExtensionClass5;
    private final MemorySegment fnClassdbRegisterExtensionClassMethod;
    private final MemorySegment fnClassdbConstructObject2;
    private final MemorySegment fnObjectSetInstance;
    private final MemorySegment fnVariantNewNil;
    private final MemorySegment fnScriptInstanceCreate3;
    private final MemorySegment fnObjectSetScriptInstance;
    private final MemorySegment fnGetGodotVersion2;

    GodotFFI(final MemorySegment getProcAddress, final MemorySegment library, final Arena arena) {
        this.getProcAddress = getProcAddress;
        this.library = library;
        this.arena = arena;

        this.fnStringNameNewWithUtf8Chars = lookup("string_name_new_with_utf8_chars");
        this.fnClassdbRegisterExtensionClass5 = lookup("classdb_register_extension_class5");
        this.fnClassdbRegisterExtensionClassMethod = lookup("classdb_register_extension_class_method");
        this.fnClassdbConstructObject2 = lookup("classdb_construct_object2");
        this.fnObjectSetInstance = lookup("object_set_instance");
        this.fnVariantNewNil = lookup("variant_new_nil");
        this.fnScriptInstanceCreate3 = lookup("script_instance_create3");
        this.fnObjectSetScriptInstance = lookup("object_set_script_instance");
        this.fnGetGodotVersion2 = lookup("get_godot_version2");
    }

    MemorySegment library() {
        return library;
    }

    MemorySegment lookup(final String name) {
        return cache.computeIfAbsent(name, key -> {
            final var cName = arena.allocateFrom(key);
            final var fnPtr = GDExtensionInterfaceGetProcAddress.invoke(getProcAddress, cName);
            if (MemorySegment.NULL.equals(fnPtr)) {
                throw new IllegalStateException("Failed to lookup function named: '" + key + "'");
            }
            return fnPtr;
        });
    }

    void stringNameNewWithUtf8Chars(final MemorySegment dest, final MemorySegment cString) {
        GDExtensionInterfaceStringNameNewWithUtf8Chars.invoke(fnStringNameNewWithUtf8Chars, dest, cString);
    }

    void classdbRegisterExtensionClass5(
            final MemorySegment className, final MemorySegment parentClassName, final MemorySegment creationInfo) {
        GDExtensionInterfaceClassdbRegisterExtensionClass5.invoke(
                fnClassdbRegisterExtensionClass5, library, className, parentClassName, creationInfo);
    }

    void classdbRegisterExtensionClassMethod(final MemorySegment className, final MemorySegment methodInfo) {
        GDExtensionInterfaceClassdbRegisterExtensionClassMethod.invoke(
                fnClassdbRegisterExtensionClassMethod, library, className, methodInfo);
    }

    MemorySegment classdbConstructObject2(final MemorySegment className) {
        return GDExtensionInterfaceClassdbConstructObject2.invoke(fnClassdbConstructObject2, className);
    }

    void objectSetInstance(
            final MemorySegment objectPtr, final MemorySegment className, final MemorySegment instancePtr) {
        GDExtensionInterfaceObjectSetInstance.invoke(fnObjectSetInstance, objectPtr, className, instancePtr);
    }

    void variantNewNil(final MemorySegment dest) {
        GDExtensionInterfaceVariantNewNil.invoke(fnVariantNewNil, dest);
    }

    MemorySegment scriptInstanceCreate3(final MemorySegment info, final MemorySegment instanceData) {
        return GDExtensionInterfaceScriptInstanceCreate3.invoke(fnScriptInstanceCreate3, info, instanceData);
    }

    void objectSetScriptInstance(final MemorySegment objectPtr, final MemorySegment scriptInstance) {
        GDExtensionInterfaceObjectSetScriptInstance.invoke(fnObjectSetScriptInstance, objectPtr, scriptInstance);
    }

    GDExtensionGodotVersion2 getGodotVersion2() {
        final var pointer = arena.allocate(GDExtensionGodotVersion2.layout());
        GDExtensionInterfaceGetGodotVersion2.invoke(fnGetGodotVersion2, pointer);
        return GDExtensionGodotVersion2.parse(pointer);
    }
}
