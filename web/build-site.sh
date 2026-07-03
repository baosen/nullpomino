#!/bin/sh
# Assemble the self-contained static site for the browser build in web/dist/.
# Serve it with a static server that supports HTTP Range requests (CheerpJ
# needs them; python3 -m http.server does NOT support Range), never file://:
#   npx http-server web/dist -p 8000 -c-1
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
echo "Serve with: npx http-server web/dist -p 8000 -c-1"
