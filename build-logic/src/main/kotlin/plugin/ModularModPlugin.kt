package plugin

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.create
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class GlobalSettings {
    // project
    var targetJavaVersion: Int by property()
    var sources: Boolean by property()

    // modules
    var includeModelsModule: Boolean by property()
    var includeAPIModule: Boolean by property()
    var integrations: List<String> by property()
}

class ModularModPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        val config = settings.gradle.extensions.create<GlobalSettings>("globalSettings")
        settings.extensions.add<GlobalSettings>("globalSettings", config)

        // evaluate if fields were assigned a value
        settings.gradle.settingsEvaluated {
            config.targetJavaVersion
            config.sources
            config.includeModelsModule
            config.includeAPIModule
            config.integrations
        }
    }
}

private fun <T> property() = object : ReadWriteProperty<GlobalSettings, T> {
    private var value: T? = null

    override fun getValue(thisRef: GlobalSettings, property: KProperty<*>): T {
        return value ?: error("Mandatory configuration field '${property.name}' was not set in settings.gradle.kts!")
    }

    override fun setValue(thisRef: GlobalSettings, property: KProperty<*>, value: T) {
        this.value = value
    }
}