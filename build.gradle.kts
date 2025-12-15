plugins {
    kotlin("jvm") version "2.1.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.tivin"
version = "1.21-2.1.2"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    compileOnly("org.spigotmc:spigot-api:1.21.7-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks {
    shadowJar {
        archiveFileName.set("SpawnerGUI.jar")
        minimize() // optional: removes unused Kotlin classes
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // Force Java 17
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
