val kotlinVersion: String = "1.9.0"
val ktorVersion: String = "2.3.2"
val loggingVersion: String = "1.4.8"

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    application
    idea
}

group = "de.wimbes.leon.dnaforge-backend"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor", "ktor-server-core-jvm", ktorVersion)
    implementation("io.ktor", "ktor-server-netty-jvm", ktorVersion)
    implementation("io.ktor", "ktor-server-websockets-jvm", ktorVersion)
    implementation("io.ktor", "ktor-serialization-kotlinx-json-jvm", ktorVersion)

    implementation("ch.qos.logback", "logback-classic", loggingVersion)


    testImplementation(kotlin("test", kotlinVersion))
    testImplementation("io.ktor", "ktor-server-tests-jvm", ktorVersion)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
