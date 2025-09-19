val kotlinVersion: String = "2.2.20"
val ktorVersion: String = "3.3.1"
val loggingVersion: String = "1.5.19"

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    application
    idea
}

group = "org.dnaforge.dnaforge-backend"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-partial-content:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$loggingVersion")


    testImplementation(kotlin("test", kotlinVersion))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()

    environment("DATADIR", "./data")
    environment("ACCESSTOKEN", "TestToken")
    environment("PORT", 8080)
    environment("CUDA", true)
    environment("LOGLEVEL", "TRACE")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("dnaforge.backend.MainKt")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
