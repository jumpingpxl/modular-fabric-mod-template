dependencies {
    if (globalSettings.includeAPIModule) {
        api(project(":mod:api"))
    } else if (globalSettings.includeModelsModule) {
        api(project(":models"))
    }

    // Additional dependencies
    // api(modDependencies.fabric.api)
}

// get valid supported minecraft versions now to throw an error during gradle reload if something ain't right
val supportedMinecraftVersions = getValidSupportedMinecraftVersions()

tasks.processResources {
    filteringCharset = "UTF-8"
    val replacements: Map<String, Any> = mapOf(
        "version" to project.version,
        "mod_id" to rootProject.name,
        "fabric_loader_version" to libraries.versions.fabric.loader.get(),
        "minecraft_version" to supportedMinecraftVersions,
        "java_version" to globalSettings.targetJavaVersion,
    )

    replacements.forEach {
        inputs.property(it.key, it.value)
    }

    filesMatching("fabric.mod.json") {
        println("Replacing fabric.mod.json placeholders with:")
        replacements.forEach { (key, value) -> println("  $key -> $value") }

        expand(replacements)
    }

    val licenseFile = rootProject.file("LICENSE")
    if (licenseFile.exists()) {
        from(licenseFile) {
            into("META-INF")
        }
    }
}

// add fabric loader as buildscript dependency to validate compatible minecraft versions
buildscript {
    repositories {
        maven("https://maven.fabricmc.net/")
    }

    dependencies {
        classpath(libraries.fabric.loader)
    }
}

fun getValidSupportedMinecraftVersions(): String {
    val usedVersion = libraries.versions.minecraft.get()
    val compatibleMinecraftVersions = globalSettings.compatibleMinecraftVersions

    // if compatible minecraft versions are not set, return the exact minecraft version that is used
    if (compatibleMinecraftVersions.isNullOrEmpty()) {
        return usedVersion
    }

    val predicate = net.fabricmc.loader.api.metadata.version.VersionPredicate.parse(compatibleMinecraftVersions)
    val version = net.fabricmc.loader.api.Version.parse(usedVersion)

    // test if the used minecraft version is compatible with the set compatible minecraft versions
    if (!predicate.test(version)) {
        // if now throw an error with solutions
        error(
            """
            The used Minecraft version '$usedVersion' is not compatible with the set compatible Minecraft versions '$compatibleMinecraftVersions'.
            ${"\u200B"}
            * Possible Solutions:
              > Check if the syntax of '$compatibleMinecraftVersions' is correct. 
              > Update the compatible Minecraft versions to include '$usedVersion' in: 
                    ${relativeFileURL("settings.gradle.kts")}
              > Update the used Minecraft version to fix the compatible Minecraft versions in:
                    ${relativeFileURL("gradle/libraries.versions.toml")}
            ${"\u200B"}
            * Resources:
              > Take a look at https://docs.fabricmc.net/develop/loader/fabric-mod-json#semantic-versioning for more information.
              > Validate the new compatible Minecraft versions with https://dexman545.github.io/outlet-database/floaderValidator
        """.trimIndent()
        )
    }

    return compatibleMinecraftVersions
}

fun relativeFileURL(path: String): String {
    // try to get the path as absolute file url, if it fails, return the path as is
    return runCatching {
        rootProject.file(path).toURI().toString()
    }.getOrDefault(path)
}