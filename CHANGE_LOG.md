# Tread Lightly :: Change Log
- - -

* Unreleased: 1.0.0

	+ **Project**
		* Converted the NeoForge MDK and the scaffolding carried over from Ballistic API into Tread Lightly
		* Mod id `treadlightly`, which doubles as the resource namespace sound packs address
		* Declared client only, in the mod class and in the manifest, so a dedicated server neither loads it nor reports a version mismatch against clients that do
		* Dropped the server, game test, and data generation run configurations, none of which apply to a client-side sound mod
		* Licence rewritten around a sound pack grant: packs may be made and sold on any terms, may start from the block map the mod ships, and may override the mod's own resources from inside a pack
		* Licence scoped to material original to this project, so it cannot purport to restrict the MIT material the mod is built from
		* Pinned the Presence Footsteps source to commit 2e9887ff of its 1.21 branch, the last state of the Minecraft 1.21.1 line before that project moved to PolyForm Shield 1.0.0 in June 2026, and recorded the MIT attribution it requires in NOTICE
