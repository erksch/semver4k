plugins {
    kotlin("multiplatform") version "1.9.21"
    id("convention.publication")
}

group = "de.voize"
version = "4.3.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.configure {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
        jvmTest.configure {
            dependencies {
                implementation("io.mockk:mockk:1.13.9")
                implementation("junit:junit:4.13.2")
            }
        }
    }
}
