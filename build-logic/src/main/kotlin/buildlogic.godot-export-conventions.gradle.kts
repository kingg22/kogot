plugins {
    java
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
