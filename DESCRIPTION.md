# <p align=center> Tread Lightly </p>

![Version](https://img.shields.io/badge/Available_for-1.21.1-blue)
![Mod Loader](https://img.shields.io/badge/Mod_Loader-NeoForge-orange)
![Side](https://img.shields.io/badge/Side-Client_only-yellow)
![License](https://img.shields.io/badge/License-All_Rights_Reserved-red)

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

Tread Lightly is All Rights Reserved. The full terms are in [LICENSE](LICENSE), and the short version is:

- Download it and play with it freely.
- Sound packs are yours. Create them for any purpose including commercial ones, distribute and sell them on any terms you choose, and use the pack formats, schemas, field names, directory layouts, and sound vocabularies to do it. You may start from the block map the mod ships and edit it, and you may override the mod's own resources from inside a pack. No ownership is claimed over your work and no attribution is required. This grant is irrevocable: it cannot be withdrawn from packs already published, and it survives any future change to the licence.
- Separate mods, tools, editors, and validators that interoperate through the public API or pack formats are equally permitted.
- Modpacks may include the mod by reference, the way a CurseForge manifest or a Modrinth index does, so the launcher fetches it from an official page. Re-hosting, bundling, or altering the JAR is not permitted.
- The mod's own source code and assets stay reserved. Material that comes from elsewhere, including the Presence Footsteps code and sounds this mod is built from, stays under its own licence and is unaffected by that. See [NOTICE](NOTICE).

Trademarks and third-party licences are covered in [NOTICE](NOTICE).
