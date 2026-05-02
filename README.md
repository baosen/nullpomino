# NullpoMino+

**NullpoMino** is an open-source action puzzle game that works on the Java platform. It has a wide variety of single-player modes and netplay to allow players to compete over the Internet or LAN.

### Building

#### Prerequisites
- Java 25 (JDK 25) or later
- [Bazel](https://bazel.build/) (see `.bazelversion`)

#### Run

```bash
./NullpoMino
```

The launcher invokes `bazel build` on first run to produce
`bazel-bin/NullpoMinoSDL_deploy.jar`, then launches the game. The SDL
frontend uses JNA to load `SDL3`, `SDL3_image`, `SDL3_mixer`, and
`SDL3_ttf` from your system library path.

#### Standalone tools

Swing-based utility programs live at the repo root alongside the
Java sources under `src/main/java/nullpomino/tool/`:

- `./ruleeditor` — edit game rulesets
- `./sequencer` — inspect replay piece sequences (by Zircean)
- `./musiclisteditor` — configure BGM files
- `./netserver` — run a netplay server
- `./netadmin` — manage a running NetServer
- `./airankstool` — generate a Ranks AI data file
