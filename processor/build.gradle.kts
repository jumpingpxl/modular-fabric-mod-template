dependencies {
    if (globalSettings.includeModelsModule) {
        implementation(project(":models"))
    }

    // gson
    implementation("com.google.code.gson:gson:2.10.1")

    // auto-service
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
}