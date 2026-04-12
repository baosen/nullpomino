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
- **`NetLobbyFrame`** — Swing-based lobby UI for browsing rooms, chatting, creating/joining rooms, and toggling ready status.
- **`StateNetGame`** — Slick2D game state that bridges the lobby with the game engine.

### Game Synchronization

This is **distributed state sync**, not lockstep or rollback. Each player runs their own game engine independently and broadcasts state changes:

| What's sent | Method | When |
|---|---|---|
| Current piece (id, x, y, rotation, color) | `netSendPieceMovement()` | Every frame the piece moves |
| Field state (compressed grid) | `netSendField()` | When blocks are placed/cleared |
| Next queue + hold piece | `netSendNextAndHold()` | When the queue changes |
| Stats (score, lines, etc.) | `netSendStats()` | Periodically |

The server relays these to all room members. Each client reconstructs other players' boards from the received data and renders them.

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
| Net utilities | `src/main/java/mu/nu/nullpo/game/net/NetUtil.java` |
| Base netplay mode | `src/main/java/mu/nu/nullpo/game/net/NetDummyMode.java` |
| VS mode base | `src/main/java/mu/nu/nullpo/game/net/NetDummyVSMode.java` |
| Lobby UI | `src/main/java/mu/nu/nullpo/gui/net/NetLobbyFrame.java` |

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
