enableFeaturePreview("VERSION_CATALOGS")
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.racci.dev/releases")
        // TODO: Fix the standing issue in Minix Conventions so we don't need to add this.
        maven("https://papermc.io/repo/repository/maven-public/")
    }

    plugins {
        val kotlinVersion: String by settings
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jetbrains.dokka") version kotlinVersion
        id("com.github.johnrengelman.shadow") version "7.1.2"
    }
    val minixConventions: String by settings
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("dev.racci.minix")) {
                useVersion(minixConventions)
            }
        }
    }
}

rootProject.name = "ElixirBot"

dependencyResolutionManagement {
    repositories {
        maven("https://repo.racci.dev/releases")
    }

    versionCatalogs {
        create("libs") {
            val minixConventions: String by settings
            from("dev.racci:catalog:$minixConventions")
        }
        create("eLib") {
            from(files("libs.versions.toml"))
        }
    }
}
