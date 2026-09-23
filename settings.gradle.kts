pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
		maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.9.8"
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
	create(rootProject) {
		// One target per API band. 26.1 covers the whole 26.1.x line, so 26.1, 26.1.1 and 26.1.2 all
		// load the same jar; the Minecraft version a target actually builds against is declared in its
		// own versions/<target>/gradle.properties.
		versions("1.21.1", "1.21.11", "26.1", "26.2", "26.3")
		vcsVersion = "1.21.1"
	}
}

rootProject.name = "tread-lightly"
