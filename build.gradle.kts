plugins {
    id("java")
}

group = "io.byteforge"
version = rootProject.extra["mod_version"] as String

// Modules to exclude from the merged jar
val excludedModules = listOf("processor", "runner")

val targetJavaVersion = 21
allprojects {
    plugins.apply("java-library")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }

    java {
        val javaVersion = JavaVersion.toVersion(targetJavaVersion)
        if (JavaVersion.current() < javaVersion) {
            toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
        }

        withSourcesJar()
    }
}

tasks.register<Jar>("mergedJar") {
    archiveClassifier.set("")

    // Make this task depend on remapJar tasks from all relevant subprojects
    val relevantProjects = subprojects.filter { !excludedModules.contains(it.name) }
    dependsOn(relevantProjects.mapNotNull { subproject ->
        subproject.tasks.findByName("remapJar") ?: subproject.tasks.findByName("jar")
    })

    // Include compiled classes from the remapJar task output when available
    from(relevantProjects.map { subproject ->
        subproject.tasks.findByName("remapJar")?.let { remapTask ->
            zipTree(remapTask.outputs.files.singleFile)
        } ?: subproject.the<SourceSetContainer>()["main"].output
    })


    // Move the fabric.mod.json processing to doLast to ensure all files are already added
    doLast {
        // Create a temp copy of the JAR
        val tempJar = temporaryDir.resolve("${archiveFileName.get()}.temp")
        archiveFile.get().asFile.copyTo(tempJar, overwrite = true)

        // Scan the temp JAR for mixin files
        val mixinFiles = mutableSetOf<String>()
        zipTree(tempJar).matching { include("*.mixins.json") }.visit {
            if (!this.isDirectory) {
                mixinFiles.add(this.relativePath.toString())
                logger.lifecycle("Found mixin config: ${this.relativePath}")
            }
        }

        // Now update the fabric.mod.json file
        zipTree(tempJar).matching { include("fabric.mod.json") }.singleFile.let { fabricModFile ->
            val fabricModContent = fabricModFile.readText()
            val fabricMod = groovy.json.JsonSlurper().parseText(fabricModContent) as Map<*, *>

            // Remove pre-launch entrypoint
            val fabricModMutable = fabricMod.toMutableMap()
            val entrypoints = (fabricMod["entrypoints"] as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<String, Any>()
            entrypoints.remove("preLaunch")
            fabricModMutable["entrypoints"] = entrypoints

            // Add mixins if any exist
            if (mixinFiles.isNotEmpty()) {
                fabricModMutable["mixins"] = mixinFiles.toList()
            }

            val jsonOutput = groovy.json.JsonOutput.toJson(fabricModMutable)
            val prettyJson = groovy.json.JsonOutput.prettyPrint(jsonOutput)

            // Create the updated fabric.mod.json
            val updatedFabricModFile = temporaryDir.resolve("fabric.mod.json")
            updatedFabricModFile.writeText(prettyJson)

            // Create the final JAR with the updated fabric.mod.json
            ant.withGroovyBuilder {
                "jar"(
                    "update" to true,
                    "destfile" to archiveFile.get().asFile.absolutePath,
                    "index" to false
                ) {
                    "fileset"("dir" to temporaryDir) {
                        "include"("name" to "fabric.mod.json")
                    }
                }
            }
        }
    }

    // Exclude mixin configuration index dir, as it is only relevant for development
    exclude("META-INF/mixins/**")
}

tasks.named("build") {
    dependsOn("mergedJar")
}

