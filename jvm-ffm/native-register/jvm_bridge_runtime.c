#include "jvm_bridge_runtime.h"
#include "gdextension_init.h"

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
    #include <windows.h>
    #define C_PATH_SEPARATOR ";"
    #define PATH_SEPARATOR "\\"
    typedef HMODULE LibHandle;
    #define LOAD_LIB(path) LoadLibraryA(path)
    #define GET_SYMBOL(handle, name) GetProcAddress(handle, name)
    #define CLOSE_LIB(handle) FreeLibrary(handle)
    typedef DWORD ThreadId;
    static ThreadId get_current_thread_id(void) { return GetCurrentThreadId(); }
    static int thread_id_equals(const ThreadId a, const ThreadId b) { return a == b; }
#else
    #include <dlfcn.h>
    #include <dirent.h>
    #include <pthread.h>
    #define C_PATH_SEPARATOR ":"
    #define PATH_SEPARATOR "/"
    typedef void *LibHandle;
    #define LOAD_LIB(path) dlopen(path, RTLD_LAZY | RTLD_GLOBAL)
    #define GET_SYMBOL(handle, name) dlsym(handle, name)
    #define CLOSE_LIB(handle) dlclose(handle)
    typedef pthread_t ThreadId;
    static ThreadId get_current_thread_id(void) { return pthread_self(); }
    static int thread_id_equals(const ThreadId a, const ThreadId b) { return pthread_equal(a, b); }
#endif

#define GODOT_BRIDGE_JCLASS "io/github/kingg22/godot/internal/initialization/GodotBridge"

typedef jint(JNICALL *JNI_CreateJavaVM_func)(JavaVM **, void **, void *);

static JavaVM *g_jvm = NULL;
static LibHandle g_jvm_handle = NULL;
static int g_jvm_started = 0;
static int g_java_runtime_initialized = 0;

static jclass g_bridge_class = NULL;
static jmethodID g_mid_initialize = NULL;
static jmethodID g_mid_shutdown = NULL;
static jmethodID g_mid_on_level_init = NULL;
static jmethodID g_mid_on_level_deinit = NULL;

static GDExtensionInterfaceGetProcAddress g_get_proc_address = NULL;
static GDExtensionClassLibraryPtr g_library = NULL;
static GDExtensionInterfacePrintError g_print_error = NULL;

static int g_main_thread_set = 0;
static ThreadId g_main_thread_id;

void gj_runtime_configure(
    const GDExtensionInterfaceGetProcAddress get_proc_address,
    const GDExtensionClassLibraryPtr library
)
{
    g_get_proc_address = get_proc_address;
    g_library = library;
}

void gj_runtime_log_info(const char *msg)
{
    printf("[GDExtension-JVM] %s\n", msg);
}

void gj_runtime_log_error(const char *msg)
{
    if (g_print_error == NULL && g_get_proc_address != NULL)
    {
        g_print_error = (GDExtensionInterfacePrintError)g_get_proc_address("print_error");
    }

    if (g_print_error != NULL)
    {
        g_print_error(msg, __func__, __FILE__, __LINE__, false);
    }
    else
    {
        fprintf(stderr, "[GDExtension-JVM] ERROR: %s\n", msg);
    }
}

static void log_error_code(const char *prefix, const int code)
{
    char buffer[256];
    snprintf(buffer, sizeof(buffer), "%s (code=%d)", prefix, code);
    gj_runtime_log_error(buffer);
}

void gj_runtime_capture_main_thread_if_needed(void)
{
    if (!g_main_thread_set)
    {
        g_main_thread_id = get_current_thread_id();
        g_main_thread_set = 1;
        gj_runtime_log_info("Captured GDExtension main thread id");
    }
}

static int is_main_thread(void)
{
    if (!g_main_thread_set)
    {
        return 0;
    }
    return thread_id_equals(get_current_thread_id(), g_main_thread_id);
}

int gj_runtime_ensure_main_thread(const char *context)
{
    if (is_main_thread())
    {
        return 1;
    }

    char message[256];
    snprintf(message, sizeof(message), "%s must run on Godot main thread", context);
    gj_runtime_log_error(message);
    return 0;
}

static int ends_with_jar(const char *name)
{
    const size_t len = strlen(name);
    return len > 4 && strcmp(name + len - 4, ".jar") == 0;
}

static int append_classpath_entry(char **result, size_t *capacity, size_t *length, const char *dir_path, const char *name)
{
    const size_t needed = strlen(dir_path) + strlen(name) + 3;

    while ((*length + needed + 1) > *capacity)
    {
        const size_t new_capacity = (*capacity) * 2;
        char *expanded = realloc(*result, new_capacity);
        if (expanded == NULL)
        {
            gj_runtime_log_error("Failed to expand classpath buffer");
            return 0;
        }
        *result = expanded;
        *capacity = new_capacity;
    }

    if (*length > 0)
    {
        (*result)[(*length)++] = *C_PATH_SEPARATOR;
    }

    *length += (size_t)snprintf(
        *result + *length,
        *capacity - *length,
        "%s%c%s",
        dir_path,
        *PATH_SEPARATOR,
        name
    );

    return 1;
}

static char *build_classpath_from_dir(const char *dir_path)
{
    size_t capacity = 1024;
    size_t length = 0;
    char *result = malloc(capacity);

    if (result == NULL)
    {
        gj_runtime_log_error("Failed to allocate classpath buffer");
        return NULL;
    }
    result[0] = '\0';

#ifdef _WIN32
    char search_path[MAX_PATH];
    snprintf(search_path, sizeof(search_path), "%s\\*.jar", dir_path);

    WIN32_FIND_DATAA find_data;
    HANDLE handle = FindFirstFileA(search_path, &find_data);
    if (handle == INVALID_HANDLE_VALUE)
    {
        gj_runtime_log_error("No JAR files found in classpath directory");
        return result;
    }

    do
    {
        if (!append_classpath_entry(&result, &capacity, &length, dir_path, find_data.cFileName))
        {
            FindClose(handle);
            free(result);
            return NULL;
        }
    } while (FindNextFileA(handle, &find_data));

    FindClose(handle);
#else
    DIR *dir = opendir(dir_path);
    if (dir == NULL)
    {
        gj_runtime_log_error("Classpath directory not found; continuing with empty classpath");
        return result;
    }

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL)
    {
        if (!ends_with_jar(entry->d_name))
        {
            continue;
        }

        if (!append_classpath_entry(&result, &capacity, &length, dir_path, entry->d_name))
        {
            closedir(dir);
            free(result);
            return NULL;
        }
    }

    closedir(dir);
#endif

    return result;
}

static LibHandle load_jvm_library(const char **out_path)
{
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
        NULL,
    };

    for (int i = 0; jvm_paths[i] != NULL; i++)
    {
        const LibHandle handle = LOAD_LIB(jvm_paths[i]);
        if (handle != NULL)
        {
            gj_runtime_log_info("JVM library loaded from embedded JRE");
            if (out_path != NULL)
            {
                *out_path = jvm_paths[i];
            }
            return handle;
        }
    }

    const char *java_home = getenv("JAVA_HOME");
    if (java_home != NULL)
    {
        char path[512];
#ifdef _WIN32
        snprintf(path, sizeof(path), "%s\\bin\\server\\jvm.dll", java_home);
#elif __APPLE__
        snprintf(path, sizeof(path), "%s/lib/server/libjvm.dylib", java_home);
#else
        snprintf(path, sizeof(path), "%s/lib/server/libjvm.so", java_home);
#endif

        const LibHandle handle = LOAD_LIB(path);
        if (handle != NULL)
        {
            gj_runtime_log_info("JVM library loaded from JAVA_HOME");
            if (out_path != NULL)
            {
                *out_path = path;
            }
            return handle;
        }
    }

    gj_runtime_log_error("Failed to find JVM library. Set JAVA_HOME or include JRE in addons/java_gdext/bin/jre");
    return NULL;
}

static int get_jni_env(JNIEnv **out_env, int *did_attach)
{
    if (out_env == NULL || did_attach == NULL)
    {
        gj_runtime_log_error("Invalid JNI env output arguments");
        return 0;
    }

    *out_env = NULL;
    *did_attach = 0;

    if (g_jvm == NULL)
    {
        gj_runtime_log_error("JVM is not available");
        return 0;
    }

    const jint status = (*g_jvm)->GetEnv(g_jvm, (void **)out_env, JNI_VERSION_1_8);
    if (status == JNI_OK)
    {
        return 1;
    }

    if (status == JNI_EDETACHED)
    {
        const jint attach_result = (*g_jvm)->AttachCurrentThread(g_jvm, (void **)out_env, NULL);
        if (attach_result != JNI_OK)
        {
            log_error_code("AttachCurrentThread failed", attach_result);
            return 0;
        }

        *did_attach = 1;
        return 1;
    }

    log_error_code("GetEnv failed", status);
    return 0;
}

static void release_jni_env(const int did_attach)
{
    if (did_attach && g_jvm != NULL)
    {
        const jint detach_result = (*g_jvm)->DetachCurrentThread(g_jvm);
        if (detach_result != JNI_OK)
        {
            log_error_code("DetachCurrentThread failed", detach_result);
        }
    }
}

static int check_and_clear_java_exception(JNIEnv *env, const char *context)
{
    if (!(*env)->ExceptionCheck(env))
    {
        return 1;
    }

    char buffer[256];
    snprintf(buffer, sizeof(buffer), "Java exception during %s", context);
    gj_runtime_log_error(buffer);
    (*env)->ExceptionDescribe(env);
    (*env)->ExceptionClear(env);
    return 0;
}

static int cache_bridge_metadata(JNIEnv *env)
{
    if (g_bridge_class != NULL)
    {
        return 1;
    }

    const jclass local_class = (*env)->FindClass(env, GODOT_BRIDGE_JCLASS);
    if (local_class == NULL)
    {
        check_and_clear_java_exception(env, "FindClass(GodotBridge)");
        return 0;
    }

    g_bridge_class = (*env)->NewGlobalRef(env, local_class);
    (*env)->DeleteLocalRef(env, local_class);

    if (g_bridge_class == NULL)
    {
        gj_runtime_log_error("Failed to create global ref for GodotBridge class");
        return 0;
    }

    g_mid_initialize = (*env)->GetStaticMethodID(env, g_bridge_class, "initialize", "(JJ)V");
    if (g_mid_initialize == NULL)
    {
        check_and_clear_java_exception(env, "GetStaticMethodID initialize(JJ)V");
        return 0;
    }

    g_mid_shutdown = (*env)->GetStaticMethodID(env, g_bridge_class, "shutdown", "()V");
    if (g_mid_shutdown == NULL)
    {
        check_and_clear_java_exception(env, "GetStaticMethodID shutdown()V");
        return 0;
    }

    g_mid_on_level_init = (*env)->GetStaticMethodID(env, g_bridge_class, "onInitializationLevel", "(S)V");
    if (g_mid_on_level_init == NULL)
    {
        check_and_clear_java_exception(env, "GetStaticMethodID onInitializationLevel(S)V");
        return 0;
    }

    g_mid_on_level_deinit = (*env)->GetStaticMethodID(env, g_bridge_class, "onDeinitializationLevel", "(S)V");
    if (g_mid_on_level_deinit == NULL)
    {
        check_and_clear_java_exception(env, "GetStaticMethodID onDeinitializationLevel(S)V");
        return 0;
    }

    return 1;
}

int gj_runtime_is_jvm_started(void)
{
    return g_jvm_started;
}

int gj_runtime_ensure_jvm_started(void)
{
    if (g_jvm_started)
    {
        return 1;
    }

    gj_runtime_log_info("Initializing JVM for Java bridge...");

    const char *jvm_path = NULL;
    const LibHandle jvm_handle = load_jvm_library(&jvm_path);
    if (jvm_handle == NULL)
    {
        return 0;
    }
    g_jvm_handle = jvm_handle;

    const JNI_CreateJavaVM_func create_jvm = (JNI_CreateJavaVM_func)GET_SYMBOL(jvm_handle, "JNI_CreateJavaVM");
    if (create_jvm == NULL)
    {
        gj_runtime_log_error("Failed to resolve JNI_CreateJavaVM symbol");
        return 0;
    }

    JavaVMInitArgs vm_args;
    JavaVMOption options[16];
    int option_count = 0;

    char *jars = build_classpath_from_dir("addons/java_gdext/bin/lib");
    if (jars == NULL)
    {
        gj_runtime_log_error("Classpath allocation failed");
        return 0;
    }

    char jvm_classpath[8192];
    snprintf(jvm_classpath, sizeof(jvm_classpath), "-Djava.class.path=%s", jars);
    options[option_count++].optionString = jvm_classpath;
    free(jars);

    options[option_count++].optionString = "--enable-native-access=ALL-UNNAMED";
    options[option_count++].optionString = "-Xms128m";
    options[option_count++].optionString = "-Xmx1024m";

#ifdef DEBUG_ENABLED
    options[option_count++].optionString = "-ea";
    options[option_count++].optionString = "-Xcheck:jni";
    options[option_count++].optionString = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:0";
#endif

    options[option_count++].optionString = "-XX:+UseG1GC";

    char library_path[512];
#ifdef _WIN32
    snprintf(library_path, sizeof(library_path), "-Djava.library.path=lib;.");
#else
    snprintf(library_path, sizeof(library_path), "-Djava.library.path=lib:.");
#endif
    options[option_count++].optionString = library_path;

    vm_args.version = JNI_VERSION_1_8;
    vm_args.nOptions = option_count;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = JNI_FALSE;

    JNIEnv *env = NULL;
    const jint create_result = create_jvm(&g_jvm, (void **)&env, &vm_args);
    if (create_result != JNI_OK)
    {
        log_error_code("Failed to create JVM", create_result);
        g_jvm = NULL;
        return 0;
    }

    if (!cache_bridge_metadata(env))
    {
        gj_runtime_log_error("Failed to cache Java bridge metadata");
        return 0;
    }

    g_jvm_started = 1;
    gj_runtime_log_info("JVM created successfully");
    return 1;
}

int gj_runtime_ensure_java_bridge_initialized(void)
{
    if (g_java_runtime_initialized)
    {
        return 1;
    }

    if (g_jvm == NULL)
    {
        gj_runtime_log_error("Cannot initialize Java bridge: JVM not started");
        return 0;
    }

    JNIEnv *env = NULL;
    int did_attach = 0;
    if (!get_jni_env(&env, &did_attach))
    {
        return 0;
    }

    if (!cache_bridge_metadata(env))
    {
        release_jni_env(did_attach);
        return 0;
    }

    const jlong proc_addr = (jlong)(uintptr_t)g_get_proc_address;
    const jlong lib_ptr = (jlong)(uintptr_t)g_library;

    (*env)->CallStaticVoidMethod(env, g_bridge_class, g_mid_initialize, proc_addr, lib_ptr);
    const int ok = check_and_clear_java_exception(env, "GodotBridge.initialize");

    release_jni_env(did_attach);

    if (ok)
    {
        g_java_runtime_initialized = 1;
    }
    return ok;
}

void gj_runtime_call_level_callback(const int is_init, const GDExtensionInitializationLevel level)
{
    if (g_jvm == NULL || g_bridge_class == NULL)
    {
        return;
    }

    JNIEnv *env = NULL;
    int did_attach = 0;
    if (!get_jni_env(&env, &did_attach))
    {
        return;
    }

    if (!cache_bridge_metadata(env))
    {
        release_jni_env(did_attach);
        return;
    }

    const jmethodID method = is_init ? g_mid_on_level_init : g_mid_on_level_deinit;
    (*env)->CallStaticVoidMethod(env, g_bridge_class, method, (jshort)level);

    if (is_init)
    {
        check_and_clear_java_exception(env, "GodotBridge.onInitializationLevel");
    }
    else
    {
        check_and_clear_java_exception(env, "GodotBridge.onDeinitializationLevel");
    }

    release_jni_env(did_attach);
}

static void call_java_shutdown(void)
{
    if (g_jvm == NULL || g_bridge_class == NULL)
    {
        return;
    }

    JNIEnv *env = NULL;
    int did_attach = 0;
    if (!get_jni_env(&env, &did_attach))
    {
        return;
    }

    if (!cache_bridge_metadata(env))
    {
        release_jni_env(did_attach);
        return;
    }

    (*env)->CallStaticVoidMethod(env, g_bridge_class, g_mid_shutdown);
    check_and_clear_java_exception(env, "GodotBridge.shutdown");
    release_jni_env(did_attach);
}

void gj_runtime_destroy(void)
{
    if (g_jvm == NULL)
    {
        return;
    }

    call_java_shutdown();

    JNIEnv *env = NULL;
    int did_attach = 0;
    if (get_jni_env(&env, &did_attach))
    {
        if (g_bridge_class != NULL)
        {
            (*env)->DeleteGlobalRef(env, g_bridge_class);
            g_bridge_class = NULL;
        }
        release_jni_env(did_attach);
    }

    const jint destroy_result = (*g_jvm)->DestroyJavaVM(g_jvm);
    if (destroy_result != JNI_OK)
    {
        log_error_code("DestroyJavaVM failed", destroy_result);
    }

    g_jvm = NULL;
    g_jvm_started = 0;
    g_java_runtime_initialized = 0;
    g_mid_initialize = NULL;
    g_mid_shutdown = NULL;
    g_mid_on_level_init = NULL;
    g_mid_on_level_deinit = NULL;

    if (g_jvm_handle != NULL)
    {
        CLOSE_LIB(g_jvm_handle);
        g_jvm_handle = NULL;
    }

    gj_runtime_log_info("JVM destroyed");
}
