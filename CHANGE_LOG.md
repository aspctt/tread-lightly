# Tread Lightly :: Change Log
- - -

* Unreleased: 1.0.0

	First release. Tread Lightly grew out of Presence Footsteps and works the same way at a
	glance, but the engine has been rebuilt for NeoForge and a good deal of it behaves
	differently. The notes below say what is new, what was cut, and what was fixed on the way.

	+ **Sound**
		* Every block gets its own footsteps, chosen from what is actually under each foot rather than one sound per block played at a fixed rate
		* A step can layer several sounds at once: the surface itself, wetness over it, and plants brushing past
		* Carpets, fences and trapdoors resolve to what you are really standing on, not the block the coordinates say
		* Stairs, ladders, jumping, landing, turning on the spot and standing still all have their own sounds
		* Standing in water or lava plays swimming rather than whatever is underneath
		* Boots add their own sound over the surface, which is still heard underneath
		* Biome trim nudges volume and pitch, eased across a border rather than snapping
		* Ships a default pack covering all 1060 blocks in the game, in stereo for your own steps and mono for everyone else's

	+ **Packs**
		* Block maps, acoustics and audio all come from resource packs, so they stack, reload with F3+T, and override in the order the player sets
		* A pack need only state what it changes; everything else falls through to the defaults
		* Blocks inherit the sound of what they were built from, so modded stairs and slabs work without anyone writing entries for them
		* Modded blocks nothing knows about fall back to their vanilla sound type
		* Per-entity block maps, so a creeper can sound different underfoot from a player
		* `/treadlightly report` writes out what every block resolves to, listing by default only the blocks nothing has an opinion about
		* F3 shows what is underfoot and what you are looking at, and how each got there

	+ **Settings**
		* Config screen through YetAnotherConfigLib, which is optional: without it the mod works and only the screen is missing
		* Volume overall and per source, with separate control over the wet and foliage layers
		* Footsteps get louder as something works up to a run, off by default
		* Choose who is heard walking, and cap how many at once for busy scenes
		* Replace the game's own footsteps or sit alongside them

	+ **Multiplayer**
		* Other players' movement is inferred from position rather than velocity, which the server does not send
		* The game's own player sounds are silenced only where this mod is actually standing in for them, so a distant player's steps are never cancelled with nothing put in their place
		* Hosting a world for others leaves the game's footsteps alone, since those are what the guests hear

	+ **Cut**
		* No Mine Little Pony support, and no flying gaits: nothing in the game ever used them
		* No keybinds
		* No particle visualiser; the debug readout answers the same question without putting branches in the hottest function in the mod

	+ **Fixed along the way**
		* Weighted sound variants compared a draw against each entry's own share of the total rather than a running sum, so with two equal weights any draw above half matched nothing and the footstep fell silent, and rarer variants could never play at all
		* Delayed sounds played once and then jammed the queue for the rest of the session, and their scheduling treated an absolute timestamp as a duration
		* A delay declared as 100 to 300ms actually landed anywhere from 100 to 400
		* Footsteps faded in from silence over the first second and a half whenever an entity came into range
		* Two entities standing on the same block in the same tick could swap footsteps, playing at each other's positions and resolved against the wrong block map
		* Four acoustics in the default pack pointed at sound events that have never existed, so they were silent and warned on every step
		* The speed ramp overwrote two of its own tuning values every frame, so no pack could change them

	+ **Performance**
		* One world entity query per solving pass instead of one per lookup, on the hottest path in the mod
		* Biome lookups cached against the block position rather than resolved for every entity every tick
		* Results cached per tick, per entity
		* Past the entity limit, players and the nearest of each kind on a block keep their footsteps so a mob farm costs a bounded amount

	+ **Project**
		* Client only, in the mod class and the manifest, so a dedicated server neither loads it nor reports a version mismatch
		* Licensed LGPL v3 or later. Sound packs need no grant of their own: a resource pack is data read at runtime, not a derivative work
		* Lesser rather than full GPL so other mods and tools can depend on this whatever licence they carry
		* Built from the Minecraft 1.21.1 line of Presence Footsteps at commit 2e9887ff, the last state under the MIT licence, with the attribution that requires recorded in NOTICE
