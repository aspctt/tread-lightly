# <p align=center> Tread Lightly </p>

![Version](https://img.shields.io/badge/Available_for-1.21.1-blue)
![Mod Loader](https://img.shields.io/badge/Mod_Loader-NeoForge-orange)
![Side](https://img.shields.io/badge/Side-Client_only-yellow)
![License](https://img.shields.io/badge/License-All_Rights_Reserved-red)

> **Status: in development.** The project skeleton is in place. The sound engine is not.

## Description

Tread Lightly gives every block its own footsteps. Vanilla picks one sound per block and plays it at a fixed rate; Tread Lightly works out what is actually under each foot, in turn, and plays something appropriate for how you are moving over it.

It replaces the sounds, not the game. Nothing is added, nothing is craftable, no block or entity behaves differently, and no world data changes. The mod is client-only: a dedicated server neither loads it nor needs to know it exists, and two players in the same world can run different packs and legitimately hear different things.

What a block sounds like is defined by a pack, not by the mod. Block maps, acoustics, and audio all come from resource packs, read through the ordinary resource pack system, which means packs stack, reload with F3+T, and override each other in the order the player sets. Adding a sound to a block should not require writing a mod, or waiting for someone else to.

This is a deliberately simplified take on the idea. The intent is a smaller engine with a format that is legible to someone who has never seen it before, rather than the largest possible feature set.

## Installation

Place the JAR in your `mods` folder.

Sound packs are ordinary resource packs. Put them in `resourcepacks/` and enable them in Options > Resource Packs.

## Dependencies

* Minecraft 1.21.1
* NeoForge 21.1.235 or newer

## Licensing

Tread Lightly is **All Rights Reserved**. The full terms are in [LICENSE](./LICENSE). The short version:

* You may download it and play with it.
* **Sound packs are yours.** You may create them for any purpose including commercial ones, distribute and sell them on any terms you choose, and use the pack formats, schemas, field names, directory layouts, and sound vocabularies to do it. You may start from the block map the mod ships and edit it, and you may override the mod's own resources from inside a pack. The project claims no ownership over your work, requires no attribution, and this grant is irrevocable: it cannot be withdrawn from packs already published, and it survives any future change to the licence.
* Separate mods, tools, editors, and validators that interoperate through the public API or pack formats are equally permitted.
* Modpacks may include the mod **by reference**, the way a CurseForge manifest or a Modrinth index does, so the launcher fetches it from an official page. Re-hosting, bundling, or altering the JAR is not permitted.
* The mod's own source code and assets stay reserved. Material that comes from elsewhere, including the Presence Footsteps code and sounds this mod is built from, stays under its own licence and is unaffected by that. See [NOTICE](./NOTICE).

Please note the copyrights and trademarks in [NOTICE](./NOTICE).

## Documentation

* [CHANGE_LOG.md](./CHANGE_LOG.md) lists what has changed.

## Credits

### Core

* aspctt - design, implementation

### Built on

* [NeoForge](https://neoforged.net/) - mod loader
* [Presence Footsteps](https://github.com/Sollace/Presence-Footsteps) by Hurricaaane (Ha3) and Sollace - the mod this one derives from, MIT

Tread Lightly is a NeoForge port and simplification of Presence Footsteps, built from the Minecraft 1.21.1 line of that project as it stood in November 2025, under the MIT License it carried at the time. Presence Footsteps has since moved to PolyForm Shield 1.0.0 for the versions released after June 2026; nothing from those is used here. The full attribution is in [NOTICE](./NOTICE).
