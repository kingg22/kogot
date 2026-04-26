plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(gradleApi())
    implementation(libs.kotlin.gradle.plugin)
    api(projects.codegen.common)
    implementation(projects.codegen.cli)
}

gradlePlugin {
    plugins {
        create("godotCodegenConventionsPlugin") {
            id = "buildlogic.godot-codegen"
            implementationClass = "io.github.kingg22.buildlogic.godot.GodotCodegenConventionsPlugin"
        }
        create("godotCodegenChorePlugin") {
            id = "buildlogic.godot-codegen-chore"
            implementationClass = "io.github.kingg22.buildlogic.godot.chore.GodotCodegenChorePlugin"
        }
    }
}
