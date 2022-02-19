plugins {
    kotlin("jvm").version("1.6.0")
    id("com.github.johnrengelman.shadow").version("6.0.0")
}

group = "com.github.exerosis.yuumilayer"
version = "1.0.0"

repositories { mavenCentral(); mavenLocal(); maven("https://jitpack.io") }

dependencies {
    implementation("com.github.exerosis:mynt:1.0.9")
    implementation("com.github.kwhat:jnativehook:2.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
}

tasks.shadowJar {
    archiveFileName.set("${project.name}.jar")
    destinationDirectory.set(file("./Application"))
    manifest.attributes["Main-Class"] = "com.github.exerosis.yuumilayer.MainKt"
}

tasks.build { dependsOn(tasks.shadowJar) }

