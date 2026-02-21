plugins {
    id("buildlogic.jvm-styles-conventions")
}

spotless {
    java {
        removeUnusedImports()
        palantirJavaFormat("2.86.0").formatJavadoc(true)
        importOrder("", "java", "javax", "\\#")
        formatAnnotations()
    }
}
