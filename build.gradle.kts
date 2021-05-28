plugins {
    kotlin("multiplatform") version "1.5.10"
    id("maven-publish")
}

group = "com.vdurmont"
version = "4.0.0"

repositories {
    mavenCentral()
}

kotlin {
    ios()
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.mockk:mockk:1.11.0")
                implementation("junit:junit:4.12")
            }
        }
    }
}
