plugins {
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.kotlin-styles-conventions")
}

sourceSets {
    main {
        kotlin.srcDir("build/generated/sources/godotApi")
    }
}

tasks.compileKotlin {
    dependsOn(":godot-java-codegen:generateGodotExtensionApi")
}
