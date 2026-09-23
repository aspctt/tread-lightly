plugins {
	id("java-library")
	id("net.neoforged.moddev") version "2.0.147"
	id("idea")
}

// This script is the central build for every versioned subproject, so anything that reads a file has to
// reach for the root rather than the version directory it is being evaluated in.
fun prop(name: String): String = property(name) as String

val modId: String = prop("mod_id")

// The game version rides along in the mod version, so the jar, the Mods list and a crash report all say
// the same thing. Maven's version comparison, which NeoForge resolves dependency ranges with, sorts
// 1.0.0+1.21.1 above 1.0.0 and below 1.0.1, so nothing depending on this is confused by it.
version = "${prop("mod_version")}+${prop("minecraft_version")}"
group = prop("mod_group_id")

base {
	archivesName = prop("archives_name")
}

// Compared numerically against the Stonecutter target name, because 1.21.11 sorts before 1.21.2 as a
// string and 26.1 sorts before 1.21.1.
fun versionAtLeast(target: String): Boolean {
	fun parts(version: String) = version.split(".").map { it.toIntOrNull() ?: 0 }
	val current = parts(stonecutter.current.version)
	val other = parts(target)
	for (i in 0 until maxOf(current.size, other.size)) {
		val a = current.getOrElse(i) { 0 }
		val b = other.getOrElse(i) { 0 }
		if (a != b) return a > b
	}
	return true
}

sourceSets.main.get().resources {
	exclude("**/*.bbmodel") // BlockBench project files
	exclude("**/*.aup3")    // Audacity project files
	exclude("**/*.flac")    // Uncompressed sound masters, shipped only as .ogg
}

repositories {
	maven("https://maven.isxander.dev/releases") {
		name = "Xander Maven"
		content { includeGroup("dev.isxander") }
	}
}

// Mojang ships Java 21 to end users through 1.21.11, and Java 25 from 26.1.
val javaVersion = if (versionAtLeast("26.1")) 25 else 21
java.toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)

neoForge {
	version = prop("neo_version")

	// Parchment lags new Minecraft releases, so a target without it still builds; only the parameter
	// names and javadoc are missing.
	if (project.hasProperty("parchment_mappings_version")) {
		parchment {
			mappingsVersion = prop("parchment_mappings_version")
			minecraftVersion = prop("parchment_minecraft_version")
		}
	}

	// Tread Lightly is client-only: it produces sound on the machine that renders the world and has
	// nothing to do on a dedicated server. Only a client run is configured.
	runs {
		create("client") {
			client()
			systemProperty("forge.logging.markers", "REGISTRIES")
			logLevel = org.slf4j.event.Level.DEBUG
			// One run directory shared by every version, so worlds and options survive switching targets.
			gameDirectory = rootProject.file("run/client")
		}
	}

	mods {
		create(modId) {
			sourceSet(sourceSets.main.get())
		}
	}
}

// Present when the development client launches, but never pulled in by anything depending on this mod.
val localRuntime = configurations.create("localRuntime")
configurations.runtimeClasspath.get().extendsFrom(localRuntime)

dependencies {
	// Compiled against, but not required at runtime: every call into it sits behind a check that it is
	// actually loaded. Non-transitive: it declares dependencies that live on neither its own maven nor
	// Maven Central, and ships them inside its jar for the loader to unpack, so resolving them here would
	// fail over something we never touch.
	compileOnly("dev.isxander:yet-another-config-lib:${prop("yacl_version")}") { isTransitive = false }
	if (findProperty("yacl_dev_runtime") != "false") {
		localRuntime("dev.isxander:yet-another-config-lib:${prop("yacl_version")}") { isTransitive = false }
	}
}

// Expand the declared properties into the mod metadata and the mixin config, which is where they live so
// that versions are declared once in gradle.properties rather than duplicated.
val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
	val replaceProperties = mapOf(
		"minecraft_version_range" to prop("minecraft_version_range"),
		"neo_version_range" to prop("neo_version_range"),
		"loader_version_range" to prop("loader_version_range"),
		"mod_id" to modId,
		"mod_name" to prop("mod_name"),
		"mod_license" to prop("mod_license"),
		"mod_version" to project.version.toString(),
		"yacl_min_version" to prop("yacl_min_version"),
		"java_version" to javaVersion.toString(),
	)
	inputs.properties(replaceProperties)
	expand(replaceProperties)
	from(rootProject.file("src/main/templates"))
	into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

// The LGPL requires the licence to accompany the binary, so the texts ship inside the JAR rather than
// only living in the repository. NOTICE carries the MIT attribution that the Presence Footsteps and MDK
// material requires, which travels with it for the same reason.
tasks.jar {
	from(rootProject.file("LICENSE"))
	from(rootProject.file("COPYING"))
	from(rootProject.file("NOTICE"))

	// The loader goes on the file name only, so a download says what it is for. The mod version stays
	// free of it: the Mods list and crash reports already name the loader.
	archiveFileName = "${prop("archives_name")}-${project.version}-neoforge.jar"
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	// Deprecations are worth seeing: reaching for an API the game has moved on from is usually a sign of
	// having ported code from an older version.
	options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

// For tools/check-linkage.py: the exact compile classpath of the NeoForge build this target is configured
// against, which -Pneo_version can point at another build of the same Minecraft line for one run.
tasks.register("writeCompileClasspath") {
	val classpath = configurations.compileClasspath.get()
	val output = layout.buildDirectory.file("linkage/${prop("neo_version")}.classpath")
	inputs.files(classpath)
	outputs.file(output)
	doLast {
		output.get().asFile.writeText(classpath.files.joinToString("\n"))
	}
}

// IDEA no longer downloads sources/javadoc jars for dependencies on its own.
idea {
	module {
		isDownloadSources = true
		isDownloadJavadoc = true
	}
}
