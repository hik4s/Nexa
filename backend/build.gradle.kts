plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    application
}

group = "com.nexa.finance"
version = "0.1.0"

application {
    mainClass.set("com.nexa.finance.ApplicationKt")
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.6.0"
val serializationVersion = "1.11.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()
}
