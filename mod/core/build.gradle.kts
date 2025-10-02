dependencies {
    api(project(path = ":mod:api", configuration = "namedElements"))
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mod_id", rootProject.name)
    inputs.property("fabric_loader_version", libraries.fabric.loader.get().version)
    inputs.property("minecraft_version", libraries.versions.minecraft.get())
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "mod_id" to rootProject.name,
            "fabric_loader_version" to libraries.fabric.loader.get().version,
            "minecraft_version" to libraries.versions.minecraft.get(),
        )
    }
}