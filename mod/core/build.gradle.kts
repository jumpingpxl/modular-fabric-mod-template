dependencies {
    api(project(path = ":mod:api", configuration = "namedElements"))
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("fabric_loader_version", rootProject.extra["loader_version"])
    inputs.property("minecraft_version", rootProject.extra["minecraft_version"])
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "fabric_loader_version" to rootProject.extra["loader_version"],
            "minecraft_version" to rootProject.extra["minecraft_version"],
        )
    }
}