plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "net.azisaba"
version = "1.2.1"

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.json:json:20220320")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
}

tasks {
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    shadowJar {
        manifest.attributes("Main-Class" to "net.azisaba.privateChatParser.Main")
        archiveFileName.set("PrivateChatParser-${project.version}.jar")
    }
}
