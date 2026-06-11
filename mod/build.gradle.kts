plugins {
    alias(libraries.plugins.loom) apply false
}

// The version catalogs
val libs = the<org.gradle.accessors.dm.LibrariesForLibraries>()

val accessWidener = file("core/src/main/resources/mod.accesswidener")
subprojects {
    // Skip non-mod projects
    if (project.name == "integrations") {
        return@subprojects
    }

    plugins.apply(libs.plugins.loom.get().pluginId)

    repositories {
        maven("https://maven.fabricmc.net/")

        maven("https://api.modrinth.com/maven") {
            content {
                includeGroup("maven.modrinth")
            }
        }
    }

    dependencies {
        annotationProcessor(project(":processor"))

        // Change the Minecraft Version in /gradle/libraries.versions.toml
        configurations["minecraft"]("com.mojang:minecraft:${libs.versions.minecraft.get()}")

        implementation(libs.fabric.loader)
        compileOnly(libs.annotations)
    }

    configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
        accessWidenerPath = accessWidener
        runs.clear()
    }

    tasks.compileJava {
        options.compilerArgs.add("-AmoduleName=" + project.name)
        options.compilerArgs.add("-AprojectId=" + rootProject.name)
        options.compilerArgs.add("-AjavaVersion=" + globalSettings.targetJavaVersion)
    }
}