plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.10")
    implementation("io.github.gradle-nexus:publish-plugin:1.3.0")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}
