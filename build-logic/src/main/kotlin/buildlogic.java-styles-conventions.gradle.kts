plugins {
    id("buildlogic.common-styles-conventions")
}

spotless {
    java {
        removeUnusedImports()
        palantirJavaFormat("2.89.0").formatJavadoc(false)
        importOrder("", "java", "javax", "\\#")
        formatAnnotations()
    }
}
