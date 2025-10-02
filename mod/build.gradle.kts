plugins {
    id("fabric-loom") version "1.11-SNAPSHOT" apply false
}

val accessWidener = file("core/src/main/resources/skybuddy.accesswidener")
subprojects {
    // Skip non-mod projects
    if (project.name == "integrations") {
        return@subprojects
    }

    val javaVersion = System.getProperty("java.version")
    println("Configuring mod subproject: ${project.name} with Java version $javaVersion")
    apply(plugin = "fabric-loom")

    val loom = the<net.fabricmc.loom.api.LoomGradleExtensionAPI>()

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

        val minecraft by configurations
        val mappings by configurations
        val modImplementation by configurations

        // To change the versions see the gradle.properties file
        minecraft("com.mojang:minecraft:${rootProject.extra["minecraft_version"]}")
        mappings(loom.officialMojangMappings())
        modImplementation("net.fabricmc:fabric-loader:${rootProject.extra["loader_version"]}")

        modImplementation("net.fabricmc.fabric-api:fabric-api:${rootProject.extra["fabric_api_version"]}")
    }

    configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
        accessWidenerPath = accessWidener

        runs {
            clear()
        }

        mixin {
            defaultRefmapName = "skybuddy-${project.name}.refmap.json"
        }
    }

    tasks.compileJava {
        options.compilerArgs.add("-AmoduleName=" + project.name)
    }
}