import com.diffplug.spotless.LineEnding
import java.nio.charset.Charset

plugins {
    id("com.diffplug.spotless")
}

spotless {
    encoding = Charset.forName("UTF-8")
    lineEndings = LineEnding.PRESERVE

    kotlinGradle {
        ktlint("1.8.0")
    }
}

tasks.spotlessCheck {
    dependsOn(tasks.spotlessApply)
}
