plugins { kotlin("jvm").version("1.6.20-M1") }

group = "com.github.exerosis.yuumilayer"
version = "1.0.0"

repositories { mavenCentral(); mavenLocal(); maven("https://jitpack.io") }

dependencies {
    implementation("com.github.exerosis:mynt:1.0.9")
    implementation("com.github.kwhat:jnativehook:2.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
}