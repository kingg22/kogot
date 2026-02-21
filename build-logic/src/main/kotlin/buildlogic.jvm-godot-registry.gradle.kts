import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.FileInputStream

plugins {
    java
}

val generatedRegistryDir = layout.buildDirectory.dir("generated/sources/godotRegistry/java")
val sourceJavaFiles = layout.buildDirectory.file("/classes/java/main")
val sourceKotlinFiles = layout.buildDirectory.file("/classes/kotlin/main")

data class ClassInfoEntry(val fqcn: String, val className: String, val parent: String)

val taskGenerateRegistry = tasks.register("generateGodotRegistry", Task::class) {
    group = "build"

    val classDirs = mutableListOf<File>()
    // TODO
    val javaClasses = sourceJavaFiles.get().asFile
    if (javaClasses.exists()) {
        classDirs.add(javaClasses)
    }
    val kotlinClasses = sourceKotlinFiles.get().asFile
    if (kotlinClasses.exists()) {
        classDirs.add(kotlinClasses)
    }

    val entries = mutableListOf<ClassInfoEntry>()
    val classVisitor = { file: File ->
        FileInputStream(file).use { stream ->
            val reader = ClassReader(stream)

            data class ClassEntry(
                val name: String,
                val superName: String?,
                val isAbstract: Boolean,
                val isInterface: Boolean,
            ) {
                var hasNoArgCtor: Boolean = false
            }

            var holder: ClassEntry? = null

            reader.accept(
                object : ClassVisitor(Opcodes.ASM9) {
                    override fun visit(
                        version: Int,
                        access: Int,
                        name: String?,
                        signature: String?,
                        superName: String?,
                        interfaces: Array<out String?>?,
                    ) {
                        holder = ClassEntry(
                            name!!,
                            superName,
                            (access and Opcodes.ACC_ABSTRACT) != 0,
                            (access and Opcodes.ACC_INTERFACE) != 0,
                        )
                    }

                    override fun visitMethod(
                        access: Int,
                        name: String?,
                        descriptor: String?,
                        signature: String?,
                        exceptions: Array<out String?>?,
                    ): MethodVisitor {
                        if (name == "<init>" && descriptor == "()V" && holder != null) {
                            holder.hasNoArgCtor = (access and Opcodes.ACC_PUBLIC) != 0
                        }
                        return super.visitMethod(access, name, descriptor, signature, exceptions)
                    }
                },
                0,
            )

            if (holder == null) return@use

            if (holder.isInterface || holder.isAbstract) {
                return@use
            }
            if (!holder.hasNoArgCtor) {
                return@use
            }
            if (holder.superName == "io/github/kingg22/godot/api/GodotNode") {
                val name = holder.name
                val fqcn = name.replace('/', '.')
                val simpleName = name.substring(name.lastIndexOf('/') + 1)
                entries.add(ClassInfoEntry(fqcn = fqcn, className = simpleName, parent = "Node"))
            }
        }
    }

    classDirs.onEach { dir ->
        fileTree(dir).matching { include("**/*.class") }.onEach { file ->
            if (!file.name.contains('$')) {
                classVisitor(file)
            }
        }
    }

    val outDir = generatedRegistryDir.get().asFile
    outDir.mkdirs()
    val outFile = File(outDir, "io/github/kingg22/godot/internal/registry/GeneratedRegistry.java")
    outFile.parentFile.mkdirs()
    val lines = buildList {
        add("package io.github.kingg22.godot.internal.registry;")
        add("")
        add("import io.github.kingg22.godot.api.GodotRegistry;")
        add("")
        add("/** Generated registry. Do not edit manually. */")
        add("public final class GeneratedRegistry {")
        add("    private GeneratedRegistry() {")
        add("        throw new UnsupportedOperationException(\"Utility class\");")
        add("    }")
        add("")
        add("    public static void registerAll(final GodotRegistry registry) {")
        entries.onEach { entry ->
            add(
                "        registry.register(\"${entry.className}\", \"${entry.parent}\", ${entry.fqcn}::new);",
            )
        }
        add("    }")
        add("}")
        add("")
    }
    outFile.writeText(lines.joinToString(System.lineSeparator()))
}

val sourceSets = the<SourceSetContainer>()

val taskCompileGeneratedRegistry = tasks.register("compileGeneratedRegistry", JavaCompile::class) {
    group = "build"
    dependsOn(taskGenerateRegistry)

    tasks.findByName("compileKotlin")?.let { this.dependsOn(it) }
    tasks.findByName("compileJava")?.let { this.dependsOn(it) }

    source(generatedRegistryDir)
    val mainSourceSet = sourceSets.named("main").get()
    val compileJava = tasks.withType<JavaCompile>().findByName("compileJava")
    val classesDirs = files(
        compileJava?.destinationDirectory,
        layout.buildDirectory.dir("classes/kotlin/main"),
    )
    classpath = mainSourceSet.compileClasspath + classesDirs
    destinationDirectory = layout.buildDirectory.dir("classes/java/main")
}

tasks.assemble.configure {
    dependsOn(taskGenerateRegistry)
}

tasks.classes.configure {
    dependsOn(taskCompileGeneratedRegistry)
}
