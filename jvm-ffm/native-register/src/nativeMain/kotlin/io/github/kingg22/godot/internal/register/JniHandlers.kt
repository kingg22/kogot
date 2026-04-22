package io.github.kingg22.godot.internal.register

import io.github.kingg22.godot.internal.register.jni.ffi.JNIEnvVar
import io.github.kingg22.godot.internal.register.jni.ffi.JNI_EDETACHED
import io.github.kingg22.godot.internal.register.jni.ffi.JNI_OK
import io.github.kingg22.godot.internal.register.jni.ffi.JNI_VERSION_1_8
import io.github.kingg22.godot.internal.register.jni.ffi.JavaVMInitArgs
import io.github.kingg22.godot.internal.register.jni.ffi.JavaVMOption
import io.github.kingg22.godot.internal.register.jni.ffi.JavaVMVar
import io.github.kingg22.godot.internal.register.jni.ffi.jint
import kotlinx.cinterop.*
import platform.posix.closedir
import platform.posix.fprintf
import platform.posix.getenv
import platform.posix.opendir
import platform.posix.pthread_self
import platform.posix.readdir
import platform.posix.stderr
import platform.windows.GetCurrentThreadId

private val pathSeparator = if (isWindows) "\\" else "/"
private val classPathSeparator = if (isWindows) ";" else ":"

// --- Utilidades de Logging ---

fun logInfo(msg: String) {
    println("[GDExtension-JVM] $msg")
}

fun logError(msg: String) {
    val state = RuntimeState
    if (state.printError == null && state.getProcAddress != null) {
        state.printError = memScoped { state.getProcAddress!!("print_error".cstr.ptr)?.reinterpret() }
    }

    state.printError?.let { printFn ->
        memScoped {
            printFn(msg.cstr.ptr, "kotlin_init".cstr.ptr, "GodotJavaBridge.kt".cstr.ptr, 0, 0u)
        }
    } ?: run {
        fprintf(stderr, "[GDExtension-JVM] ERROR: %s\n", msg)
    }
}

// --- Gestión de Hilos ---

fun captureMainThread() {
    if (!RuntimeState.mainThreadSet) {
        RuntimeState.mainThreadId = if (isWindows) {
            GetCurrentThreadId().toULong()
        } else {
            pthread_self().toLong().toULong()
        }
        RuntimeState.mainThreadSet = true
        logInfo("Captured GDExtension main thread id")
    }
}

fun isMainThread(): Boolean {
    if (!RuntimeState.mainThreadSet) return false
    val current = if (isWindows) {
        GetCurrentThreadId().toULong()
    } else {
        pthread_self().toLong().toULong()
    }
    return current == RuntimeState.mainThreadId
}

fun ensureMainThread(context: String): Boolean {
    if (isMainThread()) return true
    logError("$context must run on Godot main thread")
    return false
}

// --- Buscador de JVM y Classpath ---

fun buildClasspath(dirPath: String): String {
    val result = StringBuilder()
    val dir = opendir(dirPath) ?: return ""
    try {
        while (true) {
            val entry = readdir(dir) ?: break
            val name = entry.pointed.d_name.toKString()
            if (name.endsWith(".jar")) {
                if (result.isNotEmpty()) result.append(classPathSeparator)
                result.append(dirPath).append(pathSeparator).append(name)
            }
        }
    } finally {
        closedir(dir)
    }
    return result.toString()
}

fun findJvmLib(): Pair<COpaquePointer?, String?> {
    val paths = when {
        isWindows -> listOf("addons/java_gdext/bin/jre/bin/server/jvm.dll", "../jre/bin/server/jvm.dll")

        isMacOS -> listOf(
            "addons/java_gdext/bin/jre/lib/server/libjvm.dylib",
            "../jre/lib/server/libjvm.dylib",
        )

        else -> listOf(
            "addons/java_gdext/bin/jre/lib/server/libjvm.so",
            "addons/java_gdext/bin/jre/lib/amd64/server/libjvm.so",
            "../jre/lib/server/libjvm.so",
        )
    }

    for (path in paths) {
        loadLibrary(path)?.let { return it to path }
    }

    val javaHome = getenv("JAVA_HOME")?.toKString()
    if (javaHome != null) {
        val path = when {
            isWindows -> "$javaHome\\bin\\server\\jvm.dll"
            isMacOS -> "$javaHome/lib/server/libjvm.dylib"
            else -> "$javaHome/lib/server/libjvm.so"
        }
        loadLibrary(path)?.let { return it to path }
    }
    return null to null
}

// --- JNI Helpers ---

fun getJniEnv(): Pair<CPointer<JNIEnvVar>?, Boolean> {
    val vm = RuntimeState.jvm ?: return null to false

    return memScoped {
        val envPtr = alloc<CPointerVar<JNIEnvVar>>()
        val vmPtr = vm.pointed.value!!.pointed

        val status = vmPtr.GetEnv!!(vm, envPtr.ptr.reinterpret(), JNI_VERSION_1_8)
        if (status == JNI_OK) return envPtr.value to false

        if (status == JNI_EDETACHED) {
            if (vmPtr.AttachCurrentThread!!(vm, envPtr.ptr.reinterpret(), null) == JNI_OK) {
                return envPtr.value to true
            }
        }
        null to false
    }
}

fun releaseJniEnv(attached: Boolean) {
    if (attached) {
        val _ = RuntimeState.jvm?.pointed?.value?.pointed?.DetachCurrentThread?.invoke(RuntimeState.jvm)
    }
}

fun checkException(env: CPointer<JNIEnvVar>, context: String): Boolean {
    val envPtr = env.pointed.value!!.pointed
    if (envPtr.ExceptionCheck!!(env) != 0.toUByte()) {
        logError("Java exception during $context")
        envPtr.ExceptionDescribe!!(env)
        envPtr.ExceptionClear!!(env)
        return false
    }
    return true
}

fun cacheMetadata(env: CPointer<JNIEnvVar>): Boolean {
    if (RuntimeState.bridgeClass != null) return true
    val envPtr = env.pointed.value!!.pointed

    memScoped {
        val className = "io/github/kingg22/godot/internal/initialization/GodotBridge".cstr
        val localClass = envPtr.FindClass!!(env, className.ptr) ?: return false

        RuntimeState.bridgeClass = envPtr.NewGlobalRef!!(env, localClass)
        envPtr.DeleteLocalRef!!(env, localClass)

        val midInit = envPtr.GetStaticMethodID!!(env, RuntimeState.bridgeClass, "initialize".cstr.ptr, "(JJ)V".cstr.ptr)
        val midShutdown = envPtr.GetStaticMethodID!!(env, RuntimeState.bridgeClass, "shutdown".cstr.ptr, "()V".cstr.ptr)
        val midLevelInit = envPtr.GetStaticMethodID!!(
            env,
            RuntimeState.bridgeClass,
            "onInitializationLevel".cstr.ptr,
            "(S)V".cstr.ptr,
        )
        val midLevelDeinit = envPtr.GetStaticMethodID!!(
            env,
            RuntimeState.bridgeClass,
            "onDeinitializationLevel".cstr.ptr,
            "(S)V".cstr.ptr,
        )

        if (midInit == null || midShutdown == null || midLevelInit == null || midLevelDeinit == null) return false

        RuntimeState.midInitialize = midInit
        RuntimeState.midShutdown = midShutdown
        RuntimeState.midOnLevelInit = midLevelInit
        RuntimeState.midOnLevelDeinit = midLevelDeinit
    }
    return true
}

// --- Implementación del Ciclo de Vida ---

private typealias CreateJvm = CPointer<CFunction<(CPointerVar<JavaVMVar>, CPointerVar<CPointerVar<JNIEnvVar>>, COpaquePointer?) -> jint>>

fun ensureJvmStarted(): Boolean {
    if (RuntimeState.jvmStarted) return true

    logInfo("Initializing JVM for Java bridge...")
    val (handle, path) = findJvmLib()
    if (handle == null) {
        logError("Could not find JVM library.")
        return false
    }
    RuntimeState.jvmHandle = handle

    val createJvmAddr = getSymbol(handle, "JNI_CreateJavaVM") ?: return false
    val createJvm: CreateJvm = createJvmAddr.reinterpret()

    memScoped {
        val vmArgs = alloc<JavaVMInitArgs>()
        val options = allocArray<JavaVMOption>(8)
        var optIdx = 0

        val classpath = buildClasspath("addons/java_gdext/bin/lib")
        options[optIdx++].optionString = "-Djava.class.path=$classpath".cstr.getPointer(this)
        options[optIdx++].optionString = "--enable-native-access=ALL-UNNAMED".cstr.getPointer(this)
        options[optIdx++].optionString = "-Xms128m".cstr.getPointer(this)
        options[optIdx++].optionString = "-Xmx1024m".cstr.getPointer(this)
        options[optIdx++].optionString = "-XX:+UseG1GC".cstr.getPointer(this)
        options[optIdx++].optionString = "-Djava.library.path=lib${if (isWindows) ";" else ":"}.".cstr.getPointer(this)

        vmArgs.version = JNI_VERSION_1_8
        vmArgs.nOptions = optIdx
        vmArgs.options = options
        vmArgs.ignoreUnrecognized = 0.toUByte()

        val jvmPtr = alloc<CPointerVar<JavaVMVar>>()
        val envPtr = alloc<CPointerVar<CPointerVar<JNIEnvVar>>>()

        if (createJvm(jvmPtr, envPtr, vmArgs.ptr) != JNI_OK) {
            logError("Failed to create JVM")
            return false
        }

        RuntimeState.jvm = jvmPtr.value
        val env = envPtr.value!!.pointed.value!!
        if (!cacheMetadata(env)) return false
    }

    RuntimeState.jvmStarted = true
    logInfo("JVM created successfully")
    return true
}

fun ensureJavaBridgeInitialized(): Boolean {
    if (RuntimeState.javaInitialized) return true
    val (env, attached) = getJniEnv()
    if (env == null) return false

    return try {
        val envPtr = env.pointed.value!!.pointed
        val procAddr = RuntimeState.getProcAddress.toLong()
        val libPtr = RuntimeState.libraryPtr.toLong()

        envPtr.CallStaticVoidMethodV!!.invoke(
            env,
            RuntimeState.bridgeClass,
            RuntimeState.midInitialize,
            procAddr,
            libPtr,
        )
        val ok = checkException(env, "GodotBridge.initialize")
        if (ok) RuntimeState.javaInitialized = true
        ok
    } finally {
        releaseJniEnv(attached)
    }
}

fun callLevelCallback(isInit: Boolean, level: Short) {
    if (RuntimeState.jvm == null || RuntimeState.bridgeClass == null) return
    val (env, attached) = getJniEnv()
    if (env == null) return

    try {
        val method = if (isInit) RuntimeState.midOnLevelInit else RuntimeState.midOnLevelDeinit
        val envPtr = env.pointed.value!!.pointed
        envPtr.CallStaticVoidMethodV!!(env, RuntimeState.bridgeClass, method, level)
        checkException(env, "GodotBridge level callback")
    } finally {
        releaseJniEnv(attached)
    }
}

fun destroyJvm() {
    val jvm = RuntimeState.jvm ?: return

    val (env, attached) = getJniEnv()
    if (env != null) {
        val envPtr = env.pointed.value!!.pointed
        memScoped {
            envPtr.CallStaticVoidMethodV!!(env, RuntimeState.bridgeClass, RuntimeState.midShutdown, allocArray(0))
        }

        if (RuntimeState.bridgeClass != null) {
            envPtr.DeleteGlobalRef!!(env, RuntimeState.bridgeClass)
            RuntimeState.bridgeClass = null
        }
        releaseJniEnv(attached)
    }

    val _ = jvm.pointed.value!!.pointed.DestroyJavaVM!!(jvm)

    RuntimeState.jvm = null
    RuntimeState.jvmStarted = false
    RuntimeState.javaInitialized = false
    RuntimeState.jvmHandle?.let { closeLibrary(it) }
    logInfo("JVM destroyed")
}
