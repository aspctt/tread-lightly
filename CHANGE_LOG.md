# Tread Lightly :: Change Log
- - -

* Unreleased: 1.0.0

	+ **Project**
		* Converted the NeoForge MDK and the scaffolding carried over from Ballistic API into Tread Lightly
		* Mod id `treadlightly`, which doubles as the resource namespace sound packs address
		* Declared client only, in the mod class and in the manifest, so a dedicated server neither loads it nor reports a version mismatch against clients that do
		* Dropped the server, game test, and data generation run configurations, none of which apply to a client-side sound mod
		* Licensed under the GNU Lesser General Public License v3 or later, replacing the All Rights Reserved terms the scaffolding arrived with
		* Sound packs need no licence grant of their own under the LGPL, since a resource pack is data read at runtime rather than a derivative work, so the hand-written pack clauses are gone entirely
		* Chose Lesser over full GPL so other mods and tools can depend on Tread Lightly whatever licence they carry, which full GPL would have prevented for the many mods that are All Rights Reserved
		* Files marked with SPDX identifiers rather than a full per-file licence header
		* Pinned the Presence Footsteps source to commit 2e9887ff of its 1.21 branch, the last state of the Minecraft 1.21.1 line before that project moved to PolyForm Shield 1.0.0 in June 2026, and recorded the MIT attribution it requires in NOTICE
