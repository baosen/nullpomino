#!/bin/sh
# Assemble the self-contained static site for the browser build in web/dist/.
# Everything is plain static files (TeaVM-compiled JS plus assets fetched over
# HTTP), so any static server works — no HTTP Range support needed:
#   python3 -m http.server -d web/dist 8000
set -eu
cd "$(dirname "$0")/.."

# TeaVM 0.14's ASM front end rejects bytecode newer than Java 17, and the
# compiler must run on a JDK <=17 — the default .bazelrc config already
# builds and compiles at Java 17, so no special flag is needed here.
bazel build //web:classes_js //:config_manifest //:res_manifest

rm -rf web/dist
mkdir -p web/dist
cp bazel-bin/web/classes.js web/dist/classes.js
cp web/index.html web/dist/index.html
cp bazel-bin/config-manifest.txt web/dist/config-manifest.txt
cp bazel-bin/res-manifest.txt web/dist/res-manifest.txt
# Default config, served loose: the bootstrap fetches the manifest, then each
# file, to seed persistent (localStorage) settings on first run.
cp -r config web/dist/config
# res/ stays loose files, fetched lazily per file over HTTP.
cp -r res web/dist/res

echo "Site assembled in web/dist"
echo "Serve with: python3 -m http.server -d web/dist 8000"
