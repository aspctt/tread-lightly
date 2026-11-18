# <p align="center" style="text-align: center;"><img alt="Banner" src="https://i.ibb.co/wNJQFQKJ/Tread-Lightly-Title.png"></p>

<p align="center" style="text-align: center;"><img alt="Available for" src="https://img.shields.io/badge/Available_for-1.21.1_--_26.3-blue"> <img alt="Requires" src="https://img.shields.io/badge/Requires-Nothing-brightgreen"> <img alt="License" src="https://img.shields.io/badge/License-LGPL--3.0--or--later-red"></p>

<p align="center" style="text-align: center;"><img alt="Available for NeoForge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg"> <img alt="Won't support Forge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/unsupported/forge_vector.svg"></p>

<p align="center" style="text-align: center;"><a href="https://buymeacoffee.com/aspctt"><img alt="Buy Me a Coffee" height="40" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/donate/buymeacoffee-singular_vector.svg"></a></p>

<p align="center" style="text-align: center;"><a href="https://github.com/aspctt/tread-lightly"><img alt="Available on GitHub" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg"></a> <a href="https://modrinth.com/mod/tread-lightly-aspctt"><img alt="Available on Modrinth" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/modrinth_vector.svg"></a> <a href="https://www.curseforge.com/minecraft/mc-mods/tread-lightly"><img alt="Available on CurseForge" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/curseforge_vector.svg"></a></p>

Tread Lightly gives every block its own footsteps. Vanilla picks one sound per block and plays it at a fixed rate, while Tread Lightly works out what is under each foot in turn and plays what fits the way you are moving over it. Mobs and other players get the same treatment, and horses, cows and other four-legged mobs step in uneven pairs.

A few sounds go on top of the step. Puddles and rain add a splash, long grass brushes your legs when you sprint or go barefoot and chains jingle as you pass them.

It replaces the sounds, not the game. Nothing is added, nothing is craftable, no block or entity behaves differently and no world data changes.

### Sound packs

What a block sounds like comes from a resource pack, not from the mod. Block maps, acoustics and audio are read through the ordinary resource pack system, so packs stack, reload with F3+T and override each other in the order you set. Adding a sound to a block takes no code and no mod update.

The format is kept small on purpose, so you can read a pack without having seen one before. `/treadlightly report` lists every block no pack has mapped yet, so you know where to start.

### Client only

Tread Lightly runs entirely on your own machine. A dedicated server neither loads it nor needs to know it exists, and two players in the same world can run different packs and hear different things. When you host a world for friends, the game's own footsteps are left alone so your guests still hear them.

### Requirements

- Minecraft 1.21.1 with NeoForge 21.1.235 or newer
- Minecraft 1.21.11 with NeoForge 21.11.45 or newer
- Minecraft 26.1, 26.1.1 or 26.1.2 with NeoForge 26.1.0.19-beta or newer, all from one file
- Minecraft 26.2 with NeoForge 26.2.0.76 or newer
- Minecraft 26.3 with NeoForge 26.3.0.12-beta or newer

[YetAnotherConfigLib](https://modrinth.com/mod/yacl) is optional and only draws the settings screen. Without it Tread Lightly runs on its defaults. YetAnotherConfigLib has no build that loads on 26.3 yet, so 26.3 has no settings screen for now.

[Create Aeronautics](https://modrinth.com/mod/create-aeronautics) and anything else [Sable](https://modrinth.com/mod/sable) moves are supported on 1.21.1.

### License

Tread Lightly is licensed under the GNU Lesser General Public License, version 3 or later. The full terms are in [LICENSE](https://github.com/aspctt/tread-lightly/blob/main/LICENSE), which incorporates [COPYING](https://github.com/aspctt/tread-lightly/blob/main/COPYING) by reference. In practice:

- Use, study, modify and redistribute it, including commercially.
- Sound packs are yours. A resource pack is data the mod reads at runtime, not a derivative work, so packs carry no obligations at all. Make them for any purpose and sell them on any terms you choose. Start from the block map the mod ships and override its resources freely, with no permission or attribution needed.
- Other mods and tools may depend on Tread Lightly under any licence, including proprietary and All Rights Reserved ones.
- If you modify Tread Lightly itself and distribute it, that version must also be LGPL v3 or later, with its source available.

Tread Lightly began as a port of Presence Footsteps, used under the MIT licence it carried at the time. That attribution, along with trademarks and third-party licences, is in [NOTICE](https://github.com/aspctt/tread-lightly/blob/main/NOTICE).
