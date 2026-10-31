import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm") version "2.3.10"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "space.hugster"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(kotlin("stdlib"))

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    compileOnly("com.cjcrafter:foliascheduler:0.7.0")
    compileOnly("me.clip:placeholderapi:2.11.6")

    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    compileOnly("it.unimi.dsi:fastutil:8.5.15")

    compileOnly("io.github.revxrsal:lamp.common:4.0.0")
    compileOnly("io.github.revxrsal:lamp.bukkit:4.0.0")

    compileOnly("org.jetbrains.exposed:exposed-core:1.6.0")
    compileOnly("org.jetbrains.exposed:exposed-dao:1.6.0")
    compileOnly("org.jetbrains.exposed:exposed-jdbc:1.6.0")

    compileOnly("com.zaxxer:HikariCP:6.2.1")
    compileOnly("org.xerial:sqlite-jdbc:3.49.1.0")
    compileOnly("com.mysql:mysql-connector-j:9.2.0")
    compileOnly("org.mongodb:mongodb-driver-sync:5.3.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        javaParameters = true
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
