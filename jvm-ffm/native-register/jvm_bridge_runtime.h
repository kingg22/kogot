#pragma once

#include <stdint.h>

#include "gdextension_interface.h"

void gj_runtime_configure(
    GDExtensionInterfaceGetProcAddress get_proc_address,
    GDExtensionClassLibraryPtr library
);

void gj_runtime_capture_main_thread_if_needed(void);
int gj_runtime_ensure_main_thread(const char *context);

void gj_runtime_log_info(const char *msg);
void gj_runtime_log_error(const char *msg);

int gj_runtime_is_jvm_started(void);
int gj_runtime_ensure_jvm_started(void);
int gj_runtime_ensure_java_bridge_initialized(void);

void gj_runtime_call_level_callback(int is_init, GDExtensionInitializationLevel level);
void gj_runtime_destroy(void);
