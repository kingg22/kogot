import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.library.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    `java-test-fixtures`
}

val testDataDir: Directory = layout.projectDirectory.dir("testData")
val testGenDir: Provider<Directory> = layout.buildDirectory.dir("test-gen")

// Runtime jars the internal compiler test framework locates through `org.jetbrains.kotlin.test.*` properties.
val testArtifacts: Configuration = configurations.create("testArtifacts")

dependencies {
    // Provided at runtime by whichever compiler loads the plugin (kotlinc, Gradle's embeddable compiler, the IDE).
    compileOnly(libs.kotlin.compiler.plugin.api)

    // Tests run the real compiler through Kotlin's internal test framework (as the official plugin template does).
    testFixturesApi(libs.kotlin.compiler)
    testFixturesApi(libs.kotlin.compiler.test.framework)
    testFixturesApi(libs.kotlin.test.junit5)
    testFixturesRuntimeOnly(libs.junit)

    testArtifacts(libs.kotlin.stdlib)
    testArtifacts(libs.kotlin.stdlib.jdk8)
    testArtifacts(libs.kotlin.reflect)
    testArtifacts(libs.kotlin.test)
    testArtifacts(libs.kotlin.script.runtime)
    testArtifacts(libs.kotlin.annotations.jvm)
}

// The plugin runs inside the compiler's JVM, which can be older than the project toolchain
// (e.g. the IDE's JBR 21 when loaded through Kotlin External FIR Support). Keep the bytecode on 17.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

val generateTests = tasks.register<JavaExec>("generateTests") {
    description = "Generates JUnit 5 test classes from the files in testData/."
    inputs.dir(testDataDir).withPropertyName("testData").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(testGenDir).withPropertyName("generatedTests")

    classpath = sourceSets.testFixtures.get().runtimeClasspath
    mainClass.set("io.github.kingg22.kogot.compiler.test.GenerateTestsKt")
    workingDir = rootDir
    args(testGenDir.get().asFile.absolutePath, testDataDir.asFile.absolutePath)
}

sourceSets {
    test {
        // JUnit 5 test classes (Java) generated from testData; passing the task wires the dependency for
        // every compile task of the source set.
        java.srcDir(generateTests)
    }
}

tasks.test {
    dependsOn(testArtifacts)
    // Test data paths in the generated classes are relative to the root project.
    workingDir = rootDir

    fun libraryProperty(property: String, jarName: String) {
        val jar = testArtifacts.files.firstOrNull { Regex("""$jarName-\d.*\.jar""").matches(it.name) }
        if (jar != null) systemProperty(property, jar.absolutePath)
    }
    libraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin-stdlib")
    libraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin-stdlib-jdk8")
    libraryProperty("org.jetbrains.kotlin.test.kotlin-reflect", "kotlin-reflect")
    libraryProperty("org.jetbrains.kotlin.test.kotlin-test", "kotlin-test")
    libraryProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin-script-runtime")
    libraryProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", "kotlin-annotations-jvm")

    // `-Pkotlin.test.update.test.data=true` rewrites testData expectations from the actual compiler output.
    providers.gradleProperty("kotlin.test.update.test.data").orNull?.let {
        systemProperty("kotlin.test.update.test.data", it)
    }

    // Shared Godot API stand-in added to every diagnostics test, and the real converters it must stay in sync with.
    val apiStubDir = layout.projectDirectory.dir("testData/api")
    inputs.dir(apiStubDir).withPropertyName("godotApiStub").withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("kogot.test.godotApiStubDir", apiStubDir.asFile.absolutePath)
    val runtimeBuiltins = "kotlin-native/api/generated/src/nativeMain/kotlin/io/github/kingg22/godot/api/builtin"
    mapOf(
        "kogot.test.convertToVariant" to "$runtimeBuiltins/internal/ConvertToVariant.kt",
        "kogot.test.variantConverter" to "$runtimeBuiltins/VariantConverter.kt",
    ).forEach { (property, path) ->
        val file = rootProject.layout.projectDirectory.file(path)
        inputs.file(file).withPropertyName(property).withPathSensitivity(PathSensitivity.RELATIVE)
        systemProperty(property, file.asFile.absolutePath)
    }

    systemProperty("idea.ignore.disabled.plugins", "true")
    systemProperty("idea.home.path", rootDir)
}
