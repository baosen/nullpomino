# NullpoMino

**NullpoMino** is an open-source action puzzle game that works on the Java platform. It has a wide variety of single-player modes and netplay to allow players to compete over the Internet or LAN.

### Building

#### Prerequisites
- Java 8 (JDK 1.8) or later
- Maven

#### Compile

```bash
mvn package
```

This produces:
- `target/nullpomino-7.6.0-SNAPSHOT.jar` -- the application JAR
- `target/lib/` -- dependency JARs and native libraries

#### Run

```bash
./NullpoMino
```

The SDL frontend uses JNA to load `SDL3`, `SDL3_image`, `SDL3_mixer`, and
`SDL3_ttf` from your system library path. The script builds the project
first, then launches the game.

#### Standalone tools

Swing-based utility programs live at the repo root alongside the
Java sources under `src/main/java/mu/nu/nullpo/tool/`:

- `./ruleeditor` — edit game rulesets
- `./sequencer` — inspect replay piece sequences (by Zircean)
- `./musiclisteditor` — configure BGM files
- `./netserver` — run a netplay server
- `./netadmin` — manage a running NetServer
- `./airankstool` — generate a Ranks AI data file
