plugins {
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.kotlin-styles-conventions")
}

kotlin {
    compilerOptions {
        explicitApi()
    }
}

val codegenProject = evaluationDependsOn(":godot-java-codegen")
val generateApi = codegenProject.tasks.named("generateGodotExtensionApi")

sourceSets {
    main {
        kotlin.srcDir(generateApi.map { it.outputs.files })
    }
}

tasks.compileKotlin {
    dependsOn(generateApi)
}

tasks.spotlessKotlin {
    dependsOn(generateApi)
}
