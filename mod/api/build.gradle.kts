dependencies {
    if (globalSettings.includeModelsModule) {
        api(project(":models"))
    }

    // Additional dependencies
    api(modDependencies.fabric.api)
}