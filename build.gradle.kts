import plugin.GlobalSettings

plugins {
    id("java")
}

val globalSettings = gradle.extensions.findByType<GlobalSettings>()
    ?: error("plugin.GlobalSettings not initialized. Did you apply com.example.settings in settings.gradle.kts?")

group = rootProject.extra["maven_group"] as String
version = rootProject.extra["mod_version"] as String

// Modules to exclude from the merged jar
val excludedModules = listOf("processor", "runner")

allprojects {
    extensions.add<GlobalSettings>("globalSettings", globalSettings)
    plugins.apply("java-library")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    val targetJavaVersion = globalSettings.targetJavaVersion
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

        if (globalSettings.sources) {
            withSourcesJar()
        }
    }
}

registerMergedJar("mergeJar", "jar")
if (globalSettings.sources) {
    registerMergedJar("mergeSourcesJar", "sourcesJar", "sources", true)
}

tasks.named("build") {
    dependsOn("mergeJar")
    if (globalSettings.sources) {
        dependsOn("mergeSourcesJar")
    }
}

fun registerMergedJar(name: String, jarTaskName: String, classifier: String = "", sources: Boolean = false) {
    tasks.register<Jar>(name) {
        description = "Merges all modules together into one jar and cleans them up"
        archiveClassifier.set(classifier)

        // Make this task depend on remapJar tasks from all relevant subprojects
        val relevantProjects = subprojects.filter { !excludedModules.contains(it.name) }
        dependsOn(relevantProjects.mapNotNull { subproject ->
            subproject.tasks.findByName(jarTaskName)
        })

        // Include compiled classes from the remapJar task output when available
        from(relevantProjects.map { subproject ->
            val sourceSet = subproject.the<SourceSetContainer>()["main"]
            if (sources) {
                sourceSet.allSource
            } else {
                sourceSet.output
            }
        })

        var foundAccessWidener = false
        filesMatching("*.accesswidener") {
            if (foundAccessWidener) {
                throw GradleException("Multiple access wideners found in merged jar! Only one access widener is allowed.")
            }

            foundAccessWidener = true
            path = "${project.name}.accesswidener"
        }

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

            // Update the fabric.mod.json file
            zipTree(tempJar).matching { include("fabric.mod.json") }.singleFile.let { fabricModFile ->
                val fabricModContent = fabricModFile.readText()
                val fabricMod = groovy.json.JsonSlurper().parseText(fabricModContent) as Map<*, *>

                // Remove pre-launch entrypoint
                val fabricModMutable = fabricMod.toMutableMap()
                val entrypoints =
                    (fabricMod["entrypoints"] as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<String, Any>()
                entrypoints.remove("preLaunch")
                fabricModMutable["entrypoints"] = entrypoints

                // Add mixins if any exist
                if (mixinFiles.isNotEmpty()) {
                    fabricModMutable["mixins"] = mixinFiles.toList()
                }

                // Add access widener if it exists
                if (foundAccessWidener) {
                    fabricModMutable["accessWidener"] = "${project.name}.accesswidener"
                } else {
                    fabricModMutable["accessWidener"] = null
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

        // Exlude .keep files
        exclude("**/.keep")
    }
}
