plugins {
	id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1"

// Builds every target in one go. Building one on its own still works as ./gradlew "<target>:build".
tasks.register("buildAll") {
	group = "build"
	description = "Assembles and tests every Minecraft version declared in settings.gradle.kts."
	dependsOn(stonecutter.versions.map { ":${it.project}:build" })
}

stonecutter parameters {
	// Available to source files as `//$ minecraft` swaps and in `//? if` conditions.
	swaps["minecraft"] = "\"${node.metadata.version}\";"

	// Pure renames only. Anything that changes arity, arguments or semantics is handled with an inline
	// `//? if` directive instead, so the difference is visible where it matters.
	replacements {
		string(current.parsed >= "1.21.11") {
			// ResourceLocation became Identifier. The mod has no type of its own whose name contains either
			// word, so these patterns cannot collide in either direction.
			replace("import net.minecraft.resources.ResourceLocation;", "import net.minecraft.resources.Identifier;")
			replace("ResourceLocation ", "Identifier ")
			replace("ResourceLocation,", "Identifier,")
			replace("ResourceLocation>", "Identifier>")
			replace("ResourceLocation.", "Identifier.")
			replace("ResourceLocation)", "Identifier)")

			// SoundEvent became a record. getStepSound always returns one, so this cannot catch the
			// SoundInstance accessor that was renamed alongside it; VanillaSoundSuppressor handles both.
			replace("getStepSound().getLocation()", "getStepSound().location()")

			// Accessors renamed with nothing else about them changed.
			replace("ResourceKey::location", "ResourceKey::identifier")
			replace("client.getTimer()", "client.getDeltaTracker()")

			// Util moved down into the util package it names.
			replace("import net.minecraft.Util;", "import net.minecraft.util.Util;")

			// Entity classes sorted into packages of their own. Only the package changed.
			replace("import net.minecraft.world.entity.animal.WaterAnimal;", "import net.minecraft.world.entity.animal.fish.WaterAnimal;")
			replace("import net.minecraft.world.entity.animal.horse.AbstractHorse;", "import net.minecraft.world.entity.animal.equine.AbstractHorse;")
		}

		string(current.parsed >= "26.1") {
			// The block state's holder accessor was renamed. It returns the same holder.
			replace("getBlockHolder()", "typeHolder()")
		}

		// The camera's position accessor was renamed in 1.21.11 and the renderer's camera accessor in
		// 26.2. Both bands spell out the whole call so neither depends on the other having run first.
		string(current.parsed >= "1.21.11" && current.parsed < "26.2") {
			replace("getMainCamera().getPosition()", "getMainCamera().position()")
		}
		string(current.parsed >= "26.2") {
			replace("getMainCamera().getPosition()", "mainCamera().position()")
		}

		string(current.parsed >= "26.2") {
			// Every entity type constant moved to a class of its own. The import is a directive in Lookups.
			replace("EntityType.PLAYER", "EntityTypes.PLAYER")
		}

		string(current.parsed >= "26.3") {
			// The four argument withinManhattan searched out to a depth of all three reaches added
			// together. 26.3 kept that iteration under this name and gave the old one a single reach.
			replace("BlockPos.withinManhattan(", "BlockPos.withinBoxByManhattanDistance(")
		}
	}
}
