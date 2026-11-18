# <p align=center style="text-align: center;"> <img alt="Banner" src="https://i.ibb.co/wNJQFQKJ/Tread-Lightly-Title.png"> </p>

<p align="center" style="text-align: center;">
	<img alt="Available for" src="https://img.shields.io/badge/Available_for-1.21.1_--_26.3-blue">
	<img alt="Requires" src="https://img.shields.io/badge/Requires-Nothing-brightgreen">
	<img alt="License" src="https://img.shields.io/badge/License-LGPL--3.0--or--later-red">
</p>

<p align="center" style="text-align: center;">
	<img alt="Available for NeoForge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg">
	<img alt="Won't support Forge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/unsupported/forge_vector.svg">
</p>

<p align="center" style="text-align: center;">
	<a alt="Buy Me a Coffee" href="https://buymeacoffee.com/aspctt"><img alt="Buy Me a Coffee" height="40" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/donate/buymeacoffee-singular_vector.svg"></a>
</p>

<p align="center" style="text-align: center;">
	<a alt="Available on GitHub" href="https://github.com/aspctt/tread-lightly"><img alt="Available on GitHub" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg"></a>
	<a alt="Available on Modrinth" href="https://modrinth.com/mod/tread-lightly-aspctt"><img alt="Available on Modrinth" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/modrinth_vector.svg"></a>
	<a alt="Available on CurseForge" href="https://www.curseforge.com/minecraft/mc-mods/tread-lightly"><img alt="Available on CurseForge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/curseforge_vector.svg"></a>
</p>

> **Status:** stable. [CHANGELOG.md](./CHANGELOG.md) lists what changed in each version. Multiplayer is still the least tested part.

## Description

Tread Lightly gives every block its own footsteps. Vanilla picks one sound per block and plays it at a fixed rate, while Tread Lightly works out what is under each foot in turn and plays what fits the way you are moving over it. Mobs and other players get the same treatment, and horses, cows and other four-legged mobs step in uneven pairs.

A few sounds go on top of the step. Puddles and rain add a splash, long grass brushes your legs when you sprint or go barefoot and chains jingle as you pass them.

It replaces the sounds, not the game. Nothing is added, nothing is craftable, no block or entity behaves differently and no world data changes. Tread Lightly is also client only. A dedicated server never loads it, and two players in the same world can run different packs and hear different things.

What a block sounds like comes from a pack, not from the mod. Block maps, acoustics and audio are all read through the ordinary resource pack system, so packs stack, reload with F3+T and override each other in the order you set. Adding a sound to a block takes no code and no waiting on a mod update.

Tread Lightly is a deliberately simplified take on the idea. It goes for a small engine and a pack format you can read without having seen one before, rather than the largest feature set.

## Installation

Put the jar in your `mods` folder. Tread Lightly ships with a default sound pack, so it works as soon as the game loads.

Other sound packs are ordinary resource packs. Put them in `resourcepacks` and turn them on in Options > Resource Packs, where they layer over the defaults.

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

[YetAnotherConfigLib](https://modrinth.com/mod/yacl) is **optional** and only draws the settings screen. Without it Tread Lightly works the same, and the Mods list shows no config button. Its only 26.3 build so far declares that it needs 26.2, so NeoForge will not load it on 26.3. The screen will appear there once a build that accepts 26.3 is out.

On 1.21.1, footsteps also work on the ships of [Create Aeronautics](https://modrinth.com/mod/create-aeronautics) and anything else [Sable](https://modrinth.com/mod/sable) moves. Nothing extra is needed for that, since Sable brings the part Tread Lightly talks to.

## Writing a pack

A pack is a resource pack. This `assets/treadlightly/config/blockmap.json` makes moss blocks sound like grass and podzol like dirt with leaves on it:

```json
{
	"minecraft:moss_block": "grass",
	"minecraft:podzol": "dirt,leaves"
}
```

Packs layer over the defaults, so you only state what you are changing. A tag works in place of a block id, written as `#minecraft:planks`. The acoustics and audio sit beside the block map, laid out like the [default pack](./src/main/resources/assets/treadlightly).

Two things help while you work:

* `/treadlightly report` writes a JSON file to the `treadlightly` folder in your game directory. By default it lists only the blocks nothing has an opinion about, which is the list worth working through, and `/treadlightly report full` lists every block with what it resolves to.
* F3 shows what is under your feet and what you are looking at, and whether it was mapped directly, inherited from the block it was built from or left to the vanilla sound type. From 1.21.11 it is an entry of its own in the debug options, `treadlightly:footsteps`.

A block id that does not exist in the running version is simply unused, so one pack can serve every version. Where a block was renamed, map both ids or a tag that covers it.

## Building

Every Minecraft version is built from one source tree with [Stonecutter](https://stonecutter.kikugie.dev/). The targets are declared in [settings.gradle.kts](./settings.gradle.kts), each with its Minecraft, NeoForge and YACL versions in `versions/<target>/gradle.properties`.

```bash
./gradlew buildAll
```

That writes one jar per target under `versions/<target>/build/libs/`. To work on a single version, run `./gradlew "26.2:build"`, or switch the source tree over with `Set active project to 26.2` so the IDE resolves against that version. Run `Reset active project` before committing, so the tree goes back to 1.21.1.

Version specific code is marked inline with `//? if` comments, or handled as a rename in [stonecutter.gradle.kts](./stonecutter.gradle.kts) when nothing but a name changed.

### Checks

Two things compile cleanly and still fail in the game, so both are checked against the game's own bytecode with `javap`. CI runs them on every push.

* **Mixin targets.** A mixin names its targets in strings javac never looks at. `tools/check-mixin-targets.py` confirms that every injected method, accessor and shadowed field still exists on each target with the types the mixin expects. It also checks that each injection handler's arguments still match.
* **Older NeoForge builds.** A jar is compiled against one NeoForge build but accepts older ones too. `tools/check-linkage.py` reads every game method, field and class the jar uses and checks that each one exists in the oldest build it accepts. This is what caught the 26.1 jar calling a NeoForge method that 26.1 and 26.1.1 do not have.

```bash
python tools/check-mixin-targets.py
./gradlew :26.1:writeCompileClasspath -Pneo_version=26.1.0.19-beta -Pminecraft_version=26.1
python tools/check-linkage.py 26.1 26.1.0.19-beta
```

Raising a NeoForge minimum in `versions/<target>/gradle.properties` should come with the same check against the new floor, and CI's list of floors in `.github/workflows/build.yml` should match.

### Releasing

`./gradlew publishMods` puts every target on Modrinth and CurseForge with its sources jar, through [mod-publish-plugin](https://modmuss50.github.io/mod-publish-plugin/). It also creates one GitHub release for the version and attaches the main jars to it. Each platform gets the targets one at a time, oldest Minecraft version first. The notes come from the version's section of `CHANGELOG.md`, and the portals list each jar for the Minecraft versions in `release_minecraft_versions`.

Releases normally go out through the Release workflow:

1. Set `mod_version` in `gradle.properties` and give the version its section in `CHANGELOG.md`.
2. Commit and tag that commit with the version, like `1.1.2`. Push both.
3. Start the Release workflow from the Actions tab with Run workflow. Ticking "dry run" shows what would be uploaded without sending anything.

The workflow refuses to run unless the tag exists and marks the commit it is building. It reads `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` from the repository's Actions secrets. The first is a Modrinth personal access token that can create, read and write versions, and the second a CurseForge API token. GitHub uses the workflow's own token.

To publish from your own machine instead, set those two tokens and a `GITHUB_TOKEN` that can write the repository's contents as environment variables, then run `./gradlew publishMods`. With any of the three missing the run is a dry run, which uploads nothing and writes what it would have sent under each project's `build/publishMods`.

## Licensing

Tread Lightly is licensed under the **GNU Lesser General Public License, version 3 or later**. The full terms are in [LICENSE](./LICENSE), which incorporates [COPYING](./COPYING) by reference. What that means in practice:

* You may use, study, modify and redistribute it, including commercially.
* **Sound packs are yours.** A resource pack is data the mod reads at runtime, not a derivative work, so packs carry no obligations at all. Make them for any purpose and sell them on any terms you choose. Start from the block map the mod ships and override its resources freely, with no permission or attribution needed.
* **Other mods and tools may depend on Tread Lightly under any licence**, including proprietary and All Rights Reserved ones. That is what the "Lesser" is for, since supporting a modded block should never mean changing your own licence.
* If you modify Tread Lightly itself and distribute the result, that version must also be LGPL v3 or later, with its source available.

The MIT attribution for Presence Footsteps, along with copyrights and trademarks, is in [NOTICE](./NOTICE).

## Credits

### Core

* aspctt: design and implementation

### Built on

* [NeoForge](https://neoforged.net/): mod loader
* [Presence Footsteps](https://github.com/Sollace/Presence-Footsteps) by Hurricaaane (Ha3) and Sollace: the mod Tread Lightly grew out of

Tread Lightly started from the Minecraft 1.21.1 line of Presence Footsteps as it stood in November 2025, under the MIT licence it carried then. It has gone its own way since. Presence Footsteps moved to PolyForm Shield 1.0.0 for versions released after June 2026, and nothing from those is used here.
