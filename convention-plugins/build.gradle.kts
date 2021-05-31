plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.4.32")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}
