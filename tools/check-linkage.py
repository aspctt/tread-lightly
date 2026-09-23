"""Check that a built target links against a different NeoForge build of the same Minecraft line.

One jar is published per target, but a target can accept more than the one NeoForge it was compiled against:
26.1 is built on 26.1.2 and has to run on 26.1 and 26.1.1 as well. Compiling the source against each of them
is not enough, because javac binds every call to whatever overload the compile classpath offers, and the
jar is only ever compiled once. This reads every game method, field and class the compiled jar refers to,
straight out of its constant pools so method references count too, and asks javap whether each one exists in
the other build, following supertypes the way the JVM resolves them.

Build the target as usual, have Gradle write out the other build's classpath, then check:

	./gradlew :26.1:compileJava
	./gradlew :26.1:writeCompileClasspath -Pneo_version=26.1.0.19-beta -Pminecraft_version=26.1
	python tools/check-linkage.py 26.1 26.1.0.19-beta

Checking a target against its own NeoForge build should always come back clean; it is a quick way to see the
checker itself is sound.

It exits non-zero if anything the jar needs is missing.
"""

import glob, io, os, re, subprocess, sys

GAME = ("net/minecraft/", "net/neoforged/", "com/mojang/")


class Classpath:
	def __init__(self, jars):
		self.jars = os.pathsep.join(jars)
		self.cache = {}

	def dump(self, cls):
		if cls not in self.cache:
			try:
				self.cache[cls] = subprocess.run(["javap", "-p", "-s", "-cp", self.jars, cls],
												 capture_output=True, text=True, check=True).stdout
			except subprocess.CalledProcessError:
				self.cache[cls] = None
		return self.cache[cls]

	def members(self, cls):
		out = self.dump(cls)
		if out is None:
			return None
		found, pending = set(), None
		for line in out.splitlines():
			stripped = line.strip()
			m = re.match(r'descriptor: (\S+)$', stripped)
			if m:
				if pending is not None:
					found.add((pending, m.group(1)))
				pending = None
				continue
			pending = None
			if not stripped.endswith(";") or stripped.startswith("static {"):
				continue
			m = re.search(r'([A-Za-z_$][A-Za-z0-9_$.]*)\s*\(', stripped)
			if m:
				name = m.group(1).split(".")[-1]
				pending = "<init>" if cls.endswith(name) and name[0].isupper() else name
			else:
				pending = stripped[:-1].split()[-1]
		return found

	def supertypes(self, cls):
		out = self.dump(cls)
		if out is None:
			return []
		header = next((l for l in out.splitlines() if re.search(r'\b(class|interface) ', l)), "").split("{")[0]
		# Generics go first: a class's own type parameters can say extends too.
		while True:
			stripped = re.sub(r'<[^<>]*>', "", header)
			if stripped == header:
				break
			header = stripped
		names = []
		for kw in ("extends", "implements"):
			m = re.search(r'\b%s\b(.*?)(?:\bimplements\b|$)' % kw, header)
			if m:
				names += [n.strip() for n in m.group(1).split(",")]
		return [n for n in names if n]

	def resolves(self, cls, name, desc, seen=None):
		"""Whether cls or anything it inherits from declares name with desc. None if cls itself is missing."""
		seen = seen if seen is not None else set()
		if cls in seen:
			return False
		seen.add(cls)
		found = self.members(cls)
		if found is None:
			return None
		if (name, desc) in found:
			return True
		return any(self.resolves(sup, name, desc, seen) for sup in self.supertypes(cls))


def references(classes_dir):
	"""Every game class, method and field the compiled classes refer to, with the files that do."""
	files = glob.glob(os.path.join(classes_dir, "**", "*.class"), recursive=True)
	out = subprocess.run(["javap", "-v", "-p"] + files, capture_output=True, text=True, check=True).stdout
	refs, current = {}, None
	for line in out.splitlines():
		m = re.match(r'Classfile .*?[/\\]classes[/\\]java[/\\]main[/\\](.+)\.class$', line.strip())
		if m:
			current = m.group(1).replace("\\", "/")
			continue
		m = re.search(r'= (Methodref|InterfaceMethodref|Fieldref|Class)\s+\S+\s+// (.+)$', line)
		if not m:
			continue
		kind, text = m.groups()
		text = text.strip().strip('"')
		if kind == "Class":
			if text.startswith(GAME):
				refs.setdefault(("class", text, None, None), set()).add(current)
			continue
		owner, _, rest = text.partition(".")
		name, _, desc = rest.partition(":")
		if owner.startswith(GAME):
			refs.setdefault((kind, owner, name.strip('"'), desc), set()).add(current)
	return refs


def main(target, other):
	classes = "versions/%s/build/classes/java/main" % target
	if not os.path.isdir(classes):
		sys.exit("versions/%s is not built." % target)

	# The full compile classpath of the other build, libraries included, as Gradle resolved it.
	listing = "versions/%s/build/linkage/%s.classpath" % (target, other)
	if not os.path.isfile(listing):
		sys.exit("No classpath for NeoForge %s under versions/%s. Write it first:\n"
				 "  ./gradlew :%s:writeCompileClasspath -Pneo_version=%s -Pminecraft_version=<its Minecraft version>"
				 % (other, target, target, other))
	cp = Classpath([l for l in io.open(listing, encoding="utf-8").read().splitlines() if l.strip()])

	problems = []
	refs = references(classes)
	for (kind, owner, name, desc), users in sorted(refs.items(), key=lambda r: (r[0][1], r[0][2] or "")):
		cls = owner.replace("/", ".")
		where = ", ".join(sorted(u.rsplit("/", 1)[-1] for u in users))
		if owner.startswith("["):
			continue
		if kind == "class":
			if cp.dump(cls) is None:
				problems.append("class %s is missing  (used by %s)" % (cls, where))
			continue
		found = cp.resolves(cls, name, desc)
		if found is None:
			problems.append("class %s is missing  (used by %s)" % (cls, where))
		elif not found:
			member = "%s : %s" % (name, desc) if kind == "Fieldref" else name + desc
			problems.append("%s.%s is missing  (used by %s)" % (cls, member, where))

	print("%s against NeoForge %s: checked %d references, %d missing" % (target, other, len(refs), len(problems)))
	for p in problems:
		print("  ! " + p)
	return 1 if problems else 0


if len(sys.argv) != 3:
	sys.exit(__doc__)
sys.exit(main(sys.argv[1], sys.argv[2]))
