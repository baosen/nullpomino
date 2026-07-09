#!/usr/bin/env python3
"""Regenerate test_map.bzl for the per-file java_test rules in BUILD.bazel.

Scans every test source for (a) references to top-level classes defined in
sibling test files of the same package -> EXTRA_SRCS (transitive closure),
(b) files whose declared top-level classes differ from the filename-derived
class (extra classes, or none matching the filename at all) -> SELECT_CLASSES,
and (c) which of //src/main/java's fine-grained targets each test actually
needs (by import, or by same-package bare reference) -> TEST_DEPS. (c) is
derived from `bazel query`'d srcs of each target, so it can't drift from
BUILD.bazel -- rerun this script after moving a main source file between
targets.
Run from the repo root: python3 src/test/java/gen_test_map.py
"""

import collections
import os
import re
import subprocess

ROOT = os.path.join(os.path.dirname(__file__))
OUT = os.path.join(ROOT, "test_map.bzl")
MAIN_ROOT = os.path.join(ROOT, "..", "main", "java")

# The fine-grained //src/main/java targets that replace the old monolithic
# :core. Kept as an explicit list (rather than parsing BUILD.bazel) so a
# stray typo in BUILD.bazel surfaces as a bazel query error, not a silent gap.
MAIN_TARGETS = [
    "sdl_binding",
    "util_base",
    "game_component_data",
    "core_engine",
    "game_wallkick_impls",
    "game_randomizer_impls",
    "game_menu",
    "tool_airankstool",
    "game_ai_impls",
    "gui_base",
    "game_net_room_transport",
    "game_net_misc",
    "game_net_room_misc",
    "game_net_room_core",
    "game_net_platform",
    "game_net_client",
    "util_mode_manager",
    "util_randomizer_registry",
    "util_wallkick_registry",
    "util_ai_registry",
    "core_ui_modes",
    "game_mode_impls",
    "util_standalone_mode_registry",
]


def build_fqcn_to_target():
    """FQCN (e.g. "nullpomino.game.ai.AIPlayer") -> "//src/main/java:<target>"."""
    fqcn_to_target = {}
    for target in MAIN_TARGETS:
        out = subprocess.run(
            ["bazel", "query", "labels(srcs, //src/main/java:%s)" % target],
            cwd=os.path.join(ROOT, "..", "..", ".."),
            capture_output=True,
            text=True,
            check=True,
        ).stdout
        for line in out.splitlines():
            line = line.strip()
            if not line.startswith("//src/main/java:nullpomino/") or not line.endswith(".java"):
                continue
            relpath = line[len("//src/main/java:"):]
            fqcn = relpath[: -len(".java")].replace("/", ".")
            fqcn_to_target[fqcn] = "//src/main/java:%s" % target
    return fqcn_to_target


def compute_test_deps(testfiles, rel, closure):
    fqcn_to_target = build_fqcn_to_target()
    by_pkg = collections.defaultdict(list)
    for fqcn in fqcn_to_target:
        by_pkg[fqcn.rsplit(".", 1)[0]].append(fqcn)

    # Catches both real imports and fully-qualified inline references
    # (some coverage tests write `new nullpomino.game.net.NetRoomInfo()`
    # rather than importing it).
    any_fqcn_re = re.compile(
        r"\b(?:" + "|".join(re.escape(fqcn) for fqcn in fqcn_to_target) + r")\b"
    )
    # A trailing ".*" is captured separately so wildcard imports (common in
    # the branch-coverage test files) resolve to every class of that package
    # the test text actually references, not just literal single-class
    # imports -- those aren't literal FQCN occurrences any_fqcn_re can see.
    wildcard_import_re = re.compile(r"^import\s+(?:static\s+)?([\w.]+)\.\*;", re.M)
    package_re = re.compile(r"^package\s+([\w.]+);", re.M)

    def add_package_refs(targets, text, pkg):
        for fqcn in by_pkg.get(pkg, ()):
            cls = fqcn.rsplit(".", 1)[1]
            if re.search(r"\b" + re.escape(cls) + r"\b", text):
                targets.add(fqcn_to_target[fqcn])

    test_deps = {}
    for f in testfiles:
        group = [f] + sorted(closure(f, set()) - {f})
        text = "\n".join(open(g, encoding="utf-8").read() for g in group)

        targets = {fqcn_to_target[m.group(0)] for m in any_fqcn_re.finditer(text)}

        for m in wildcard_import_re.finditer(text):
            add_package_refs(targets, text, m.group(1))

        pkg_match = package_re.search(text)
        if pkg_match:
            add_package_refs(targets, text, pkg_match.group(1))

        if targets:
            test_deps[rel(f)] = sorted(targets)
    return test_deps


def main():
    files = []
    for dp, _, fns in os.walk(ROOT):
        files += [os.path.join(dp, fn) for fn in fns if fn.endswith(".java")]
    testfiles = [f for f in files if "/testutil/" not in f]
    rel = lambda f: os.path.relpath(f, ROOT)

    # Top-level classes per package dir, from declarations at column 0. A
    # file's class list may not match its filename (a package-private class
    # can live in an arbitrarily named file, e.g. TSpinAIBranchCoverageTest3
    # .java declares only TSpinAIBranchCoverageTest2), so --select-class args
    # must come from the declarations, not the filename.
    defs = collections.defaultdict(dict)  # dir -> class name -> file
    select_classes = {}
    decl = re.compile(r"^(?:public\s+|final\s+|abstract\s+)*(?:class|interface|enum)\s+(\w+)", re.M)
    testset = set(testfiles)
    for f in files:
        src = open(f, encoding="utf-8").read()
        base = os.path.basename(f)[:-5]
        pkg = os.path.dirname(rel(f)).replace("/", ".")
        declared = decl.findall(src)
        for name in declared:
            defs[os.path.dirname(f)][name] = f
        defs[os.path.dirname(f)].setdefault(base, f)
        if f in testset and declared != [base]:
            select_classes[rel(f)] = [pkg + "." + n for n in declared]

    direct = collections.defaultdict(set)
    for f in testfiles:
        src = open(f, encoding="utf-8").read()
        for name, deff in defs[os.path.dirname(f)].items():
            if deff != f and re.search(r"\b" + re.escape(name) + r"\b", src):
                direct[f].add(deff)

    def closure(f, seen):
        for d in direct.get(f, ()):
            if d not in seen:
                seen.add(d)
                closure(d, seen)
        return seen

    extra_srcs = {
        rel(f): sorted(rel(d) for d in closure(f, set()) - {f}) for f in direct
    }
    test_deps = compute_test_deps(testfiles, rel, closure)

    with open(OUT, "w") as out:
        out.write('"""Generated by gen_test_map.py -- do not edit by hand."""\n\n')
        for name, mapping in [
            ("EXTRA_SRCS", extra_srcs),
            ("SELECT_CLASSES", select_classes),
            ("TEST_DEPS", test_deps),
        ]:
            out.write("%s = {\n" % name)
            for k in sorted(mapping):
                out.write('    "%s": [\n' % k)
                for v in sorted(mapping[k]):
                    out.write('        "%s",\n' % v)
                out.write("    ],\n")
            out.write("}\n\n")
    print(
        "wrote %s: %d EXTRA_SRCS, %d SELECT_CLASSES, %d TEST_DEPS"
        % (OUT, len(extra_srcs), len(select_classes), len(test_deps))
    )


if __name__ == "__main__":
    main()
