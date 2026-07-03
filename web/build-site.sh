#!/bin/sh
# Assemble the self-contained static site for the browser build in web/dist/.
# Serve it with any static file server (never file://), e.g.:
#   python3 -m http.server 8000 --directory web/dist
set -eu
cd "$(dirname "$0")/.."

bazel build --config=web //:NullpoMinoWeb_deploy.jar

rm -rf web/dist
mkdir -p web/dist
cp bazel-bin/NullpoMinoWeb_deploy.jar web/dist/nullpomino.jar
cp web/index.html web/dist/index.html
# res/ must stay loose files: CheerpJ's /app mount fetches them lazily
# per-file over HTTP, so the 17 MB asset tree is never bulk-downloaded.
cp -r res web/dist/res

echo "Site assembled in web/dist"
echo "Serve with: python3 -m http.server 8000 --directory web/dist"
