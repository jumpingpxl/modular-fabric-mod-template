plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("modularModPlugin") {
            id = "modular-mod-plugin"
            implementationClass = "plugin.ModularModPlugin"
        }
    }
}