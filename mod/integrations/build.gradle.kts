subprojects {
    dependencies {
        api(project(path = ":mod:core", configuration = "namedElements"))
    }
}