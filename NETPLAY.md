# NullpoMino Netplay

## How It Works

### Transport & Protocol

- **TCP sockets** over port 9200. The server uses Java NIO (`Selector`/`SocketChannel`) for scalability; clients use plain `Socket` in a background thread.
- **Text-based protocol**: messages are tab-delimited, newline-terminated (`COMMAND\tARG1\tARG2\n`). Human-readable and easy to debug.
- Large payloads (field state, replays) are **Deflate-compressed + Base64-encoded**. Integrity checked via Adler32 checksums.

### Server

The server (`NetServer.java`) is the central authority. Key responsibilities:

- **Connection/auth** — handles login, observer login, and admin login (admin passwords RC4-encrypted). Players get a UID on successful login.
- **Lobby & rooms** — maintains a room list, player list, and chat history. Rooms hold up to 6 players plus spectators.
- **Game lifecycle** — `gameStart()` fires when all seated players are ready (sends a shared random seed), `gameFinished()` detects the winner, and the rating system updates.
- **Message relay** — the server's main job during gameplay is to **broadcast** each player's state updates to everyone else in the room via `processPacket()`.

### Client

- **`NetBaseClient`** — background thread that reads from the socket, buffers incomplete packets, and dispatches complete messages to `NetMessageListener` listeners.
- **`NetPlayerClient`** — extends the base client with player-specific state (room list, player list, current room).
- **`NetObserverClient`** — a lighter-weight base-client variant that logs in as a spectator (see [Observer mode](#observer-mode) below). Runs on the title screen when `observer.enable=true`.
- **`NetLobbyFrame`** — Swing-free session/protocol object. Owns the `NetPlayerClient`, chat buffers, room list, rule catalogue, and message pump. No longer a `JFrame`; all UI lives in SDL states.
- **`StateNet*SDL`** — SDL3 lobby screens (server select, lobby, room, create room, ranking, rule change) that read/mutate the shared `NetLobbyFrame` session.
- **`StateNetGameSDL`** — SDL game state that runs the netplay round once the server confirms a room join.

### Game Synchronization

This is **distributed state sync**, not lockstep or rollback. Each player runs their own game engine independently and broadcasts state changes:

| What's sent | Method | When |
|---|---|---|
| Current piece (id, x, y, rotation, color) | `netSendPieceMovement()` | Every frame the piece moves |
| Field state (compressed grid) | `netSendField()` | When blocks are placed/cleared |
| Next queue + hold piece | `netSendNextAndHold()` | When the queue changes |
| Stats (score, lines, etc.) | `netSendStats()` | Periodically |

The server relays these to all room members. Each client reconstructs other players' boards from the received data and renders them.

### Observer mode

Separate from the normal player flow, a client can connect to a server as a **read-only observer** via `NetObserverClient`. The protocol handshake is `observerlogin` instead of `login`: the server responds with the current room list and player counts but does **not** assign a UID, does not add the connection to the player list, and rejects any command that would mutate server state (join a room, chat, etc.).

The SDL frontend uses observer mode for a single purpose — a live activity indicator on the title screen. When `observer.enable=true` in `config/setting/netobserver.cfg`, `NullpoMinoSDL.startObserverClient()` opens a connection to the configured server on every re-entry into `STATE_TITLE` and renders the resulting `N/M` (observers/players) counter in the bottom-right corner of the menu. `stopObserverClient()` tears it down when leaving the title screen.

The **OBSERVE** button on the server-select screen is a shortcut for setting up this mode: it writes the picked server's host/port into `netobserver.cfg` with `observer.enable=true` and bounces back to the title, where the indicator starts up.

Not to be confused with **watching a running room**: that is done through the lobby's **VIEW** button, which opens the target room's settings in read-only detail mode. From there, the room-detail screen's **WATCH** button uses the normal `NetPlayerClient` connection to join as a spectator (no seat, no garbage sent/received, no rating change).

### Chat commands

Typed into the lobby or room chat box. Command prefix is case-insensitive (`/NAME`, `/Name`, and `/name` all work); arguments keep their original case.

| Command | Effect |
|---|---|
| `/name <nickname>` | Rename on the fly without reconnecting. The server rejects duplicates or renames while a game is running and broadcasts a system message to everyone in the lobby on success. |
| `/team <team name>` | Change (or clear, with no argument) the player's team. Team appears in parentheses next to the nickname in player lists. |

Anything not starting with `/` is sent as a normal chat message to the current context (lobby or room).

### Game Flow

1. **Login** — client sends `login`, gets `loginsuccess` + UID
2. **Lobby** — browse rooms, chat
3. **Join/Create room** — `roomjoin` / `roomcreate`
4. **Ready up** — `ready` toggle; server waits for all players
5. **Start** — server sends `start` with a **shared random seed** (so all players get the same piece sequence)
6. **Play** — each client runs independently, broadcasting piece/field/stats; server relays to the room
7. **End** — server detects last player standing, sends `finish`, updates ratings

### Key Files

| Component | Path |
|---|---|
| Server | `src/main/java/mu/nu/nullpo/game/net/NetServer.java` |
| Base client | `src/main/java/mu/nu/nullpo/game/net/NetBaseClient.java` |
| Player client | `src/main/java/mu/nu/nullpo/game/net/NetPlayerClient.java` |
| Observer client | `src/main/java/mu/nu/nullpo/game/net/NetObserverClient.java` |
| Net utilities | `src/main/java/mu/nu/nullpo/game/net/NetUtil.java` |
| Base netplay mode | `src/main/java/mu/nu/nullpo/game/net/NetDummyMode.java` |
| VS mode base | `src/main/java/mu/nu/nullpo/game/net/NetDummyVSMode.java` |
| Lobby session | `src/main/java/mu/nu/nullpo/gui/net/NetLobbyFrame.java` |
| SDL lobby screens | `src/main/java/mu/nu/nullpo/gui/sdl/StateNet*SDL.java` |
| SDL lobby widgets | `src/main/java/mu/nu/nullpo/gui/sdl/widget/` |

## Server Setup

### Quick Start

Run the launch script from the project's `scripts/` directory:

```sh
# Linux/macOS
./scripts/netserver

# Windows
scripts\netserver.bat

# Custom port (default is 9200)
./scripts/netserver 5000
```

Both scripts invoke `java -cp ... mu.nu.nullpo.game.net.NetServer` with the port as an optional first argument. The project must be built first with `mvn package`.

### Configuration

Edit `config/etc/netserver.cfg` before starting. The key settings:

**Basics:**

| Setting | Default | Notes |
|---|---|---|
| `netserver.port` | `9200` | Listening port |
| `netserver.admin.username` | `a` | Admin username (blank = disable admin) |
| `netserver.admin.password` | `b` | Admin password (blank = disable admin) |
| `netserver.timeoutTime` | `30000` | Dead connection timeout (ms), 0 to disable |

**Rating system** (ELO-based):

| Setting | Default | Notes |
|---|---|---|
| `netserver.ratingDefault` | `1500` | Starting rating |
| `netserver.ratingNormalMaxDiff` | `16` | K-value per game |
| `netserver.ratingAllowSameIP` | `true` | Set `false` for production to prevent abuse |

**Privacy:**

| Setting | Default | Notes |
|---|---|---|
| `netserver.showhosttype` | `3` | 0=hidden, 1=raw IP, 2=hostname, 3=crypted IP, 4=crypted hostname |

**Change the default admin credentials** (`a`/`b`) before exposing the server to others.

### Admin Tool

A Swing-based admin GUI is available:

```sh
# Linux/macOS
./scripts/netadmin

# Windows
scripts\netadmin.bat
```

Connect with the admin username/password from the config. From here you can ban/kick players, delete rooms, and view the leaderboard.

### Files the Server Creates/Loads

```
config/
├── etc/
│   ├── netserver.cfg              # Main config
│   ├── netserver_presets.cfg      # Rated room presets
│   ├── netserver_rulelist.lst     # Available rule sets
│   └── log_server.cfg             # Logging config
└── setting/
    ├── netserver_playerdata.cfg   # Player ratings & data (created at runtime)
    ├── netserver_mpranking.cfg    # Multiplayer leaderboard
    ├── netserver_spranking.cfg    # Single-player leaderboard
    └── ...
```

### Connecting Clients

Players connect through the game's netplay lobby, entering the server's IP/hostname and port (default 9200). Spectators can also connect in observe-only mode.
