#include "gdextension_init.h"
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
    #include <windows.h>
    #define C_PATH_SEPARATOR ";"
    #define PATH_SEPARATOR "\\"
    #define JVM_LIB_NAME "jvm.dll"
    typedef HMODULE LibHandle;
    #define LOAD_LIB(path) LoadLibraryA(path)
    #define GET_SYMBOL(handle, name) GetProcAddress(handle, name)
    #define CLOSE_LIB(handle) FreeLibrary(handle)
#elif __APPLE__
    #include <dlfcn.h>
    #include <dirent.h>
    #define C_PATH_SEPARATOR ":"
    #define PATH_SEPARATOR "/"
    #define JVM_LIB_NAME "libjvm.dylib"
    typedef void* LibHandle;
    #define LOAD_LIB(path) dlopen(path, RTLD_LAZY | RTLD_GLOBAL)
    #define GET_SYMBOL(handle, name) dlsym(handle, name)
    #define CLOSE_LIB(handle) dlclose(handle)
#else
    #include <dlfcn.h>
    #include <dirent.h>
    #define C_PATH_SEPARATOR ":"
    #define PATH_SEPARATOR "/"
    #define JVM_LIB_NAME "libjvm.so"
    typedef void* LibHandle;
    #define LOAD_LIB(path) dlopen(path, RTLD_LAZY | RTLD_GLOBAL)
    #define GET_SYMBOL(handle, name) dlsym(handle, name)
    #define CLOSE_LIB(handle) dlclose(handle)
#endif

#define GODOT_BRIDGE_JCLASS "io/github/kingg22/godot/internal/initialization/GodotBridge"

// Global JVM state
static JavaVM *g_jvm = NULL;
static JNIEnv *g_env = NULL;
static LibHandle g_jvm_handle = NULL;

// Global Godot functions
static GDExtensionInterfaceGetProcAddress g_get_proc_address = NULL;
static GDExtensionClassLibraryPtr g_library = NULL;
static GDExtensionInterfacePrintError g_print_error = NULL;

// Function pointer type for JNI_CreateJavaVM
typedef jint (JNICALL *JNI_CreateJavaVM_func)(JavaVM **, void **, void *);

// Logging functions
static void log_info(const char *msg) {
    printf("[GDExtension-JVM] %s\n", msg);
}

static void log_error(const char *msg)
{
    if (g_print_error == NULL && g_get_proc_address != NULL)
    {
        g_print_error = (GDExtensionInterfacePrintError)g_get_proc_address("print_error");
    }
    if (g_print_error != NULL)
    {
        g_print_error(msg, __func__, __FILE__, __LINE__, false);
    } else {
        fprintf(stderr, "[GDExtension-JVM] ERROR: %s\n", msg);
    }
}

// Find all jar to load
static int ends_with_jar(const char* name) {
    const size_t len = strlen(name);
    return len > 4 && strcmp(name + len - 4, ".jar") == 0;
}

char* build_classpath_from_dir(const char* dir_path) {
    printf("Going to find all jars in the dir: %s\n", dir_path);
    size_t capacity = 1024;
    size_t length = 0;
    char* result = malloc(capacity);

    if (!result) return NULL;
    result[0] = '\0';

#ifdef _WIN32
    char search_path[MAX_PATH];
    snprintf(search_path, sizeof(search_path), "%s\\*.jar", dir_path);
    WIN32_FIND_DATAA find_data;
    HANDLE hFind = FindFirstFileA(search_path, &find_data);

    if (hFind == INVALID_HANDLE_VALUE) {
        log_error("Invalid directory, can't find any jars")
        return result;
    }

    do {
        const char* name = find_data.cFileName;
        const size_t needed = strlen(dir_path) + 1 + strlen(name) + 2;
        result = realloc(result, capacity);

        if (length > 0) result[length++] = *C_PATH_SEPARATOR;

        length += snprintf(
            result + length,
            capacity - length,
            "%s%c%s",
            dir_path,
            *PATH_SEPARATOR,
            name
        );
    } while (FindNextFileA(hFind, &find_data));
#else
    DIR* dir = opendir(dir_path);
    if (!dir) {
        log_error("Can't explore the directory provided, not found any jars");
        return result;
    }

    struct dirent* entry;

    while ((entry = readdir(dir)) != NULL) {
        if (!ends_with_jar(entry->d_name)) continue;
        const size_t needed = strlen(dir_path) + 1 + strlen(entry->d_name) + 2;
        if (length + needed >= capacity) {
            capacity *= 2;
            result = realloc(result, capacity);
        }

        if (length > 0) result[length++] = *C_PATH_SEPARATOR;

        length += snprintf(
            result + length,
            capacity - length,
            "%s%c%s",
            dir_path,
            *PATH_SEPARATOR,
            entry->d_name
        );
    }
    closedir(dir);
#endif
    return result;
}

// Find and load the JVM library
static LibHandle load_jvm_library(const char **out_path) {
    // Try embedded JRE first (for distribution)
    const char *jvm_paths[] = {
        #ifdef _WIN32
                "addons/java_gdext/bin/jre/bin/server/jvm.dll",
                "../jre/bin/server/jvm.dll",
        #elif __APPLE__
                "addons/java_gdext/bin/jre/lib/server/libjvm.dylib",
                "../jre/lib/server/libjvm.dylib",
        #else
                "addons/java_gdext/bin/jre/lib/server/libjvm.so",
                "addons/java_gdext/bin/jre/lib/amd64/server/libjvm.so",
                "../jre/lib/server/libjvm.so",
        #endif
                NULL
    };

    for (int i = 0; jvm_paths[i] != NULL; i++) {
        const LibHandle handle = LOAD_LIB(jvm_paths[i]);
        if (handle) {
            log_info("JVM library loaded from embedded JRE");
            if (out_path) *out_path = jvm_paths[i];
            return handle;
        }
    }

    // Try JAVA_HOME
    const char *java_home = getenv("JAVA_HOME");
    if (java_home) {
        char path[512];
        #ifdef _WIN32
                snprintf(path, sizeof(path), "%s\\bin\\server\\jvm.dll", java_home);
        #elif __APPLE__
                snprintf(path, sizeof(path), "%s/lib/server/libjvm.dylib", java_home);
        #else
                snprintf(path, sizeof(path), "%s/lib/server/libjvm.so", java_home);
        #endif

        const LibHandle handle = LOAD_LIB(path);
        if (handle) {
            log_info("JVM library loaded from JAVA_HOME");
            if (out_path) *out_path = java_home;
            return handle;
        }
    }

    log_error("Failed to find JVM library. Set JAVA_HOME or include JRE in 'jre/' directory");
    return NULL;
}

// Initialize the JVM with Project Panama support
static int initialize_jvm_with_panama() {
    log_info("Initializing JVM for Project Panama...");

    const char *jvm_path = NULL;
    const LibHandle jvm_handle = load_jvm_library(&jvm_path);
    if (!jvm_handle) {
        return 0;
    }
    g_jvm_handle = jvm_handle;

    // Get JNI_CreateJavaVM function pointer
    const JNI_CreateJavaVM_func create_jvm = GET_SYMBOL(jvm_handle, "JNI_CreateJavaVM");

    if (!create_jvm) {
        log_error("Failed to find JNI_CreateJavaVM symbol");
        return 0;
    }

    // Configure JVM options
    JavaVMInitArgs vm_args;
    JavaVMOption options[16];
    int option_count = 0;

    // 1. Set classpath - point to your compiled JAR
    char* jars = build_classpath_from_dir("addons/java_gdext/bin/lib");
    char jvm_classpath[8192];
    snprintf(
        jvm_classpath,
        sizeof(jvm_classpath),
        "-Djava.class.path=%s",
        jars
    );
    options[option_count++].optionString = jvm_classpath;
    printf("Going to load this classpath: %s\n", jvm_classpath);
    free(jars);

    // 2.
    // For Java 21+
    options[option_count++].optionString = "--enable-native-access=ALL-UNNAMED";

    // 3. Memory settings
    options[option_count++].optionString = "-Xms128m";
    options[option_count++].optionString = "-Xmx1024m";

    // 4. Enable assertions in debug builds
    #ifdef DEBUG_ENABLED
    options[option_count++].optionString = "-ea";
    // options[option_count++].optionString = "-verbose:jni";
    options[option_count++].optionString = "-Xcheck:jni";
    // 7. Enable JDWP in debug builds
    options[option_count++].optionString = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:0";
    // transport TCP socket
    // server yes, the JVM going to listen
    // don't await to connection in the startup
    // port
    #endif

    // 5. GC options (optional, tune as needed)
    options[option_count++].optionString = "-XX:+UseG1GC";

    // 6. Set library path for native dependencies
    char library_path[512];
    #ifdef _WIN32
    snprintf(library_path, sizeof(library_path), "-Djava.library.path=lib;.");
    #else
    snprintf(library_path, sizeof(library_path), "-Djava.library.path=lib:.");
    #endif
    options[option_count++].optionString = library_path;

    vm_args.version = JNI_VERSION_21;  // Java 21 for full Panama support
    vm_args.nOptions = option_count;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = JNI_FALSE;

    // Create the JVM
    log_info("Creating JVM instance...");
    const jint result = create_jvm(&g_jvm, (void**)&g_env, &vm_args);

    if (result != JNI_OK) {
        log_error("Failed to create JVM");
        printf("JNI Error code: %d\n", result);
        return 0;
    }

    log_info("JVM created successfully with Project Panama enabled!");
    return 1;
}

// Call Java initialization - pass GDExtension interface pointers
static int initialize_java_bridge() {
    if (!g_env) {
        log_error("JNIEnv is NULL");
        return 0;
    }

    log_info("Initializing Java bridge...");

    // Find your main bridge class
    const jclass bridgeClass = (*g_env)->FindClass(g_env, GODOT_BRIDGE_JCLASS);
    if (!bridgeClass) {
        log_error("Failed to find GodotBridge class");
        (*g_env)->ExceptionDescribe(g_env);
        return 0;
    }

    // Find the initialize method that accepts the GDExtension pointers
    // Signature: (JJ)V where J = long (for pointers)
    const jmethodID initMethod = (*g_env)->GetStaticMethodID(
        g_env,
        bridgeClass,
        "initialize",
        "(JJ)V"  // (getProcAddress pointer, library pointer) -> void
    );

    if (!initMethod) {
        log_error("Failed to find initialize(long, long) method");
        (*g_env)->ExceptionDescribe(g_env);
        (*g_env)->DeleteLocalRef(g_env, bridgeClass);
        return 0;
    }

    // Pass the GDExtension interface as pointers to Java
    const jlong proc_addr = (jlong)(uintptr_t)g_get_proc_address;
    const jlong lib_ptr = (jlong)(uintptr_t)g_library;

    log_info("Calling Java GodotBridge.initialize()...");
    (*g_env)->CallStaticVoidMethod(g_env, bridgeClass, initMethod, proc_addr, lib_ptr);

    if ((*g_env)->ExceptionCheck(g_env)) {
        log_error("Exception in Java initialize method");
        (*g_env)->ExceptionDescribe(g_env);
        (*g_env)->DeleteLocalRef(g_env, bridgeClass);
        return 0;
    }

    (*g_env)->DeleteLocalRef(g_env, bridgeClass);
    log_info("Java bridge initialized successfully!");
    return 1;
}

// Cleanup function called by GDExtension
static void cleanup_jvm(const void *userdata, const GDExtensionInitializationLevel p_level) {
    (void) userdata;  // acknowledge unreferenced parameter
    if (p_level != GDEXTENSION_INITIALIZATION_SCENE) {
        return;
    }

    if (g_jvm) {
        log_info("Shutting down JVM...");

        // Call Java cleanup if available
        if (g_env) {
            const jclass bridgeClass = (*g_env)->FindClass(g_env, GODOT_BRIDGE_JCLASS);
            if (bridgeClass) {
                const jmethodID shutdownMethod = (*g_env)->GetStaticMethodID(
                    g_env, bridgeClass, "shutdown", "()V"
                );
                if (shutdownMethod) {
                    (*g_env)->CallStaticVoidMethod(g_env, bridgeClass, shutdownMethod);
                }
                (*g_env)->DeleteLocalRef(g_env, bridgeClass);
            }
        }

        (*g_jvm)->DestroyJavaVM(g_jvm);
        g_jvm = NULL;
        g_env = NULL;
        if (g_jvm_handle) {
            CLOSE_LIB(g_jvm_handle);
            g_jvm_handle = NULL;
        }
        log_info("JVM destroyed");
    }
}

static void initialize_jvm(const void *userdata, const GDExtensionInitializationLevel p_level) {
    (void) userdata;  // acknowledge unreferenced parameter
    if (p_level != GDEXTENSION_INITIALIZATION_SCENE) {
        return;
    }

    // Initialize JVM with Project Panama support
    if (!initialize_jvm_with_panama()) {
        log_error("Failed to initialize JVM");
    }

    // Initialize Java bridge and pass GDExtension pointers
    if (!initialize_java_bridge()) {
        log_error("Failed to initialize Java bridge");
        cleanup_jvm(NULL, GDEXTENSION_INITIALIZATION_SCENE);
    }
}

// Main GDExtension initialization entry point
GDExtensionBool GDE_EXPORT godot_java_bridge_init(
    const GDExtensionInterfaceGetProcAddress p_get_proc_address,
    const GDExtensionClassLibraryPtr p_library,
    GDExtensionInitialization *r_initialization
) {
    log_info("=== Godot Java Bridge Initialization ===");

    // Store GDExtension interface pointers
    g_get_proc_address = p_get_proc_address;
    g_library = p_library;

    // Setup GDExtension lifecycle callbacks
    r_initialization->initialize = initialize_jvm;
    r_initialization->deinitialize = cleanup_jvm;
    r_initialization->userdata = NULL;
    r_initialization->minimum_initialization_level = GDEXTENSION_INITIALIZATION_SCENE;

    log_info("=== Initialization Complete ===");
    return true;
}
