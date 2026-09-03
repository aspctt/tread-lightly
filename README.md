# <p align=center> Tread Lightly </p>

![Version](https://img.shields.io/badge/Available_for-1.21.1-blue)
![Mod Loader](https://img.shields.io/badge/Mod_Loader-NeoForge-orange)
![Side](https://img.shields.io/badge/Side-Client_only-yellow)
![License](https://img.shields.io/badge/License-LGPL_v3_or_later-blue)

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

Tread Lightly is licensed under the **GNU Lesser General Public License, version 3 or later**. The full terms are in [LICENSE](./LICENSE), which incorporates [COPYING](./COPYING) by reference. What that means in practice:

* You may use, study, modify, and redistribute it, including commercially.
* **Sound packs are yours.** A resource pack is data the mod reads at runtime, not a derivative work, so packs carry no obligations whatsoever. Make them for any purpose, sell them on any terms you choose, start from the block map the mod ships, and override the mod's own resources freely. No permission needed and no attribution required.
* **Other mods and tools may depend on Tread Lightly under any licence**, including proprietary and All Rights Reserved ones. That is what the "Lesser" buys, and it is deliberate: modded block support should not require anyone to change their own licence.
* If you modify Tread Lightly itself and distribute the result, that modified version must also be LGPL v3 or later, with source available.

Tread Lightly began as a port of [Presence Footsteps](https://github.com/Sollace/Presence-Footsteps), used under the MIT licence it carried at the time. The attribution MIT requires, along with copyrights and trademarks, is in [NOTICE](./NOTICE).

## Documentation

* [CHANGE_LOG.md](./CHANGE_LOG.md) lists what has changed.

## Credits

### Core

* aspctt - design, implementation

### Built on

* [NeoForge](https://neoforged.net/) - mod loader
* [Presence Footsteps](https://github.com/Sollace/Presence-Footsteps) by Hurricaaane (Ha3) and Sollace - the mod this one grew out of, MIT

Tread Lightly began as a port of Presence Footsteps and is diverging into its own mod. It is built from the Minecraft 1.21.1 line of that project as it stood in November 2025, under the MIT License it carried at the time. Presence Footsteps has since moved to PolyForm Shield 1.0.0 for versions released after June 2026, and nothing from those is used here. The full attribution is in [NOTICE](./NOTICE).
