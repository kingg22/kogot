#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include "gdextension_interface.h"

#if !defined(GDE_EXPORT)

#if defined(_WIN32)
#define GDE_EXPORT __declspec(dllexport)
#elif defined(__GNUC__)
#define GDE_EXPORT __attribute__((visibility("default")))
#else
#define GDE_EXPORT
#endif
#endif

static void cleanup_jvm(void *userdata, GDExtensionInitializationLevel p_level);
static void initialize_jvm(void *userdata, GDExtensionInitializationLevel p_level);

GDExtensionBool GDE_EXPORT godot_java_bridge_init(
    GDExtensionInterfaceGetProcAddress p_get_proc_address,
    GDExtensionClassLibraryPtr p_library,
    GDExtensionInitialization *r_initialization
);
