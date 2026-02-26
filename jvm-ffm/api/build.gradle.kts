plugins {
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.kotlin-styles-conventions")
}

kotlin {
    compilerOptions {
        explicitApi()
    }
}

val codegenProject = evaluationDependsOn(":" + projects.codegen.name)
val generateApi = codegenProject.tasks.named<GenerateGodotTask>("generateGodotExtensionApi")

generateApi.configure {
    outputDir.set(layout.buildDirectory.dir("generated/sources/godotApi"))
}

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
