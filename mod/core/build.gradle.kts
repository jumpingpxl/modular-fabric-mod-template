dependencies {
    if (globalSettings.includeAPIModule) {
        api(project(path = ":mod:api"))
    } else if (globalSettings.includeModelsModule) {
        api(project(":models"))
    }

    // Additional dependencies
    // implementation(modDependencies.fabric.api)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val replacements: Map<String, Any> = mapOf(
        "version" to project.version,
        "mod_id" to rootProject.name,
        "fabric_loader_version" to libraries.versions.fabric.loader.get(),
        "minecraft_version" to libraries.versions.minecraft.get(),
        "java_version" to globalSettings.targetJavaVersion,
    )

    replacements.forEach {
        inputs.property(it.key, it.value)
    }

    filesMatching("fabric.mod.json") {
        expand(replacements)
    }
}