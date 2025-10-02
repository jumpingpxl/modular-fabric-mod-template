// hacky way to get all integration modules
val integrations = ArrayList<String>()
project.parent?.subprojects?.forEach {
    if (it.name.startsWith("integration-")) {
        integrations.add(it.name)
    }
}

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

dependencies {
    // Core, API and Models
    implementation(project(path = ":mod:core", configuration = "namedElements"))

    // All integration modules
    integrations.forEach {
        implementation(project(path = ":mod:integrations:$it", configuration = "namedElements"))
    }

    // Dev Auth
    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")

    // Add additional mods to load in the dev environment here
    modImplementation("maven.modrinth:modmenu:${rootProject.extra["mod_menu_version"]}")
}

loom {
    runs {
        create("run") {
            client()

            name("Client")
            ideConfigGenerated(true)
            runDir("..\\..\\run")

            property("io.byteforge.skybuddy.development", "true")

            // Mixin Debug Properties
            property("mixin.debug", "true")
            property("mixin.debug.export", "true")

            // Dev Auth Properties
            property("devauth.enabled", "true")
            //property("devauth.account", "main")
        }
    }
}

// Add the sponge-mixin java agent for mixin reloading in dev environment
afterEvaluate {
    val runConfig = loom.runs.getByName("run")
    try {
        val mixinJar = configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts
            .find { artifact ->
                val id = artifact.moduleVersion.id
                id.group == "net.fabricmc" && id.name == "sponge-mixin"
            }?.file

        if (mixinJar != null) {
            println("Found sponge-mixin at: ${mixinJar.absolutePath}")
            runConfig.vmArg("-javaagent:\"${mixinJar.absolutePath}\"")
        } else {
            println("Could not find sponge-mixin jar. Mixin reloading will not be available.")
        }
    } catch (e: Exception) {
        println("Error finding sponge-mixin: ${e.message}")
    }
}