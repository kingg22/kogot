#include "gdextension_init.h"
#include "jvm_bridge_runtime.h"

static void initialize_jvm(void *userdata, GDExtensionInitializationLevel p_level)
{
    (void)userdata;

    if (!gj_runtime_ensure_main_thread("initialize callback"))
    {
        return;
    }

    switch (p_level)
    {
        case GDEXTENSION_INITIALIZATION_CORE:
            gj_runtime_log_info("Init phase CORE");
            break;
        case GDEXTENSION_INITIALIZATION_SERVERS:
            gj_runtime_log_info("Init phase SERVERS");
            break;
        case GDEXTENSION_INITIALIZATION_SCENE:
            gj_runtime_log_info("Init phase SCENE");
            if (!gj_runtime_ensure_jvm_started())
            {
                gj_runtime_log_error("Failed to initialize JVM at SCENE phase");
                return;
            }
            if (!gj_runtime_ensure_java_bridge_initialized())
            {
                gj_runtime_log_error("GodotBridge.initialize failed");
                gj_runtime_destroy();
                return;
            }
            gj_runtime_call_level_callback(1, p_level);
            break;
        case GDEXTENSION_INITIALIZATION_EDITOR:
            gj_runtime_log_info("Init phase EDITOR");
            if (!gj_runtime_is_jvm_started())
            {
                gj_runtime_log_error("EDITOR phase reached before JVM startup");
                return;
            }
            gj_runtime_call_level_callback(1, p_level);
            break;
        default:
            gj_runtime_log_error("Unknown initialization phase");
            break;
    }
}

static void cleanup_jvm(void *userdata, GDExtensionInitializationLevel p_level)
{
    (void)userdata;

    if (!gj_runtime_ensure_main_thread("deinitialize callback"))
    {
        return;
    }

    switch (p_level)
    {
        case GDEXTENSION_INITIALIZATION_EDITOR:
            gj_runtime_log_info("Deinit phase EDITOR");
            gj_runtime_call_level_callback(0, p_level);
            break;
        case GDEXTENSION_INITIALIZATION_SCENE:
            gj_runtime_log_info("Deinit phase SCENE");
            gj_runtime_call_level_callback(0, p_level);
            break;
        case GDEXTENSION_INITIALIZATION_SERVERS:
            gj_runtime_log_info("Deinit phase SERVERS");
            gj_runtime_call_level_callback(0, p_level);
            break;
        case GDEXTENSION_INITIALIZATION_CORE:
            gj_runtime_log_info("Deinit phase CORE");
            gj_runtime_call_level_callback(0, p_level);
            gj_runtime_destroy();
            break;
        default:
            gj_runtime_log_error("Unknown deinitialization phase");
            break;
    }
}

GDExtensionBool GDE_EXPORT godot_java_bridge_init(
    const GDExtensionInterfaceGetProcAddress p_get_proc_address,
    const GDExtensionClassLibraryPtr p_library,
    GDExtensionInitialization *r_initialization)
{
    if (p_get_proc_address == NULL || p_library == NULL || r_initialization == NULL)
    {
        return false;
    }

    gj_runtime_configure(p_get_proc_address, p_library);
    gj_runtime_capture_main_thread_if_needed();

    gj_runtime_log_info("=== Godot Java Bridge Initialization ===");

    r_initialization->initialize = initialize_jvm;
    r_initialization->deinitialize = cleanup_jvm;
    r_initialization->userdata = NULL;
    r_initialization->minimum_initialization_level = GDEXTENSION_INITIALIZATION_CORE;

    gj_runtime_log_info("Configured phased initialization from CORE to EDITOR");
    return true;
}
