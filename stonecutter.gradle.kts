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
	}
}
