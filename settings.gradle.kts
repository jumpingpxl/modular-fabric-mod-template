rootProject.name = "example" // is also used as mod id, so keep it simple

// configure the project here
globalSettings {
    // project
    targetJavaVersion = 25
    sources = false // whether to create a sources jar on build

    // modules
    includeModelsModule = true // whether to add the models module (for shared code between the mod & processor)
    includeAPIModule = true // whether to use the api module as base. If false, the core module will be the base
    integrations = listOf(
        "modmenu",
        // the integrations to load (in the `integrations` folder)
    )
}

pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net")
    }
}

plugins {
    id("modular-mod-plugin")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("modDependencies") {
            from(files("gradle/mod-dependencies.versions.toml"))
        }
        create("libraries") {
            from(files("gradle/libraries.versions.toml"))
        }
    }
}

if (globalSettings.includeModelsModule) {
    include("models")
}

if (globalSettings.includeAPIModule) {
    include("mod:api")
}

include("processor")
include("mod:core")

// don't include runner when in ci
if (System.getenv("CI") != "true") {
    include("mod:runner")
}

globalSettings.integrations.forEach {
    var path = "mod:integrations:${it}"
    include(path)
    project(":${path}").name = "integration-${it}"
}
