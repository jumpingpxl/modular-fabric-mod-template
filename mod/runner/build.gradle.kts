repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

dependencies {
    // Core, API and Models
    implementation(project(":mod:core"))

    // All integration modules
    globalSettings.integrations.forEach {
        implementation(project(":mod:integrations:integration-$it")) {
            isTransitive = false // do not load dependencies of integrations
        }
    }

    // Dev Auth
    runtimeOnly(modDependencies.devauth)

    // Add additional mods to load in the dev environment here
    implementation(modDependencies.modmenu)
}

loom {
    runs {
        create("run") {
            client()

            displayName = "Client"
            generateRunConfig = true
            runDirectory = file("..\\..\\run")

            // Mixin Debug Properties
            systemProperties.put("mixin.debug", "true")
            systemProperties.put("mixin.debug.export", "true")

            // Dev Auth Properties
            systemProperties.put("devauth.enabled", "true")
            //systemProperties.put("devauth.account", "main")
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