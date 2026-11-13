plugins {
	id("dev.kikugie.stonecutter")
	// Versioned here and applied bare in build.gradle.kts, so every subproject shares this one copy of the
	// plugin. Each subproject hands the root's GitHub task to parent(), which casts it to the plugin's own
	// task class, and that cast fails against a second copy loaded separately.
	id("me.modmuss50.mod-publish-plugin") version "2.2.1"
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

// Releasing. `./gradlew publishMods` puts every target on Modrinth and CurseForge and attaches each jar to
// one GitHub release for the version. The per-target half lives in build.gradle.kts.

val releaseVersion: String = providers.gradleProperty("mod_version").get()

// Uploads only happen with all three tokens set. Missing any one makes the whole run a dry run, which
// writes what would have been sent under each project's build/publishMods and contacts nothing, so a
// forgotten token can never leave a release up on some platforms and not others.
// A provider rather than a value, so only a build that publishes ever reads the tokens.
val releaseTokens = listOf("MODRINTH_TOKEN", "CURSEFORGE_TOKEN", "GITHUB_TOKEN")
val isReleaseDryRun: Provider<Boolean> = releaseTokens
	.map { name -> providers.environmentVariable(name).map { it.isBlank() }.orElse(true) }
	.reduce { isMissing, isOtherMissing -> isMissing.zip(isOtherMissing) { first, second -> first || second } }

// A link reference at the foot of CHANGELOG.md, such as "[1.1.1]: https://...", which ends the last section
val changelogLinkReference = Regex("""^\[[^\]]+]: .*""")

/**
 * One version's notes from CHANGELOG.md: everything under its "## [x.y.z]" heading, up to the next
 * heading or the link references. Called when a publish task runs rather than while configuring, so a
 * version without a section yet stops only a release, never a build.
 */
fun changelogSection(version: String): String {
	val lines = file("CHANGELOG.md").readLines()
	val start = lines.indexOfFirst { it.startsWith("## [$version]") }
	check(start >= 0) { "CHANGELOG.md has no section for $version, so there is nothing to publish as its changelog." }

	return lines.drop(start + 1)
		.takeWhile { !it.startsWith("## ") && !changelogLinkReference.matches(it) }
		.joinToString("\n")
		.trim()
}

publishMods {
	// GitHub's release notes, without the version heading its title already shows. The subprojects build
	// the portal changelogs from this, so CHANGELOG.md is the only place the notes are written.
	changelog = providers.provider { changelogSection(releaseVersion) }
	type = STABLE
	version = releaseVersion
	displayName = releaseVersion
	dryRun = isReleaseDryRun

	// The release itself, created empty. Each subproject then uploads its jar into it through parent().
	github {
		accessToken = providers.environmentVariable("GITHUB_TOKEN")
		repository = "aspctt/tread-lightly"
		// Only used when the tag does not exist yet, in which case GitHub creates it at the tip of main.
		// Push the tag first so it marks the commit the jars were built from.
		commitish = "main"
		tagName = releaseVersion
		allowEmptyFiles = true
	}
}

// On each platform the targets upload one at a time, oldest Minecraft version first, so the GitHub release
// lists its jars in that order and the newest version ends up on top of each portal's file list. Left
// alone, parallel execution uploads them in whatever order the workers finish. The platforms still run
// alongside each other. Wired once every target has been evaluated, since each one creates its own tasks.
gradle.projectsEvaluated {
	for (taskName in listOf("publishGithub", "publishModrinth", "publishCurseforge")) {
		val uploads = stonecutter.versions.map { project(":${it.project}").tasks.named(taskName) }
		uploads.zipWithNext { earlier, later ->
			later.configure { mustRunAfter(earlier) }
		}
	}
}
