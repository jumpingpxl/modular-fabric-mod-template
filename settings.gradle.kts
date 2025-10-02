rootProject.name = "skybuddy"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("modDependencies") {
            from(files("/gradle/mod-dependencies.versions.toml"))
        }
        create("libraries") {
            from(files("/gradle/libraries.versions.toml"))
        }
    }
}

include("models")
include("processor")

include("mod:api")
include("mod:core")
include("mod:runner") //todo don't include in pipelines

file("mod/integrations").listFiles()?.forEach {
    if (it.isDirectory && !it.name.startsWith(".") && !it.name.equals("build")) {
        include("mod:integrations:${it.name}")
        project(":mod:integrations:${it.name}").name = "integration-${it.name}"
    }
}
