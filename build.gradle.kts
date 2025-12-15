import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.0-RC2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.tivin"
version = "1.21-2.1.3"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.8-R0.1-SNAPSHOT")

    // THIS is what provides kotlin/jvm/internal/Intrinsics
    implementation(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    // Make shadowJar replace the normal jar
    archiveClassifier.set("")

    // DO NOT minimize Kotlin
    minimize {
        exclude(dependency("org.jetbrains.kotlin:.*"))
    }
}

// Make `build` produce the shaded jar
tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
