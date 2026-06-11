# Modular Fabric Mod Template

A template for building modular Minecraft Fabric mods. <br/>
Featuring automatic mixin configuration generation and isolated modules for robust, optional mod support.

## Benefits

- **Modular Structure:** Separates modules for API & core implementation, mod support and the development environment.
- **Automated Mixin Configuration:** Reduces boilerplate and potential errors due to automatic generation of the Mixin
  configuration.
- **Isolated Modules for Optional Mod Support:** Keep your code clean and eliminate the risk of breaking your mod if a (
  supposedly) optional mod dependency is missing at runtime.

## Using the template

1. Clone the repository.
2. Open the project in your favorite IDE (IntelliJ IDEA is recommended).
3. Replace the value of `rootProject.name` in `/settings.gradle.kts` with your desired mod id.
   > Don't forget to also rename your assets directory in `/mod/core/src/main/resources/assets/<modid>` to match this
   new
   mod id.
4. Update the mod name, description, author, etc. in `/mod/core/src/main/resources/fabric.mod.json`.
5. Reload the Gradle project, done!

## Project Structure

```text
.
├── settings.gradle.kts                     # Root settings (configures modules & mod id)
├── build.gradle.kts                        # Root build script, merges all modules together on build
├── gradle.properties                       # Global properties (Mod version)
├── gradle/                                 
│   ├── libraries.versions.toml             # Version catalog for non mod dependencies (Minecraft & Fabric Loader Version, etc)
│   └── mod-dependencies.versions.toml      # Version catalog for mod dependencies (Fabric API, ModMenu, etc)
├── models/                                 # Shared code/annotations for the mod and processor
├── processor/                              # Annotation processor for automatic mixins.json generation
└── mod/                                    # Parent module for all Minecraft/Fabric code
    ├── api/                                # Abstract layer (no mixins or complex logic)
    ├── core/                               # Core implementation (mixins, assets, required dependencies)
    ├── integrations/                       # Parent module for optional third-party mod support
    └── runner/                             # Dev-only environment runner (excluded from final build)
```

## Configuration

This template is designed to be easy to use. However, due to the nature of a more split source - not everything to
configure is in the same place.

Where to find things (to view and modify):

- mod id -> `/settings.gradle.kts` (`rootProject.name`)
- mod version -> `/gradle.properties`
- assets, fabric.mod.json & access widener -> `/mod/core/src/main/resources`
- minecraft, fabric-loader & fabric-loom version -> `/gradle/libraries.versions.toml`
- fabric-api version & other mod dependencies -> `/gradle/mod-dependencies.versions.toml`
- template for automatically generated `mixins.json` files -> `/processor/src/main/resources/default.mixins.json`
- development run configuration creation, dev auth configuration -> `/mod/runner/build.gradle.kts`

### Adding Dependencies

Depending on where you need to access the dependency you have 4 different options.

1. **Create a new Isolated Module:** If you want to isolate the dependency completely from the other modules, while also
   still having access to Minecraft and the Fabric Loader, you can create a new isolated module as described below and
   declare the dependency in that module's `build.gradle.kts`. To use the dependency within the runner, you also need to
   add it in the runner - more below.
2. **Add it to the API Module:** When you want to use the dependency within the API module, add it to
   `/mod/api/build.gradle.kts`.
3. **Add it to the Core Module:** If you don't want to use the dependency within your API module, add it to
   `/mod/core/build.gradle.kts`.

> **Note:** Dependencies added to the Model, API or Core module are also added to the runner. <br/>
> Dependencies of isolated modules **are not**. These have to be added to the runner manually.

#### To the Runner (Dev Environment)

Simply add any (mod) dependency you want in your development environment to the dependencies block in
`/mod/runner/build.gradle.kts`. <br/>
Dependencies of the Model, API and Core module are already included.

### Adding a New Isolated Module for Optional Mod Support

If you want to add a new isolated module for optional mod support, simply add the desired module name to the value of
`integrations` in `/settings.gradle.kts` and reload Gradle. <br/>
If there is not yet a directory with that name in `/mod/integrations/`, it will automatically be created - along with an
empty build.gradle.kts.

### Disabling the Models and API Module

You can disable the `models` and/or `api` module by updating the value of `includeModelsModule` and/or
`includeAPIModule` in `/settings.gradle.kts` to `false` respectively and reload Gradle.

### Adding Mixin Extra Support

Add the following json object to `/processor/src/main/resources/default.mixins.json`. Depending on what you are
intending to use, adjust the min version.

```
    "mixinextras": {
       "minVersion": "0.5.0"
    }
```

### Warnings

- The access widener should not be renamed from `mod.accesswidener`. During build, it will automatically be renamed to
  `<modid>.accesswidener` (`<modid>` being the value of `rootProject.name` in `/settings.gradle.kts`)
- The `preLaunch` entrypoint `modularmodtemplate.runner.DevEnvironmentMixinApplier::apply` (in `fabric.mod.json`) is
  required to load your mixins when starting the game via the run configuration. But don't worry, the entrypoint is
  removed during build and thus isn't included in the output jar.

## Troubleshoot

- *If* the run configuration broke after updating the Minecraft version or mod id: delete it, reload the project from
  disk and reload Gradle. A new (hopefully working) run configuration should have been created.