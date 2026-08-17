plugins {
    java
}

allprojects {
    group = "io.exchangelite"
    version = "0.1.0"
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.3")
    }
}

project(":engine") {
    dependencies {
        "implementation"(project(":common"))
    }
}

project(":sidecar") {
    dependencies {
        "implementation"(project(":common"))
    }
}

project(":cli") {
    dependencies {
        "implementation"(project(":common"))
    }
}
