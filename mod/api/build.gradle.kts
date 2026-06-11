dependencies {
    if (globalSettings.includeModelsModule) {
        api(project(":models"))
    }

    // Additional dependencies
    implementation(modDependencies.fabric.api)
}