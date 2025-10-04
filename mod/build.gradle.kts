plugins {
    alias(libraries.plugins.loom) apply false
}

// The version catalogs
val libs = the<org.gradle.accessors.dm.LibrariesForLibraries>()
val mods = the<org.gradle.accessors.dm.LibrariesForModDependencies>()

val accessWidener = file("core/src/main/resources/mod.accesswidener")
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
        minecraft("com.mojang:minecraft:${libs.versions.minecraft.get()}")
        mappings(loom.officialMojangMappings())
        modImplementation(libs.fabric.loader)
        modImplementation(mods.fabric.api)
    }

    configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
        accessWidenerPath = accessWidener

        runs {
            clear()
        }

        mixin {
            defaultRefmapName = "${rootProject.name}-${project.name}.refmap.json"
        }
    }

    tasks.compileJava {
        options.compilerArgs.add("-AmoduleName=" + project.name)
        options.compilerArgs.add("-AprojectId=" + rootProject.name)
    }
}