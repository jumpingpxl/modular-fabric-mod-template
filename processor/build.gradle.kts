dependencies {
    implementation(project(":models"))

    // gson
    implementation("com.google.code.gson:gson:2.10.1")

    // mixin (for mixin annotation processor)
    implementation("net.fabricmc:sponge-mixin:0.15.4+mixin.0.8.7")

    // auto-service
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
}