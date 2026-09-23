# <p align=center> Tread Lightly </p>

![Version](https://img.shields.io/badge/Available_for-1.21.1_--_26.3-blue)
![Mod Loader](https://img.shields.io/badge/Mod_Loader-NeoForge-orange)
![Side](https://img.shields.io/badge/Side-Client_only-yellow)
![License](https://img.shields.io/badge/License-LGPL_v3_or_later-blue)

> **Status: ready for a first release.** The engine, the default pack, the config screen and the pack authoring tools are all in. Multiplayer behaviour is the least exercised part.

## Description

Tread Lightly gives every block its own footsteps. Vanilla picks one sound per block and plays it at a fixed rate; Tread Lightly works out what is actually under each foot, in turn, and plays something appropriate for how you are moving over it.

It replaces the sounds, not the game. Nothing is added, nothing is craftable, no block or entity behaves differently, and no world data changes. The mod is client-only: a dedicated server neither loads it nor needs to know it exists, and two players in the same world can run different packs and legitimately hear different things.

What a block sounds like is defined by a pack, not by the mod. Block maps, acoustics, and audio all come from resource packs, read through the ordinary resource pack system, which means packs stack, reload with F3+T, and override each other in the order the player sets. Adding a sound to a block should not require writing a mod, or waiting for someone else to.

This is a deliberately simplified take on the idea. The intent is a smaller engine with a format that is legible to someone who has never seen it before, rather than the largest possible feature set.

## Installation

Place the JAR in your `mods` folder. It ships a default sound pack, so it works as soon as it loads.

Additional sound packs are ordinary resource packs. Put them in `resourcepacks/` and enable them in Options > Resource Packs; they layer over the defaults, so a pack need only state what it changes.

## Dependencies

One jar per Minecraft version. The 26.1 jar covers 26.1, 26.1.1 and 26.1.2.

| Minecraft | NeoForge | YetAnotherConfigLib |
|---|---|---|
| 1.21.1 | 21.1.235 or newer | 3.6 or newer |
| 1.21.11 | 21.11.45 or newer | 3.8.1 or newer |
| 26.1, 26.1.1, 26.1.2 | 26.1.0.19-beta or newer | 3.9.6 or newer |
| 26.2 | 26.2.0.76 or newer | 3.9.6 or newer |
| 26.3 | 26.3.0.12-beta or newer | none loads yet |

The NeoForge minimum for each is the oldest build that jar has been checked against, not a guess.

[YetAnotherConfigLib](https://modrinth.com/mod/yacl) is **optional** and only draws the settings screen. Without it the mod works and the Mods list simply shows no config button. Its only 26.3 build so far declares that it needs 26.2, so NeoForge will not load it on 26.3; the screen will appear once a build that accepts 26.3 is out.

## Writing a pack

A pack is a resource pack. Put the block map, acoustics and audio under `assets/treadlightly/` and it layers over the defaults, so you need only state what you are changing.

Two things help while you work:

* `/treadlightly report` writes out what every block currently resolves to. By default it lists only the blocks nothing has an opinion about, which is the list worth working through. `/treadlightly report full` lists everything.
* F3 shows what is under your feet and what you are looking at, and whether it was mapped directly, inherited from the block it was built from, or fell through to the vanilla sound type. From 1.21.11 it is an entry of its own in the debug options, `treadlightly:footsteps`.

A block id that does not exist in the running version is simply unused, so one pack can serve every version. Where a block was renamed, map both ids, or a tag that covers it.

## Building

Every Minecraft version is built from one source tree with [Stonecutter](https://stonecutter.kikugie.dev/). The targets are declared in [settings.gradle.kts](./settings.gradle.kts), each with its Minecraft, NeoForge and YACL versions in `versions/<target>/gradle.properties`.

```bash
./gradlew buildAll
```

That writes one jar per target under `versions/<target>/build/libs/`. To work on a single version, run `./gradlew "26.2:build"`, or switch the source tree over with the "Set active project to ..." tasks so the IDE resolves against that version. Run `Reset active project` before committing, so the tree goes back to 1.21.1.

Version specific code is marked inline with `//? if` comments, or handled as a rename in [stonecutter.gradle.kts](./stonecutter.gradle.kts) when nothing but a name changed.

### Checks

Two things compile cleanly and still fail in the game, so both are checked against the game's own bytecode with `javap`. CI runs them on every push.

* **Mixin targets.** A mixin names its targets in strings javac never looks at. `tools/check-mixin-targets.py` confirms every injected method, accessor and shadowed field still exists on each target, with the types the mixin expects, and that each injection handler's arguments still match.
* **Older NeoForge builds.** A jar is compiled once, against one NeoForge build, but accepts older ones too. `tools/check-linkage.py` reads every game method, field and class the jar uses and checks each one exists in the oldest build it accepts. This is what caught the 26.1 jar calling a NeoForge method that 26.1 and 26.1.1 do not have.

```bash
python tools/check-mixin-targets.py
./gradlew :26.1:writeCompileClasspath -Pneo_version=26.1.0.19-beta -Pminecraft_version=26.1
python tools/check-linkage.py 26.1 26.1.0.19-beta
```

Raising a NeoForge minimum in `versions/<target>/gradle.properties` should come with the same check against the new floor, and CI's list of floors in `.github/workflows/build.yml` should match.

## Licensing

Tread Lightly is licensed under the **GNU Lesser General Public License, version 3 or later**. The full terms are in [LICENSE](./LICENSE), which incorporates [COPYING](./COPYING) by reference. What that means in practice:

* You may use, study, modify, and redistribute it, including commercially.
* **Sound packs are yours.** A resource pack is data the mod reads at runtime, not a derivative work, so packs carry no obligations whatsoever. Make them for any purpose, sell them on any terms you choose, start from the block map the mod ships, and override the mod's own resources freely. No permission needed and no attribution required.
* **Other mods and tools may depend on Tread Lightly under any licence**, including proprietary and All Rights Reserved ones. That is what the "Lesser" buys, and it is deliberate: modded block support should not require anyone to change their own licence.
* If you modify Tread Lightly itself and distribute the result, that modified version must also be LGPL v3 or later, with source available.

Tread Lightly began as a port of [Presence Footsteps](https://github.com/Sollace/Presence-Footsteps), used under the MIT licence it carried at the time. The attribution MIT requires, along with copyrights and trademarks, is in [NOTICE](./NOTICE).

## Documentation

* [CHANGELOG.md](./CHANGELOG.md) lists what has changed.

## Credits

### Core

* aspctt - design, implementation

### Built on

* [NeoForge](https://neoforged.net/) - mod loader
* [Presence Footsteps](https://github.com/Sollace/Presence-Footsteps) by Hurricaaane (Ha3) and Sollace - the mod this one grew out of, MIT

Tread Lightly began as a port of Presence Footsteps and is diverging into its own mod. It is built from the Minecraft 1.21.1 line of that project as it stood in November 2025, under the MIT License it carried at the time. Presence Footsteps has since moved to PolyForm Shield 1.0.0 for versions released after June 2026, and nothing from those is used here. The full attribution is in [NOTICE](./NOTICE).
