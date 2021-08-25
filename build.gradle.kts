plugins {
    kotlin("multiplatform") version "1.5.10"
    id("convention.publication")
}

group = "de.voize"
version = "4.1.0"

repositories {
    mavenCentral()
}

kotlin {
    ios()
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

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
