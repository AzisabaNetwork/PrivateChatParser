plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "net.azisaba"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.json:json:20220320")
}

tasks {
    shadowJar {
        manifest.attributes("Main-Class" to "net.azisaba.privateChatParser.Main")
        archiveFileName.set("PrivateChatParser-${project.version}.jar")
    }
}
