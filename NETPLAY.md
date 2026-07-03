# NullpoMino Netplay (P2P Mesh)

Netplay is fully peer-to-peer: there is **no server process**. Every peer of a
session holds a direct TCP connection to every other peer (a full mesh), game
traffic flows directly between players, and one peer — the *arbiter* — runs the
lobby bookkeeping. If the arbiter leaves, the lowest-uid survivor takes over
automatically and the session (even a game in progress) continues.

## Playing

1. **NETPLAY** → the session screen. Enter a nickname (and optionally a team).
2. **CREATE** starts a new session — you become its first arbiter and land in
   the lobby. Friends on your LAN see the session appear within ~2 seconds.
3. **JOIN** connects to a session discovered on the LAN (green rows).
   **DIRECT** joins by hand-typed `host:port` when discovery can't reach the
   session (different network, firewalled UDP). The address to give friends is
   shown in the lobby header.
4. The lobby is the familiar multi-room browser: create rooms (multiplayer,
   single-player, rule-locked, map rooms), join, spectate, chat. Everything
   in-room — ready/auto-start, garbage, KOs, winner, rematch — works as it
   always has. ESC in-game returns to the lobby.

Internet play: every peer must be able to reach every other peer, so each
player forwards their mesh TCP port (default **9202**, `netmesh.port` in
`config/etc/netmesh.cfg`) and joins via DIRECT. Two players behind the same
NAT joining a remote session may fail to connect to each other (no hairpin
support); LAN and one-NAT-per-player setups work.

## How It Works

### Topology

```
        Alice (uid 0, arbiter)
        /                \
   TCP mesh links     TCP mesh links
      /                    \
   Bob (uid 1) ---------- Carol (uid 2)
```

- **Control plane** (rooms, seats, ready, seeds, deaths, winners, chat):
  members send their standard client lines to the arbiter, which runs the room
  state machine (`MeshAuthority`, a faithful port of the old server logic over
  the same `NetRoomInfo`/`NetPlayerInfo` classes) and emits authoritative
  broadcasts to everyone.
- **Game plane** (`game\t...`/`gstat\t...` piece/field/attack traffic): the
  sender stamps its own uid/seat and sends **directly** to same-room peers.
  The arbiter never sees or relays it — no relay hop, no added latency.
- **Room scoping**: arbiter broadcasts carry a room scope; each peer's session
  layer delivers room-scoped lines (start/dead/finish/chat/…) to its local
  client only when it is in that room, so parallel rooms can't interfere.

### Wire protocol

All lines are tab-delimited, newline-terminated UTF-8 — the original NullpoMino
protocol, unchanged. The mesh adds an envelope (`mesh\t...` frames, defined in
`MeshProtocol`) for the handshake (`hello`/`welcome`+snapshot/`peerok`),
session traffic (`c`ontrol / `b`roadcast with seq+scope / `d`irect), authority
state extras (`authg`/`authr`), cache dissemination (`snap` frames), liveness
(`ping`/`pong`), and failure handling (`peerdown`/`kick`/`arbiter`). Because
the standard protocol is reused verbatim, the whole client stack — lobby,
screens, every game mode — runs over the mesh untouched via the
`NetMeshPlayerClient` seam.

### Joining

A joiner dials the arbiter, which admits it (assigning a uid), replies with
the member roster + a full state snapshot, and announces the newcomer to the
existing members. The joiner then dials every member directly (authenticated
by the session token from the welcome) and reports `meshok`; only then can it
enter rooms. Stale/non-mesh clients that connect get a graceful deny.

### Arbiter migration

Every peer passively mirrors all authoritative broadcasts plus the auth-extras
frames (`MeshMirror`), so any peer holds the complete session state. When the
arbiter's link dies, each peer independently computes the successor (lowest
surviving uid — deterministic, no election). The successor adopts its mirror,
claims with an `arbiter` frame, re-baselines everyone, and processes the old
arbiter's departure through the normal path — mid-game that emits its death
(correct placement) and, if only one player remains, the round's finish.
Controls lost in flight to the dead arbiter (a `dead`, `racewin`, or `ready`)
are re-sent by their owners after the claim; all are idempotent.

Split meshes (peer A and B lose their direct link but both still reach the
arbiter) are arbitrated: both report `peerdown`, and after a 2-second window
the arbiter kicks the member with the most broken links.

### LAN discovery

Every peer of a joinable session broadcasts a UDP beacon (port **9201**, every
1.5 s, 5 s TTL):

```
NullpoLAN\t2\t[tcpPort]\t[nameEnc]\t[verMajor]\tM\t[sessionId]\t[lobbyNameEnc]\t[players]
```

The session screen dedupes beacons by sessionId (any surviving peer keeps the
session discoverable). Disable with `netmesh.lanAnnounce=false`.

### Local records (no server accounts)

Server-side persistence became per-player local files under `config/setting/`:

- `netplay_mydata.cfg` — your per-style ELO rating, play/win counts, and
  single-player personal bests. Sent with your join handshake so opponents'
  clients can display it and the arbiter can compute rated results.
- `netplay_mpranking.cfg` — your local multiplayer leaderboard, built from the
  rated games you witnessed. The RANKING screen reads it.
- `netplay_spranking.cfg` — local all-time single-player netplay rankings.

Rated rooms use the same ELO math the server used (the arbiter computes deltas
and broadcasts `rating` lines; each peer persists only its own). This is
honor-system by construction — ratings are self-reported baselines and every
peer keeps its own view. There are no rated presets, bans, observers, or admin
tools; daily SP rankings don't exist locally.

### Trust model

Unchanged from the original design: each client simulates only its own board
and broadcasts state; attack strength and KO attribution are computed by the
attacking/dying client; race-mode placements are declared by the winner. The
arbiter validates seats and sequencing, not gameplay.

## Configuration

`config/etc/netmesh.cfg`:

| Setting | Default | Notes |
|---|---|---|
| `netmesh.port` | `9202` | Mesh TCP listen port (0 = ephemeral; busy port falls back to ephemeral) |
| `netmesh.lanAnnounce` | `true` | Broadcast the UDP 9201 beacon while joinable |

## Key Files

| Piece | Path |
|---|---|
| Frame formats + constants | `src/main/java/nullpomino/game/net/mesh/MeshProtocol.java` |
| TCP links + listener | `src/main/java/nullpomino/game/net/mesh/MeshPeerLink.java`, `MeshTransport.java` |
| Session dispatcher (handshake, routing, scoping, migration) | `src/main/java/nullpomino/game/net/mesh/MeshSession.java` |
| Arbiter room state machine | `src/main/java/nullpomino/game/net/mesh/MeshAuthority.java` |
| Passive state replica | `src/main/java/nullpomino/game/net/mesh/MeshMirror.java` |
| Local ratings/records | `src/main/java/nullpomino/game/net/mesh/MeshLocalRecords.java`, `MeshRating.java` |
| Client seam | `src/main/java/nullpomino/game/net/NetMeshPlayerClient.java` |
| LAN beacons | `src/main/java/nullpomino/game/net/NetLanDiscovery.java` |
| Session screen | `src/main/java/nullpomino/gui/sdl/StateNetServerSelectSDL.java` |
| Lobby session object | `src/main/java/nullpomino/gui/net/NetLobbyFrame.java` |

The threading model is deliberately simple: one reader + one writer thread per
link, and a single dispatcher thread per session that owns all state. Per-peer
FIFO ordering holds end-to-end, which is what the client's strict message
ordering (roster before `roomjoinsuccess`, all `dead` before `finish`)
depends on.
