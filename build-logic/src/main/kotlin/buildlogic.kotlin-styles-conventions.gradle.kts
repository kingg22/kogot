plugins {
    id("buildlogic.jvm-styles-conventions")
}

spotless {
    kotlin {
        ktlint("1.8.0")
    }
}
