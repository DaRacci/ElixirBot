plugins {
    application
    id("dev.racci.minix.kotlin")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

repositories {
    mavenCentral()

    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

dependencies {

    implementation(platform(kotlin("bom")))
    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.kotlinx)

    implementation(eLib.kord.extensions)
    implementation(eLib.kord.phishing)

    // Logging dependencies
    implementation(eLib.groovy)
    implementation(eLib.logback)
    implementation(eLib.logging)

    // Tags
//    implementation(libs.kotlinx.serialization.json)
//    implementation(libs.kotlinx.serialization.kaml)

    implementation(eLib.toml)
    implementation(eLib.githubAPI)
    implementation(eLib.kmongo)
    implementation(libs.bundles.exposed)
}

application {
    // This is deprecated, but the Shadow plugin requires it
    mainClassName = "dev.racci.elixir.ElixirBotKt"
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "dev.racci.elixir.ElixirBotKt"
        )
    }
}
