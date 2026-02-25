plugins {
    id("buildlogic.common-styles-conventions")
}

spotless {
    kotlin {
        ktlint("1.8.0")
    }
}
