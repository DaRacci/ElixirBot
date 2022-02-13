plugins {
    application
    id("dev.racci.minix.kotlin")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

version = "1.0"

repositories {
    mavenCentral()

    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

dependencies {

    implementation(eLib.kord.extensions)
    implementation(eLib.kord.phishing)
//    implementation(libs.kotlin.stdlib)

    // Logging dependencies
    implementation(eLib.groovy)
    implementation(eLib.logback)
    implementation(eLib.logging)

    // Tags
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.kaml)

    @Suppress("GradlePackageUpdate")
    implementation("com.github.jezza:toml:1.2")

    // Github API
    implementation("org.kohsuke:github-api:1.301")

    // Exposed
    implementation(libs.bundles.exposed)

    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
}

application {
    // This is deprecated, but the Shadow plugin requires it
    mainClassName = "dev.racci.elixir.ElixirBotKt"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "dev.racci.elixir.ElixirBotKt"
        )
    }
}
