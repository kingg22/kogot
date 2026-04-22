import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    id("buildlogic.kotlin-multiplatform-conventions")
    id("buildlogic.kotlin-styles-conventions")
}

val isRelease = hasProperty("releaseMode") || hasProperty("release")
val isCi = System.getenv("CI") != null

val listOfNativeBuildType = if (isRelease) {
    listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)
} else if (isCi) {
    listOf()
} else {
    listOf(NativeBuildType.DEBUG)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.experimental.ExperimentalNativeApi",
        )
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.kotlinNative.ffi)
    }

    applyDefaultHierarchyTemplate()

    linuxX64 {
        applyBinariesExport()
        configureJniImport("linux")
    }
    mingwX64 {
        configureJniImport("win32")
        applyBinariesExport()
    }
}

fun resolveJavaHome(): Provider<File> = providers.systemProperty("java.home")
    .map { File(it) }
    .map {
        if (it.name == "jre") {
            it.parentFile
        } else {
            it
        }
    }

fun KotlinNativeTarget.configureJniImport(currentPlatform: String) {
    check(currentPlatform == "linux" || currentPlatform == "darwin" || currentPlatform == "win32") {
        "Unsupported platform: $currentPlatform"
    }
    val javaHomeProperty = resolveJavaHome()

    compilations.getByName("main").cinterops.register("jni") {
        packageName = "io.github.kingg22.godot.internal.register.jni.ffi"
        val javaHomeStr = javaHomeProperty.get()
        println("JAVA_HOME: '$javaHomeStr'")
        val jniCurrentOsDir = javaHomeStr.resolve("include").resolve(currentPlatform)
        if (jniCurrentOsDir.exists()) {
            defFile(project.file("nativeInterop/cinterop/jni.def"))
            includeDirs.allHeaders(
                javaHomeStr.resolve("include").toString(),
                jniCurrentOsDir.toString(),
            )
        } else {
            logger.warn("JNI current platform is not supported, because doesn't exist the dir: '$jniCurrentOsDir'")
        }
    }
}

fun KotlinNativeTarget.applyBinariesExport(baseName: String = "godot-java-ffm") {
    binaries {
        sharedLib(buildTypes = listOfNativeBuildType) {
            this.baseName = baseName
        }
    }
}
