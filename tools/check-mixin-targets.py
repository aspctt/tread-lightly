"""Check every mixin target against the bytecode of the game it will be applied to.

Mixin targets are annotation strings, so javac never validates them: a method that was renamed, a field that
moved, or an injection handler whose arguments no longer match all compile cleanly and only fail when the game
applies the mixin. This resolves each @Mixin class for a Stonecutter target and asks javap whether

- every `method =` target is declared on the mixin's target class, and an @Inject handler's arguments and
  CallbackInfoReturnable type line up with the method it picks
- every @Accessor and @Shadow field is declared on the target class with the type the mixin gives it
- every @At INVOKE target exists on its owner, following supertypes so NeoForge's interface extensions count

Run it from the repository root, after building the targets to check:

	./gradlew buildAll
	python tools/check-mixin-targets.py            # every target that has been built
	python tools/check-mixin-targets.py 26.3       # just the ones named
	python tools/check-mixin-targets.py 26.1 --neoforge 26.1.0.19-beta

The last form checks against another NeoForge build a target accepts, once its game jar has been fetched by
compiling against it: ./gradlew :26.1:compileJava -Pneo_version=26.1.0.19-beta -Pminecraft_version=26.1

It exits non-zero if any target has a problem.
"""

import glob, io, os, re, subprocess, sys

MIXIN_PACKAGE = "com/aspctt/treadlightly/mixin"
MIXIN_CONFIG = "treadlightly.mixins.json"

PRIMITIVES = {"boolean": "Z", "byte": "B", "char": "C", "short": "S", "int": "I", "long": "J",
			  "float": "F", "double": "D", "void": "V"}
BOXED = {"Boolean": "Z", "Byte": "B", "Character": "C", "Short": "S", "Integer": "I", "Long": "J",
		 "Float": "F", "Double": "D", "Void": "V"}


def strip_comments(src):
	out, i, n = [], 0, len(src)
	while i < n:
		if src.startswith("//", i):
			j = src.find("\n", i)
			i = n if j < 0 else j
		elif src.startswith("/*", i):
			j = src.find("*/", i + 2)
			seg = src[i:(n if j < 0 else j + 2)]
			out.append("\n" * seg.count("\n"))
			i = n if j < 0 else j + 2
		elif src[i] == '"':
			j = i + 1
			while j < n and src[j] != '"':
				if src[j] == "\\":
					j += 1
				j += 1
			out.append(src[i:j + 1])
			i = j + 1
		else:
			out.append(src[i])
			i += 1
	return "".join(out)


def split_top_level(text, sep=","):
	"""Split on sep, ignoring any inside angle brackets."""
	parts, depth, current = [], 0, []
	for ch in text:
		if ch == "<":
			depth += 1
		elif ch == ">":
			depth -= 1
		if ch == sep and depth == 0:
			parts.append("".join(current))
			current = []
		else:
			current.append(ch)
	if "".join(current).strip():
		parts.append("".join(current))
	return [p.strip() for p in parts]


class Source:
	"""One mixin file, with enough of its imports to turn source type names into descriptors."""

	def __init__(self, path):
		self.name = os.path.basename(path)
		self.text = strip_comments(io.open(path, encoding="utf-8").read())
		self.imports = {m.group(2): m.group(1) + m.group(2)
						for m in re.finditer(r'import ([a-z0-9_.]+\.)([A-Z][A-Za-z0-9_$]*);', self.text)}

	def binary_name(self, simple):
		"""net.minecraft.Foo$Bar for Foo.Bar, resolved through this file's imports."""
		head, _, rest = simple.partition(".")
		if head[0].islower():
			return simple
		base = self.imports.get(head, "java.lang." + head)
		return base + ("$" + rest.replace(".", "$") if rest else "")

	def descriptor(self, type_text):
		type_text = re.sub(r'@\w+(\.\w+)*\s*|\bfinal\s+', "", type_text).strip()
		type_text = re.sub(r'<.*>', "", type_text).strip()
		dims = type_text.count("[]")
		base = type_text.replace("[]", "").strip()
		desc = PRIMITIVES.get(base) or "L" + self.binary_name(base).replace(".", "/") + ";"
		return "[" * dims + desc


class Classpath:
	def __init__(self, jars):
		self.jars = os.pathsep.join(jars)
		self.cache = {}

	def dump(self, cls):
		"""javap output for a class, or None when it is not on the classpath."""
		if cls not in self.cache:
			try:
				self.cache[cls] = subprocess.run(["javap", "-p", "-s", "-cp", self.jars, cls],
												 capture_output=True, text=True, check=True).stdout
			except subprocess.CalledProcessError:
				self.cache[cls] = None
		return self.cache[cls]

	def members(self, cls):
		"""(methods, fields) declared on cls: [(name, descriptor)] each."""
		out = self.dump(cls)
		if out is None:
			return None, None
		methods, fields, pending = [], [], None
		for line in out.splitlines():
			stripped = line.strip()
			m = re.match(r'descriptor: (\S+)$', stripped)
			if m:
				if pending is not None:
					pending[0].append((pending[1], m.group(1)))
					pending = None
				continue
			pending = None
			if not stripped.endswith(";") or stripped.startswith("static {"):
				continue
			m = re.search(r'([A-Za-z_$][A-Za-z0-9_$.]*)\s*\(', stripped)
			if m:
				name = m.group(1).split(".")[-1]
				# Constructors print under the class name.
				pending = (methods, "<init>" if cls.endswith(name) and name[0].isupper() else name)
			else:
				pending = (fields, stripped[:-1].split()[-1])
		return methods, fields

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
		return [n for n in names if n and n[0].islower()]

	def has_method(self, cls, name, desc, depth=0):
		"""Whether cls or any supertype declares name with desc; None if cls is not on the classpath."""
		methods, _ = self.members(cls)
		if methods is None:
			return None
		if (name, desc) in methods:
			return True
		if depth < 4:
			for sup in self.supertypes(cls):
				if self.has_method(sup, name, desc, depth + 1):
					return True
		return False


def handler_after(src, end):
	"""The method an annotation ending at `end` sits on: (name, [parameter types])."""
	m = re.compile(r'\s*(?:@\w+(?:\([^)]*\))?\s*)*((?:(?:private|public|protected|static|final)\s+)*)'
				   r'[\w<>\[\].?, ]+?\s+([\w$]+)\s*\(([^)]*)\)').match(src, end)
	if not m:
		return None, None
	params = [p.rsplit(None, 1)[0] for p in split_top_level(m.group(3)) if p]
	return m.group(2), params


def annotation_end(src, start):
	"""Index just past the balanced parentheses of the annotation starting at `start`."""
	i = src.index("(", start)
	depth = 0
	while True:
		if src[i] == "(":
			depth += 1
		elif src[i] == ")":
			depth -= 1
			if depth == 0:
				return i + 1
		i += 1


def check_version(version, neo=None):
	# The target's own NeoForge unless told otherwise. The artifacts folder keeps every build a target has
	# been compiled against, so the jar is picked by version rather than taken wholesale.
	if neo is None:
		props = io.open("versions/%s/gradle.properties" % version, encoding="utf-8").read()
		neo = re.search(r'^neo_version=(\S+)', props, re.M).group(1)
	jars = [p for p in glob.glob("versions/%s/build/moddev/artifacts/*-%s*.jar" % (version, neo))
			if "sources" not in p and "client-extra" not in p]
	if not jars:
		return None
	# From 26.1 NeoForge's own classes are not merged into the game jar, so its universal jar joins the path.
	cache = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "modules-2", "files-2.1",
						 "net.neoforged", "neoforge", neo)
	jars += glob.glob(os.path.join(cache, "*", "neoforge-%s-universal.jar" % neo))
	cp = Classpath(jars)

	root = "versions/%s/build/generated/stonecutter/main/java/%s" % (version, MIXIN_PACKAGE)
	if not os.path.isdir(root):
		root = "src/main/java/" + MIXIN_PACKAGE

	# The generated mixin config is the authority on which mixins this target actually applies.
	active = None
	for config in ("versions/%s/build/generated/sources/modMetadata/%s" % (version, MIXIN_CONFIG),
				   "versions/%s/build/resources/main/%s" % (version, MIXIN_CONFIG)):
		if os.path.isfile(config):
			active = set(re.findall(r'"([A-Za-z0-9_$.]+)"', io.open(config, encoding="utf-8").read()))
			break

	problems, notes, checked = [], [], 0
	for name in sorted(os.listdir(root)):
		if not name.endswith(".java") or (active is not None and name[:-5] not in active):
			continue
		src = Source(os.path.join(root, name))
		text = src.text
		m = re.search(r'@Mixin\(([A-Za-z0-9_.]+)\.class\)', text)
		if not m:
			continue
		target = src.binary_name(m.group(1))
		methods, fields = cp.members(target)
		if methods is None:
			problems.append("%s: javap could not load the target %s" % (name, target))
			continue

		for inject in re.finditer(r'@(Inject|Redirect|ModifyArg|ModifyArgs|ModifyVariable|ModifyConstant|WrapOperation|WrapWithCondition|ModifyExpressionValue|ModifyReturnValue)\(', text):
			end = annotation_end(text, inject.start())
			body = text[inject.start():end]
			kind = inject.group(1)
			handler, params = handler_after(text, end)

			for selector in re.findall(r'method\s*=\s*"([^"]+)"', body):
				checked += 1
				mname = selector.split("(")[0]
				candidates = [d for n, d in methods if n == mname]
				if "(" in selector:
					candidates = [d for d in candidates if d == selector[len(mname):]]
				if not candidates:
					problems.append("%s: %s declares no method %s" % (name, target, selector))
					continue
				if kind != "Inject" or params is None:
					continue

				# An @Inject handler takes the target's arguments, then its CallbackInfo.
				ci = next((i for i, p in enumerate(params) if re.match(r'CallbackInfo(Returnable)?\b', p)), None)
				if ci is None:
					problems.append("%s: %s takes no CallbackInfo" % (name, handler))
					continue
				wanted = "(" + "".join(src.descriptor(p) for p in params[:ci]) + ")"
				matching = [d for d in candidates if d.startswith(wanted)]
				if not matching:
					problems.append("%s: %s takes %s, but %s.%s is %s" % (
						name, handler, wanted, target, mname, " or ".join(candidates)))
					continue
				if len(matching) > 1:
					problems.append("%s: %s matches %d overloads of %s; give the descriptor" % (
						name, mname, len(matching), target))
					continue
				returns = re.match(r'CallbackInfoReturnable<(.+)>$', params[ci])
				if returns:
					arg = returns.group(1).strip()
					expected = BOXED.get(arg) or src.descriptor(arg)
					actual = matching[0].split(")", 1)[1]
					if expected != actual:
						problems.append("%s: %s returns %s, but %s.%s returns %s" % (
							name, handler, expected, target, mname, actual))

			# @At INVOKE targets are descriptors too, and just as unchecked by javac.
			for owner, mname, mdesc in re.findall(r'target\s*=\s*"L([^;]+);([A-Za-z_$<][A-Za-z0-9_$>]*)(\([^"]*)"', body):
				checked += 1
				found = cp.has_method(owner.replace("/", "."), mname, mdesc)
				if found is None:
					notes.append("%s: %s is not on the classpath, skipped" % (name, owner))
				elif not found:
					problems.append("%s: @At %s.%s%s NOT FOUND" % (name, owner, mname, mdesc))

		# Accessors name a field, or take it from the method name.
		for acc in re.finditer(r'@Accessor(?:\(\s*(?:value\s*=\s*)?"([^"]+)"\s*\))?\s+([\w<>\[\].]+)\s+([\w$]+)\s*\(', text):
			checked += 1
			fname = acc.group(1) or re.sub(r'^(get|is|set)', "", acc.group(3))[:1].lower() + re.sub(r'^(get|is|set)', "", acc.group(3))[1:]
			check_field(problems, name, target, fields, fname, src.descriptor(acc.group(2)))

		# Shadowed fields have to be declared on the target itself, with the same type.
		for sh in re.finditer(r'@Shadow\s+((?:@\w+\s+)*)((?:(?:private|public|protected|static|final)\s+)*)([\w<>\[\].]+)\s+([\w$]+)\s*;', text):
			checked += 1
			check_field(problems, name, target, fields, sh.group(4), src.descriptor(sh.group(3)))

	print("%s on NeoForge %s: checked %d targets, %d problem(s)" % (version, neo, checked, len(problems)))
	for n in notes:
		print("  ~ " + n)
	for p in problems:
		print("  ! " + p)
	return len(problems)


def check_field(problems, name, target, fields, fname, wanted):
	types = [d for n, d in fields if n == fname]
	if not types:
		problems.append("%s: %s declares no field %s" % (name, target, fname))
	elif wanted not in types:
		problems.append("%s: %s.%s is %s, the mixin says %s" % (name, target, fname, types[0], wanted))


args = sys.argv[1:]
against = None
if "--neoforge" in args:
	i = args.index("--neoforge")
	against = args[i + 1]
	del args[i:i + 2]
versions = args or sorted(os.path.basename(p) for p in glob.glob("versions/*") if os.path.isdir(p))
failed, ran = 0, 0
for v in versions:
	result = check_version(v, against)
	if result is None:
		print("%s: not built, skipped" % v)
		continue
	ran += 1
	failed += result
if ran == 0:
	sys.exit("Nothing to check. Build the targets first.")
sys.exit(1 if failed else 0)
