plugins {
    id("buildlogic.java-library-conventions")
    id("buildlogic.java-styles-conventions")
    id("buildlogic.java-null-check")
    id("buildlogic.jvm-godot-registry")
}

dependencies {
    implementation(projects.godotJavaBinding)
}

val taskDeps = tasks.register("copyRuntimeDepsToLib", Copy::class) {
    group = "build"
    from(configurations.runtimeClasspath)
    into(file("$rootDir/lib"))
}

val taskJar = tasks.register("copyRuntimeJarToLib", Copy::class) {
    group = "build"
    dependsOn(tasks.jar)
    from(tasks.jar)
    into(file("$rootDir/lib"))
}

tasks.assemble.configure {
    dependsOn(taskDeps, taskJar)
}
