plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jda) {
        // Audio encoding and encryption, the bot never joins voice channels
        exclude(module = "opus-java")
        exclude(module = "tink")
    }
    implementation(libs.gson)
    // logging
    implementation(libs.slf4j.api)
    implementation(libs.log4j.core)
    runtimeOnly(libs.log4j.slf4j.impl)
    // testing
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass = "eu.andret.torphes.Torphes"
}

tasks {
    compileJava {
        options.release = 21
    }

    test {
        useJUnitPlatform()
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.WARN

        from({
            configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
        })

        manifest {
            attributes["Main-Class"] = application.mainClass
        }
    }
}
