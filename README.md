# NullpoMino+

**NullpoMino+** is an open-source action puzzle game that works on the Java platform. It has a wide variety of single-player modes and netplay to allow players to compete over the Internet or LAN.

## Building

### Prerequisites

- Java 17 (JDK 17)
- [Bazel](https://bazel.build/) (see `.bazelversion`)

### Run

#### Web

```bash
web/build-site.sh
python3 -m http.server -d web/dist 8000 # other static file servers works too!
```

#### Desktop/laptop

```bash
./NullpoMino
```

### Tools

Swing-based utility programs live at the repo root alongside the
Java sources under `src/main/java/nullpomino/tool/`:

- `./ruleeditor` — edit game rulesets
- `./sequencer` — inspect replay piece sequences (by Zircean)
- `./musiclisteditor` — configure BGM files
- `./airankstool` — generate a Ranks AI data file

Netplay is peer-to-peer and needs no server — see [NETPLAY.md](NETPLAY.md).
