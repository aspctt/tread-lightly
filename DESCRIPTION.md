# <p align=center>Tread Lightly</p>

![Version](https://img.shields.io/badge/Available_for-1.21.1-blue)
![Mod Loader](https://img.shields.io/badge/Mod_Loader-NeoForge-orange)
![Requires](https://img.shields.io/badge/Requires-Nothing-brightgreen)
![License](https://img.shields.io/badge/License-LGPL_v3_or_later-blue)

Tread Lightly gives every block its own footsteps. Vanilla picks one sound per block and plays it at a fixed rate. Tread Lightly works out what is actually under each foot, in turn, and plays something appropriate for how you are moving over it.

It replaces the sounds, not the game. Nothing is added, nothing is craftable, no block or entity behaves differently, and no world data changes.

### Sound packs

What a block sounds like is defined by a pack, not by the mod. Block maps, acoustics, and audio all come from resource packs, read through the ordinary resource pack system, so packs stack, reload with F3+T, and override each other in the order the player sets.

Adding a sound to a block requires no code and no mod update.

### Client only

The mod runs entirely on the machine that renders the world. A dedicated server neither loads it nor needs to know it exists, and two players in the same world can run different packs and legitimately hear different things.

### Simplified

This is a deliberately smaller take on the idea than the mod it comes from: a leaner engine and a format meant to be legible to someone who has never seen it before.

### Requirements

Minecraft 1.21.1 and NeoForge 21.1.235 or newer. Client side only.

### License

Tread Lightly is licensed under the GNU Lesser General Public License, version 3 or later. The full terms are in [LICENSE](LICENSE), which incorporates [COPYING](COPYING) by reference. In practice:

- Use, study, modify, and redistribute it, including commercially.
- Sound packs are yours. A resource pack is data the mod reads at runtime, not a derivative work, so packs carry no obligations whatsoever. Make them for any purpose, sell them on any terms you choose, start from the block map the mod ships, and override the mod's own resources freely. No permission needed and no attribution required.
- Other mods and tools may depend on Tread Lightly under any licence, including proprietary and All Rights Reserved ones.
- Modify Tread Lightly itself and distribute it, and that modified version must also be LGPL v3 or later, with source available.

Tread Lightly began as a port of Presence Footsteps, used under the MIT licence it carried at the time. That attribution, along with trademarks and third-party licences, is in [NOTICE](NOTICE).
