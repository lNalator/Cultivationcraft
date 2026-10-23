# CultivationCraft: RE Xianxia

A Minecraft Forge mod about cultivation: Qi sources, realms (Foundation Establishment, Qi Condensation, Core Forming), elemental cores, concepts and techniques.

- Minecraft **1.19.2**, Forge **43.1.52**, official Mojang mappings
- Requires the **MLM Animator** mod ([lNalator/MultilLimbedModel](https://github.com/lNalator/MultilLimbedModel)), version 0.2.2 or later

## Requirements

- **JDK 17** (Temurin, Oracle or any other build). Gradle runs with the JDK in `JAVA_HOME`; the build itself is pinned to a Java 17 toolchain.
- Gradle itself is not needed: use the wrapper (`./gradlew`, Gradle 7.6.6).

## Set up the development environment

This mod builds against the MLM Animator **sources**, not a published jar. Clone both repositories side by side:

```
some-folder/
├── Cultivationcraft/
└── MultilLimbedModel/
```

```bash
git clone https://github.com/lNalator/Cultivationcraft.git
git clone https://github.com/lNalator/MultilLimbedModel.git
cd Cultivationcraft
```

If MLM Animator is somewhere else, pass its path to every Gradle command, or set it once in `~/.gradle/gradle.properties`:

```bash
./gradlew build -PmlmSourceDir=/path/to/MultilLimbedModel
```

`settings.gradle` includes that checkout as a composite build, so changes made in MLM Animator are picked up directly. Use a MLM Animator revision compatible with this mod: CI builds against the `v0.2.2` tag.

If your default Java is not 17, point Gradle to a JDK 17 first:

```bash
export JAVA_HOME=/path/to/jdk-17
```

### IDE

- **IntelliJ IDEA:** open `build.gradle` as a project, then run `./gradlew genIntellijRuns`.
- **Eclipse:** `./gradlew genEclipseRuns`, then import the folder as an existing Gradle project.
- **VS Code:** install the Java and Gradle extensions, then `./gradlew genVSCodeRuns`.

The first import decompiles Minecraft and can take several minutes.

## Build

```bash
./gradlew build
```

The mod jar is written to `build/libs/cultivationcraft-<version>.jar` (the version comes from `mod_version` in `gradle.properties`).

To play with it in a normal Minecraft install, put these in the `mods` folder of a Forge 1.19.2 (43.1.52 or later) profile:

- `cultivationcraft-<version>.jar`
- the MLM Animator jar (build it in `MultilLimbedModel` with `./gradlew build`, see its README)

## Test in game

| Command | What it does |
|---|---|
| `./gradlew runClient` | Starts Minecraft with the mod (and MLM Animator) loaded, in the `run/` folder |
| `./gradlew runServer` | Starts a dedicated server. The first time, set `eula=true` in `run/eula.txt` and run it again |
| `./gradlew runGameTestServer` | Runs the registered game tests then exits. There are no game tests yet, and Forge stops with an error when there are none |

Testing aids, available in the development client (or in a normal game launched with `-Dcultivationcraft.dev=true`):

- `/cultidev`
- `/cultivation`
- **Elemental Debug Stick** (creative Tools tab): right-click an entity, a block or the air to apply an element; sneak + right/left-click to cycle elements.

There is no automated test suite: changes are checked by building (`./gradlew build`) and testing in game.

