# NullpoMino Netplay (P2P Rooms)

Netplay is fully peer-to-peer: there is **no server process**. Every room is
its own P2P session — the players of a room hold direct TCP connections to
each other (a full mesh), game traffic flows directly between them, and one
peer — the *arbiter* — runs the room bookkeeping. If the arbiter leaves, the
lowest-uid survivor takes over automatically and the room (even a game in
progress) continues.

## Playing

1. **NETPLAY** drops you straight into the **LAN lounge**: your nickname and
   team live in the top bar, the table lists every room found on the LAN, and
   the chat box is a lounge-wide LAN chat — everyone sitting on this screen
   sees it, no connection needed.
2. **CREATE** opens the room form (multiplayer, single-player, rule-locked,
   map rooms); OK creates the room and puts you in it. Your room appears on
   friends' lounges within ~2 seconds.
3. **JOIN** enters the selected room; **VIEW** spectates it. Type
   `/join host:port` in chat to reach a room UDP discovery can't (internet
   play — the port is `netroom.port`, default **9202**).
4. Everything in-room — ready/auto-start, garbage, KOs, winner, rematch —
   works as it always has. ESC in-game leaves the room and returns to the
   lounge. Errors and join progress appear as colored lines in the chat.

Internet play: every player of a room must be able to reach every other, so
each forwards their TCP port (`netroom.port` in `config/etc/netroom.cfg`) and
joins via `/join`. Two players behind the same NAT joining a remote room may
fail to connect to each other (no hairpin support); LAN and
one-NAT-per-player setups work. Lounge chat is LAN-only.

## How It Works

### Topology

```
        Alice (uid 0, arbiter)
        /                \
   direct TCP links   direct TCP links
      /                    \
   Bob (uid 1) ---------- Carol (uid 2)
```

- **Control plane** (rooms, seats, ready, seeds, deaths, winners, chat):
  members send their standard client lines to the arbiter, which runs the room
  state machine (`RoomAuthority`, a faithful port of the old server logic over
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
protocol, unchanged. The room protocol adds an envelope (`room\t...` frames, defined in
`RoomProtocol`) for the handshake (`hello`/`welcome`+snapshot/`peerok`),
session traffic (`c`ontrol / `b`roadcast with seq+scope / `d`irect), authority
state extras (`authg`/`authr`), cache dissemination (`snap` frames), liveness
(`ping`/`pong`), and failure handling (`peerdown`/`kick`/`arbiter`). Because
the standard protocol is reused verbatim, the whole client stack — lobby,
screens, every game mode — runs over the room session untouched via the
`NetRoomPlayerClient` seam.

### Joining

A joiner dials the arbiter, which admits it (assigning a uid), replies with
the member roster + a full state snapshot, and announces the newcomer to the
existing members. The joiner then dials every member directly (authenticated
by the session token from the welcome) and reports `roomok`; only then can it
enter rooms. Stale/non-room clients that connect get a graceful deny.

### Arbiter migration

Every peer passively mirrors all authoritative broadcasts plus the auth-extras
frames (`RoomMirror`), so any peer holds the complete session state. When the
arbiter's link dies, each peer independently computes the successor (lowest
surviving uid — deterministic, no election). The successor adopts its mirror,
claims with an `arbiter` frame, re-baselines everyone, and processes the old
arbiter's departure through the normal path — mid-game that emits its death
(correct placement) and, if only one player remains, the round's finish.
Controls lost in flight to the dead arbiter (a `dead`, `racewin`, or `ready`)
are re-sent by their owners after the claim; all are idempotent.

Split links (peer A and B lose their direct link but both still reach the
arbiter) are arbitrated: both report `peerdown`, and after a 2-second window
the arbiter kicks the member with the most broken links.

### LAN discovery + lounge chat

The room's arbiter broadcasts a UDP beacon (port **9201**, every 1.5 s,
5 s TTL) carrying the full room-table row:

```
NullpoLAN\t2\t[tcpPort]\t[nameEnc]\t[verMajor]\tR\t[sessionId]\t[ownerEnc]\t[players]
  \t[roomNameEnc]\t[rated]\t[ruleNameEnc]\t[modeEnc]\t[playing]\t[seated]\t[maxPlayers]\t[spectators]
```

Joiners dial the beacon's source address, so only the arbiter announces —
if it leaves, the promoted successor takes over announcing within one beacon
cycle (the lounge dedupes by sessionId across the handover). A room is only
announced once it actually exists — a half-created room (create form still
open) cannot be joined. Disable with `netroom.lanAnnounce=false`.

Lounge chat lines are one-shot broadcasts on the same port (type `C` with a
random msgId): every lounge on the LAN shows them, duplicates from
multi-interface broadcasts are suppressed, and senders echo locally so their
own line shows exactly once. Best-effort by design: no history, no delivery
guarantee, LAN only.

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

## The Web Build (WebRTC)

The browser (TeaVM) build runs the exact same room engine - `RoomSession`,
`RoomAuthority`, the wire protocol, migration - over different plumbing,
selected via the `RoomNet`/`LoungeService` seams the entry point installs:

| Piece | Desktop | Web |
|---|---|---|
| Peer links | TCP sockets (`TcpRoomTransport`/`RoomPeerLink`) | WebRTC DataChannels, ordered+reliable (`RtcRoomTransport`/`RtcPeerLink`) |
| Peer address | IP + port | Signaling clientId (16 hex, per page load) |
| Event dispatch | Dispatcher thread + blocking queue (`ThreadRoomDispatcher`) | Trampoline on the JS event loop (`WebRoomDispatcher`) |
| Discovery + lounge | UDP broadcast beacons (`NetLanDiscovery`) | Public MQTT-over-WSS brokers, same packet codec (`MqttLoungeService`) |
| Signaling | none needed (direct dial) | Non-trickle SDP offer/answer over per-peer broker inbox topics |
| NAT traversal | port forwarding | STUN (Google + Cloudflare; no TURN - symmetric-NAT pairs cannot connect) |

The brokers (default `broker.emqx.io` + `test.mosquitto.org`, bonded: connect
to all, publish to all, dedupe inbound) carry only the lounge and the
offer/answer handshake. Game traffic flows purely peer-to-peer; a broker
dying mid-game affects nothing but discovery of new rooms, and arbiter
migration works with every broker down. The lounge is public to anyone on
the same brokers and topic root - the LAN trust model, internet-wide.
Topics are versioned (`npp/v<major>/...`) so incompatible clients never meet.

Web-only code lives in `src/main/java/nullpomino/gui/teavm/net/`; the
shared codecs (`game/net/mqtt/MqttCodec`, `game/net/web/WebSignaling`) are
pure Java with JVM unit tests.

## Configuration

`config/etc/netroom.cfg`:

| Setting | Default | Notes |
|---|---|---|
| `netroom.port` | `9202` | Room TCP listen port (0 = ephemeral; busy port falls back to ephemeral) |
| `netroom.lanAnnounce` | `true` | Announce to the lounge while joinable (UDP beacon / MQTT topic) |

`config/etc/netweb.cfg` (web build only, re-fetched on every page load):

| Setting | Default | Notes |
|---|---|---|
| `netweb.mqtt.brokers` | emqx + mosquitto | MQTT-over-WSS broker URLs, space-separated, bonded |
| `netweb.mqtt.topicRoot` | `npp` | Topic prefix; protocol major version is appended |
| `netweb.stun` | Google + Cloudflare | ICE servers; `turn:` URLs can be added later |
| `netweb.iceGatherTimeout` | `3000` | Cap on ICE gathering before the SDP ships anyway |

## Key Files

| Piece | Path |
|---|---|
| Frame formats + constants | `src/main/java/nullpomino/game/net/room/RoomProtocol.java` |
| TCP links + listener | `src/main/java/nullpomino/game/net/room/RoomPeerLink.java`, `RoomTransport.java` |
| Session dispatcher (handshake, routing, scoping, migration) | `src/main/java/nullpomino/game/net/room/RoomSession.java` |
| Arbiter room state machine | `src/main/java/nullpomino/game/net/room/RoomAuthority.java` |
| Passive state replica | `src/main/java/nullpomino/game/net/room/RoomMirror.java` |
| Local ratings/records | `src/main/java/nullpomino/game/net/room/RoomLocalRecords.java`, `RoomRating.java` |
| Client seam | `src/main/java/nullpomino/game/net/NetRoomPlayerClient.java` |
| LAN beacons + lounge chat | `src/main/java/nullpomino/game/net/NetLanDiscovery.java` |
| The LAN lounge screen | `src/main/java/nullpomino/gui/sdl/StateNetLobbySDL.java` |
| Lobby session object | `src/main/java/nullpomino/gui/net/NetLobbyFrame.java` |

The threading model is deliberately simple: one reader + one writer thread per
link, and a single dispatcher per session that owns all state (a thread on
desktop, a run-to-completion trampoline in the browser). Per-peer FIFO
ordering holds end-to-end - TCP per link on desktop, an ordered reliable
DataChannel per link on web - which is what the client's strict message
ordering (roster before `roomjoinsuccess`, all `dead` before `finish`)
depends on.
