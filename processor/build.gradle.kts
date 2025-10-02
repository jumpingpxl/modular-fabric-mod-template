dependencies {
    implementation(project(":models"))

    // gson
    implementation(libraries.gson)

    // mixin (for mixin annotation processor)
    implementation("net.fabricmc:sponge-mixin:0.15.4+mixin.0.8.7")

    // auto-service
    implementation(libraries.autoservice.annotations)
    annotationProcessor(libraries.autoservice)
}