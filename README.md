# Modular Fabric Mod Template

A template for a modular Minecraft Fabric mod with automatic mixin configuration generation and isolated optional mod
support.

## Benefits

- Modular structure \- separate modules for API, core implementation, mod integrations, and development environment
- Automatic mixin configuration generation \- reduces boilerplate and potential errors
- Isolated modules for optional mod support \- keeps the core module clean and manageable

## Project Structure

- the root project \- contains settings.gradle, gradle.properties, build.gradle.kts, etc. Responsible for merging
  everything together upon build.
    - `mod` \- parent module for everything that's working with the Minecraft code, Fabric API, etc.
        - `api` \- abstract layer of the mod, shouldn't contain mixins or other complex stuff
        - `core` \- core implementation of the mod, contains mixins, all assets, mod support for required mods, etc.
        - `integrations` \- parent module for isolated mod support of optional mod dependencies
        - `runner` \- module for running the mod in a development environment, only relevant for development. Should
          only contain code for the development environment.
    - `models` \- shared code between the mod modules and the annotation processor (primarily annotations)
    - `processor` \- annotation processor for automatic `mixins.json` generation, etc.

## Where to find things

- assets, fabric.mod.json, accesswidener -> `mod/core/src/main/resources`
- mod version -> `gradle.properties`
- minecraft, fabric-loader and fabric-loom version -> `gradle/libraries.versions.toml`
- fabric-api version & other mod dependencies -> `gradle/mod-dependencies.versions.toml`
- template for `mixins.json` files -> `processor/src/main/resources/default.mixins.json`
- development run configuration creation, dev auth configuration -> `mod/runner/build.gradle.kts`

## Using the template

1. Clone the repository
2. Open the project in your favorite IDE (IntelliJ IDEA is recommended)
3. Replace **everything** with the default mod id (`skybuddy`) with your own mod id (TODO: maybe reduce the amount of
   places this has to be done?)
4. Update the mod name, description, author, etc. in `mod/core/src/main/resources/fabric.mod.json`
5. Done. You can now start developing your mod!