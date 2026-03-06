import net.ltgt.gradle.errorprone.errorprone

plugins {
    // Apply the java conventions plugin from build-logic.
    id("buildlogic.java-library-conventions")
    id("buildlogic.java-null-check")
    id("buildlogic.java-styles-conventions")
    id("buildlogic.jextract")
}

jextract {
    packageName = "io.github.kingg22.godot.internal.ffm"
    headerClassName = "FFMUtils"
    argsFile = layout.projectDirectory.file("scripts/v4_6_1/godot_includes.txt")
    headerFile(rootProject.layout.projectDirectory.file("godot-version/v4_6_1/gdextension_interface.h"))
}

tasks.withType<JavaCompile> {
    options.errorprone {
        disable("NotJavadoc", "StringConcatToTextBlock")
    }
}

tasks.jextract.configure {
    // 🗑️ Eliminar archivos específicos tras generar
    doLast {
        val targetPackageAsPath = packageName.get().replace(".", "/")
        val headerClassNameString = headerClassName.get()
        val outputDirFile = outputDir.get().asFile
        val packageDir = outputDirFile.resolve(targetPackageAsPath)
        val file1 = packageDir.resolve("$headerClassNameString.java")
        val file2 = packageDir.resolve($$"$${headerClassNameString}$shared.java")
        require(file1.exists() && file2.exists()) { "Generated files not found: $file1, $file2" }

        file1.delete()
        file2.delete()
    }
}

tasks.spotlessJava {
    dependsOn(tasks.jextract)
}
