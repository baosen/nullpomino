// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nullpomino.game.net.NetLanDiscovery;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Deterministic branch-level tests for {@link RoomSession} built on an
 * in-memory {@link RoomNet} (synchronous dispatcher, recorded transport,
 * no sockets, no timers). Every scenario drives the session on the test
 * thread by feeding protocol frames and events directly, which reaches the
 * error paths and edge branches the wire-level tests cannot hit reliably.
 */
class RoomSessionBranchGapTest {

    private static final float VER = GameManager.getVersionMajor();
    private static final boolean DEV = GameManager.isDevBuild();
    private static final String TOKEN = "cafecafecafecafecafecafecafecafe";

    // ---------------------------------------------------------------- fakes

    /** Records everything the session writes; never touches a socket */
    static final class FakeLink extends RoomLink {
        final List<String> sent = new ArrayList<String>();
        String closeReason;
        boolean flushClose;
        private boolean closed;
        private final String remote;

        FakeLink(String remote) {
            this.remote = remote;
        }

        @Override public void sendLine(String line) { if (!closed) sent.add(line); }
        @Override public void close(String reason) {
            if (!closed) { closed = true; closeReason = reason; }
        }
        @Override public void closeAfterFlush(String reason) {
            if (!closed) { closed = true; closeReason = reason; flushClose = true; }
        }
        @Override public boolean isClosed() { return closed; }
        @Override public String getRemoteAddress() { return remote; }

        boolean sentPrefix(String prefix) {
            for (String s : sent) if (s.startsWith(prefix)) return true;
            return false;
        }
    }

    static final class Dial {
        final String host;
        final int port;
        final RoomTransport.DialCallback callback;
        Dial(String host, int port, RoomTransport.DialCallback callback) {
            this.host = host; this.port = port; this.callback = callback;
        }
    }

    static final class FakeTransport implements RoomTransport {
        final List<Dial> dials = new ArrayList<Dial>();
        @Override public int startListening(int configuredPort) { return 9200; }
        @Override public void dial(String host, int port, int timeoutMs, DialCallback callback) {
            dials.add(new Dial(host, port, callback));
        }
        @Override public int getListenPort() { return 9200; }
        @Override public String getDisplayAddress() { return "127.0.0.1"; }
        @Override public void shutdown() {}
    }

    /** FIFO but fully synchronous: post() drains on the calling thread */
    static final class SyncDispatcher implements RoomDispatcher {
        private EventHandler handler;
        private final ArrayDeque<RoomEvent> queue = new ArrayDeque<RoomEvent>();
        private boolean draining;
        private boolean shut;

        @Override public void start(EventHandler h) { handler = h; }
        @Override public void post(RoomEvent event) {
            if (shut || handler == null) return;
            queue.add(event);
            if (draining) return;
            draining = true;
            try {
                while (!queue.isEmpty() && !shut) handler.handle(queue.poll());
            } finally {
                draining = false;
            }
        }
        @Override public void startTicker(int periodMs) {}
        @Override public void shutdown() { shut = true; }
    }

    static final class FakeBeacon implements RoomBeacon {
        int starts;
        int stops;
        @Override public void start(Supplier supplier) { starts++; }
        @Override public void stop() { stops++; }
    }

    static final class FakeNet implements RoomNet {
        final FakeTransport transport = new FakeTransport();
        final SyncDispatcher dispatcher = new SyncDispatcher();
        final FakeBeacon beacon = new FakeBeacon();
        @Override public RoomTransport createTransport(RoomEventSink sink) { return transport; }
        @Override public RoomDispatcher createDispatcher() { return dispatcher; }
        @Override public RoomBeacon createBeacon() { return beacon; }
    }

    static final class RecListener implements RoomSession.Listener {
        final List<String> states = new ArrayList<String>();
        final List<String> arbiterChanges = new ArrayList<String>();
        @Override public void onSessionState(RoomSession.State state, String detail) {
            states.add(state.toString());
        }
        @Override public void onArbiterChanged(int arbiterUid, boolean localIsArbiter) {
            arbiterChanges.add(arbiterUid + ":" + localIsArbiter);
        }
    }

    /** One fake-net session plus its recorded outputs */
    final class Harness {
        final FakeNet net = new FakeNet();
        final RecListener listener = new RecListener();
        final List<String> client = new ArrayList<String>();
        final List<String> closed = new ArrayList<String>();
        RoomSession session;
        FakeLink arb;   // joiner's link to the arbiter

        void wire() {
            session.setLineListener(line -> client.add(line));
            session.setClosedListener(reason -> closed.add(reason));
            sessions.add(session);
        }

        void line(RoomLink link, String frame) { session.onLine(link, frame); }
        void tick() { session.processOneEvent(RoomEvent.tick()); }

        boolean clientHasPrefix(String prefix) {
            for (String s : client) if (s.startsWith(prefix)) return true;
            return false;
        }
    }

    private final List<RoomSession> sessions = new ArrayList<RoomSession>();

    @AfterEach
    void tearDown() {
        for (RoomSession s : sessions) s.shutdown();
    }

    private static RoomConfig config() {
        RoomConfig config = new RoomConfig();
        config.listenPort = 0;
        config.lanAnnounce = true;   // safe: FakeBeacon never opens a socket
        return config;
    }

    private Harness arbiter() throws Exception {
        Harness h = new Harness();
        h.session = RoomSession.create("Alice", config(), h.listener, h.net);
        h.wire();
        return h;
    }

    /** A join() stub: dialing recorded but no callback fired yet (CONNECTING) */
    private Harness joinStub() throws Exception {
        Harness h = new Harness();
        h.session = RoomSession.join("127.0.0.1", 9200, "Joiner", config(), h.listener, h.net);
        h.wire();
        return h;
    }

    /** Fire the arbiter dial callback: the joiner sends its hello join */
    private FakeLink dialArbiter(Harness h) {
        Dial d = h.net.transport.dials.remove(0);
        FakeLink link = new FakeLink("10.0.0.1");
        d.callback.onDialed(link);
        h.arb = link;
        return link;
    }

    /** Feed a welcome for uid 'selfUid' under arbiter uid 0 */
    private void feedWelcome(Harness h, int selfUid, RoomProtocol.RosterEntry... entries) {
        h.line(h.arb, RoomProtocol.buildWelcome(TOKEN, selfUid, "Joiner", 0, 5,
                Arrays.asList(entries)));
    }

    /** Joiner in READY state with the given already-linked peers (uid -> link) */
    private Harness readyJoiner(int selfUid, int... peerUids) throws Exception {
        Harness h = joinStub();
        dialArbiter(h);
        RoomProtocol.RosterEntry[] entries = new RoomProtocol.RosterEntry[peerUids.length];
        for (int i = 0; i < peerUids.length; i++) {
            entries[i] = new RoomProtocol.RosterEntry(peerUids[i], "10.0.0." + (50 + i),
                    7050 + i, "Peer" + peerUids[i]);
        }
        feedWelcome(h, selfUid, entries);
        h.line(h.arb, RoomProtocol.LINE_SNAPEND);
        for (int i = 0; i < peerUids.length; i++) {
            Dial d = h.net.transport.dials.remove(0);
            FakeLink link = new FakeLink("10.0.0." + (50 + i));
            d.callback.onDialed(link);
            peerLinks.put(peerUids[i], link);
            h.line(link, RoomProtocol.buildPeerOk(peerUids[i]));
        }
        assertEquals(RoomSession.State.READY, h.session.getState());
        return h;
    }

    private final Map<Integer, FakeLink> peerLinks = new java.util.HashMap<Integer, FakeLink>();

    private static String helloJoin(String name, int port) {
        return RoomProtocol.buildHelloJoin(VER, DEV, port, name, new int[0], new int[0], new int[0]);
    }

    /** Admit a member into an arbiter session over a fake link */
    private FakeLink admitMember(Harness h, String name, int port) {
        FakeLink link = new FakeLink("10.0.0." + port % 100);
        h.session.onLinkAccepted(link);
        h.line(link, helloJoin(name, port));
        assertTrue(link.sentPrefix("room\twelcome\t"), "hello join must be welcomed");
        return link;
    }

    private static String roomCreateLine(String name, int maxPlayers) {
        NetRoomInfo template = new NetRoomInfo();
        template.maxPlayers = maxPlayers;
        template.strMode = "NET-VS-BATTLE";
        return "roomcreate\t" + NetUtil.urlEncode(name) + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode("NET-VS-BATTLE");
    }

    private static String playerBlob(int uid, String name, int roomID, int seatID, boolean ready) {
        NetPlayerInfo p = new NetPlayerInfo();
        p.uid = uid;
        p.strName = name;
        p.roomID = roomID;
        p.seatID = seatID;
        p.ready = ready;
        return p.exportString();
    }

    // ---------------------------------------------------------------- reflection helpers

    private static Object getField(RoomSession session, String name) throws Exception {
        Field f = RoomSession.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(session);
    }

    private static void setField(RoomSession session, String name, Object value) throws Exception {
        Field f = RoomSession.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(session, value);
    }

    private static void invoke(RoomSession session, String method) throws Exception {
        Method m = RoomSession.class.getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(session);
    }

    // ================================================================ frame plumbing edges

    @Test
    void pingPongDenyAndMalformedFramesAreHandled() throws Exception {
        Harness h = arbiter();
        FakeLink lx = new FakeLink("10.0.0.9");

        h.line(lx, RoomProtocol.LINE_PING);
        assertEquals(RoomProtocol.LINE_PONG, lx.sent.get(lx.sent.size() - 1));
        h.line(lx, RoomProtocol.LINE_PONG);              // silently accepted
        h.line(lx, "room\tmystery\tframe");              // unhandled type, logged only
        h.line(lx, "room\tauthg\tbad\tbad");             // malformed authg dropped
        h.line(lx, "room\tauthr\tonly");                 // malformed authr dropped
        h.line(lx, "room\tpeer\tbad");                   // malformed peer announce dropped
        h.line(lx, "room\troomok");                      // roomok from an unbound link
        h.line(lx, "room\tpeerok\t99");                  // peerok for an unknown uid while READY
        assertTrue(h.session.isOpen(), "None of the malformed frames may kill the session");

        // 2-field deny on a non-arbiter link: reason defaults, the link is closed
        h.line(lx, "room\tdeny");
        assertEquals("denied", lx.closeReason);

        // severLinkForTest edge cases: unknown uid, and own entry (no link)
        h.session.severLinkForTest(99);
        h.session.severLinkForTest(0);
        assertTrue(h.session.isOpen());

        // Empty client line is ignored; a closed session drops sends
        h.session.sendLine("");
        h.session.sendLine("\n");
        assertTrue(h.client.isEmpty());

        // Shutdown twice: the second call is a guarded no-op, closed fires once
        h.session.processOneEvent(RoomEvent.shutdown("first"));
        h.session.processOneEvent(RoomEvent.shutdown("again"));
        assertEquals(List.of("first"), h.closed);
        h.session.sendLine("late line after close");
        assertFalse(h.session.isOpen());
    }

    @Test
    void helloRejectionsAtTheArbiter() throws Exception {
        Harness h = arbiter();

        FakeLink malformed = new FakeLink("10.0.1.1");
        h.line(malformed, "room\thello\tjoin\tnotafloat\tfalse\t123\tName");
        assertEquals("malformed hello", malformed.closeReason);

        FakeLink wrongVer = new FakeLink("10.0.1.2");
        h.line(wrongVer, RoomProtocol.buildHelloJoin(VER + 1.0f, DEV, 7002, "Old",
                new int[0], new int[0], new int[0]));
        assertTrue(wrongVer.sentPrefix("room\tdeny\t" + RoomProtocol.DENY_DIFFERENT_VERSION));
        assertTrue(wrongVer.flushClose);

        FakeLink wrongBuild = new FakeLink("10.0.1.3");
        h.line(wrongBuild, RoomProtocol.buildHelloJoin(VER, !DEV, 7003, "Dev",
                new int[0], new int[0], new int[0]));
        assertTrue(wrongBuild.sentPrefix("room\tdeny\t" + RoomProtocol.DENY_DIFFERENT_BUILD));

        FakeLink badToken = new FakeLink("10.0.1.4");
        h.line(badToken, RoomProtocol.buildHelloPeer(VER, DEV, "wrongtoken", 5, 7004));
        assertTrue(badToken.sentPrefix("room\tdeny\t" + RoomProtocol.DENY_BAD_TOKEN));
        assertEquals("bad token", badToken.closeReason);
    }

    @Test
    void helloJoinAtANonArbiterIsDenied() throws Exception {
        Harness h = readyJoiner(2);
        FakeLink joiner = new FakeLink("10.0.2.1");
        h.line(joiner, helloJoin("Late", 7005));
        assertTrue(joiner.sentPrefix("room\tdeny\t" + RoomProtocol.DENY_BAD_TOKEN));
        assertEquals("join at non-arbiter", joiner.closeReason);
    }

    // ================================================================ snapshot content

    @Test
    void snapshotCarriesBlobsAndChatHistory() throws Exception {
        Harness h = arbiter();
        h.session.sendLine(roomCreateLine("Snappy", 4));
        assertTrue(h.clientHasPrefix("roomcreatesuccess\t"));

        // Blobs live in the mirror; inject them the way cache frames arrive
        FakeLink feeder = new FakeLink("10.0.3.1");
        h.line(feeder, RoomProtocol.buildSnapRoomRule(0, "RULEBLOB"));
        h.line(feeder, RoomProtocol.buildSnapMap(0, "MAPBLOB"));

        // Chat history lives in the authority
        h.session.sendLine("chat\t" + NetUtil.urlEncode("hello room"));
        h.session.sendLine("lobbychat\t" + NetUtil.urlEncode("hello lobby"));

        FakeLink joiner = admitMember(h, "Bob", 7010);
        assertTrue(joiner.sent.contains("room\tsnap\troomrule\t0\tRULEBLOB"));
        assertTrue(joiner.sent.contains("room\tsnap\tmap\t0\tMAPBLOB"));
        assertTrue(joiner.sentPrefix("room\tsnap\tchat\t0\t"));
        assertTrue(joiner.sentPrefix("room\tsnap\tlobbychat\t"));
    }

    // ================================================================ control frame guards

    @Test
    void controlFrameGuardsAtTheArbiter() throws Exception {
        Harness h = arbiter();
        h.session.sendLine(roomCreateLine("Gate", 4));

        // Unwrappable control frame (no payload)
        FakeLink unbound = new FakeLink("10.0.4.1");
        h.line(unbound, "room\tc");
        // Well-formed control from a link that never handshook (uid -1)
        h.line(unbound, RoomProtocol.wrapControl("chat\thi"));

        // A control from a uid absent from the roster still reaches the authority
        FakeLink stray = new FakeLink("10.0.4.2");
        stray.uid = 77;
        h.line(stray, RoomProtocol.wrapControl("chat\thi"));
        assertTrue(h.session.isOpen());

        // Half-joined member (no roomok yet) may not enter rooms...
        FakeLink member = admitMember(h, "Bob", 7020);
        h.line(member, RoomProtocol.wrapControl("roomjoin\t0\tfalse"));
        assertFalse(member.sentPrefix("room\td\troomjoinsuccess"),
                "roomjoin must be blocked before roomok");
        // ...but may after
        h.line(member, RoomProtocol.LINE_ROOMOK);
        h.line(member, RoomProtocol.wrapControl("roomjoin\t0\tfalse"));
        assertTrue(member.sentPrefix("room\td\troomjoinsuccess"));
    }

    @Test
    void directRepliesToUnreachableMembersAreDropped() throws Exception {
        Harness h = arbiter();
        FakeLink member = admitMember(h, "Bob", 7030);
        RoomRoster roster = (RoomRoster) getField(h.session, "roster");

        // Link lost but entry still present: the direct reply is skipped
        roster.get(1).link = null;
        h.line(member, RoomProtocol.wrapControl("ruleget\t0"));
        assertFalse(member.sentPrefix("room\td\trulegetsuccess"));

        // Entry gone entirely: also skipped
        roster.remove(1);
        h.line(member, RoomProtocol.wrapControl("ruleget\t0"));
        assertFalse(member.sentPrefix("room\td\trulegetsuccess"));
    }

    // ================================================================ beacon gating

    @Test
    void beaconStartsOnceAndReflectsRuleLockAndNameFallback() throws Exception {
        Harness h = arbiter();
        assertEquals(1, h.net.beacon.starts, "create() starts the announcer");
        invoke(h.session, "startAnnouncer");
        assertEquals(1, h.net.beacon.starts, "second start is a no-op");

        h.session.sendLine(roomCreateLine("Beacon", 4));
        h.tick();
        NetLanDiscovery.Announce a = h.session.getBeaconSnapshotForTest();
        assertNotNull(a);
        assertEquals("", a.ruleName, "unlocked rooms announce no rule");

        RoomMirror mirror = (RoomMirror) getField(h.session, "mirror");
        mirror.getRoom().ruleLock = true;
        mirror.getRoom().ruleName = "CUSTOM";
        mirror.getPlayer(0).strName = "Renamed";
        h.tick();
        a = h.session.getBeaconSnapshotForTest();
        assertEquals("CUSTOM", a.ruleName);
        assertEquals("Renamed", a.playerName, "names come from the mirror");

        // No mirror self: falls back to the construction-time name
        setField(h.session, "localUid", 999);
        h.tick();
        assertEquals("Alice", h.session.getBeaconSnapshotForTest().playerName);
        setField(h.session, "localUid", 0);
    }

    // ================================================================ local sends

    @Test
    void localRecordRequestsAndSpSendStates() throws Exception {
        Harness h = arbiter();

        h.session.sendLine("ping");
        assertTrue(h.client.contains("pong"));

        // In the lobby: self exists but is in no room, spsend is dropped
        h.session.sendLine("spsend\t1\tX");
        assertFalse(h.clientHasPrefix("spsend"));

        // game in the lobby: no fan-out (roomID -1)
        h.session.sendLine("game\tearly");

        h.session.sendLine(roomCreateLine("Records", 2));
        assertTrue(h.clientHasPrefix("roomcreatesuccess\t"));

        // Seated: a bad checksum answers spsendng without touching disk
        h.session.sendLine("spsend\t123\tCORRUPT");
        assertTrue(h.client.contains("spsendng"));

        h.session.sendLine("mpranking\t0");
        assertTrue(h.clientHasPrefix("mpranking\t0\t"));

        h.session.sendLine("spranking\t" + NetUtil.urlEncode("any") + "\t"
                + NetUtil.urlEncode("MODE") + "\t0\tfalse");
        assertTrue(h.clientHasPrefix("spranking\t"));

        // racewin is tracked as resendable volatile state
        h.session.sendLine("racewin\t1\t2");
        assertEquals("racewin\t1\t2", getField(h.session, "pendingRacewinControl"));
    }

    @Test
    void gameFanOutSkipsOtherRoomsAndUnmirroredPeers() throws Exception {
        Harness h = arbiter();
        h.session.sendLine(roomCreateLine("Fan", 4));

        // Bob joins the session and the room
        FakeLink bob = admitMember(h, "Bob", 7040);
        h.line(bob, RoomProtocol.LINE_ROOMOK);
        h.line(bob, RoomProtocol.wrapControl("roomjoin\t0\tfalse"));
        assertTrue(bob.sentPrefix("room\td\troomjoinsuccess"));

        // Carol joins the session but stays in the lobby
        FakeLink carol = admitMember(h, "Carol", 7041);
        h.line(carol, RoomProtocol.LINE_ROOMOK);

        // A peer known at transport level but absent from the mirror
        FakeLink ghost = new FakeLink("10.0.5.3");
        h.line(ghost, RoomProtocol.buildHelloPeer(VER, DEV, h.session.getSessionId(), 33, 7333));
        assertTrue(ghost.sentPrefix("room\tpeerok\t"));

        int bobLines = bob.sent.size();
        int ghostLines = ghost.sent.size();
        h.session.sendLine("game\tpiece\t1");
        assertEquals("game\t0\t0\tpiece\t1", bob.sent.get(bob.sent.size() - 1));
        assertEquals(bobLines + 1, bob.sent.size());
        assertEquals(ghostLines, ghost.sent.size(), "unmirrored peers get no game lines");

        // gstat loops back into the local client
        h.session.sendLine("gstat\t42");
        assertTrue(h.client.contains("gstat\t0\t0\t" + NetUtil.urlEncode("Alice") + "\t42"));

        // Inbound peer lines: malformed, unknown sender, other-room sender, valid gstat
        h.line(bob, "game\tx");                          // too short
        h.line(bob, "game\tzz\t0\tx");                   // non-numeric uid
        h.line(bob, "game\t99\t0\tx");                   // unknown sender
        h.line(carol, "game\t2\t0\tx");                  // sender still in the lobby
        assertFalse(h.clientHasPrefix("game\t"));
        h.line(bob, "gstat\t1\t1\t" + NetUtil.urlEncode("Bob") + "\tdata");
        assertTrue(h.client.contains("gstat\t1\t1\t" + NetUtil.urlEncode("Bob") + "\tdata"));
    }

    @Test
    void watcherSeatAndSelfExcludedBroadcasts() throws Exception {
        Harness h = arbiter();
        h.session.sendLine(roomCreateLine("Watch", 4));
        FakeLink bob = admitMember(h, "Bob", 7045);
        h.line(bob, RoomProtocol.LINE_ROOMOK);
        h.line(bob, RoomProtocol.wrapControl("roomjoin\t0\tfalse"));

        // Re-enter the own room as a watcher: playerleave excludes the local
        // client (the broadcast's exceptUid is our own uid)
        h.session.sendLine("roomjoin\t0\ttrue");
        assertTrue(h.clientHasPrefix("roomjoinsuccess\t0\t-1"));
        assertFalse(h.clientHasPrefix("playerleave\t"), "own playerleave is excluded");
        assertTrue(bob.sentPrefix("room\tb\t"), "Bob still receives the broadcast");

        // Watching: seatID is -1, so game fan-out and spsend are dropped
        int bobLines = bob.sent.size();
        h.session.sendLine("game\tzzz");
        h.session.sendLine("spsend\t1\tX");
        assertEquals(bobLines, bob.sent.size());
        assertFalse(h.client.contains("spsendng"));
    }

    // ================================================================ joiner handshake

    @Test
    void malformedWelcomeAbortsTheJoin() throws Exception {
        Harness h = joinStub();
        dialArbiter(h);
        h.line(h.arb, "room\twelcome\tbad");
        assertEquals(List.of("JOIN_FAILED:malformed welcome"), h.closed);
    }

    @Test
    void welcomeRosterSelfEntryAndPreLinkedPeer() throws Exception {
        Harness h = joinStub();
        dialArbiter(h);

        // The client attaches while still connecting: no synthesized welcome yet
        h.session.clientReady();
        assertFalse(h.clientHasPrefix("welcome\t"));

        // Roster echoes our own uid and carries the arbiter's row
        feedWelcome(h, 2,
                new RoomProtocol.RosterEntry(2, "127.0.0.1", 9200, "Joiner"),
                new RoomProtocol.RosterEntry(0, "10.0.0.1", 7000, "Arb"),
                new RoomProtocol.RosterEntry(3, "10.0.0.3", 7003, "Cee"));
        assertEquals(RoomSession.State.JOINING, h.session.getState());

        // Local sends other than clientready are dropped while joining
        h.session.sendLine("chat\ttoo early");

        // Peer 3 dials us before snapend: its entry is already linked
        FakeLink l3 = new FakeLink("10.0.0.3");
        h.session.onLinkAccepted(l3);
        h.line(l3, RoomProtocol.buildHelloPeer(VER, DEV, TOKEN, 3, 7003));
        assertTrue(l3.sentPrefix("room\tpeerok\t2"));

        // Snapend: nothing left to dial, join completes, welcome reaches the client
        h.line(h.arb, RoomProtocol.LINE_SNAPEND);
        assertEquals(RoomSession.State.READY, h.session.getState());
        assertTrue(h.arb.sent.contains(RoomProtocol.LINE_ROOMOK));
        assertTrue(h.clientHasPrefix("welcome\t"), "queued clientready is honored on join");
        assertTrue(h.net.transport.dials.isEmpty(), "linked peers are not re-dialed");
    }

    @Test
    void peerOkBookkeepingAcrossTheJoin() throws Exception {
        Harness h = joinStub();
        dialArbiter(h);
        feedWelcome(h, 2,
                new RoomProtocol.RosterEntry(3, "10.0.0.3", 7003, "Cee"),
                new RoomProtocol.RosterEntry(4, "10.0.0.4", 7004, "Dee"));

        // peerok before snapend: nothing awaited yet, and uid 99 has no entry
        h.line(h.arb, "room\tpeerok\t99");
        assertEquals(RoomSession.State.JOINING, h.session.getState());

        h.line(h.arb, RoomProtocol.LINE_SNAPEND);
        assertEquals(2, h.net.transport.dials.size());

        FakeLink l3 = new FakeLink("10.0.0.3");
        h.net.transport.dials.get(0).callback.onDialed(l3);
        FakeLink l4 = new FakeLink("10.0.0.4");
        h.net.transport.dials.get(1).callback.onDialed(l4);
        assertTrue(l3.sentPrefix("room\thello\tpeer\t"));

        h.line(l3, RoomProtocol.buildPeerOk(3));
        assertEquals(RoomSession.State.JOINING, h.session.getState(), "one peer still pending");
        h.line(l4, RoomProtocol.buildPeerOk(4));
        assertEquals(RoomSession.State.READY, h.session.getState());
    }

    @Test
    void completeJoinToleratesAMissingArbiterLink() throws Exception {
        Harness h = joinStub();
        FakeLink arb = dialArbiter(h);
        feedWelcome(h, 2);
        setField(h.session, "arbiterLink", null);
        h.line(arb, RoomProtocol.LINE_SNAPEND);
        assertEquals(RoomSession.State.READY, h.session.getState());
        assertFalse(arb.sent.contains(RoomProtocol.LINE_ROOMOK));
    }

    @Test
    void denyOnTheArbiterLinkAbortsTheJoin() throws Exception {
        Harness h = joinStub();
        dialArbiter(h);
        feedWelcome(h, 2);
        h.line(h.arb, "room\tdeny\tBAD_TOKEN\tdetail");
        assertEquals(List.of("JOIN_FAILED:BAD_TOKEN"), h.closed);
    }

    @Test
    void arbiterLossDuringJoinAbortsWithoutMigration() throws Exception {
        Harness h = joinStub();
        dialArbiter(h);
        feedWelcome(h, 2);
        h.session.onLinkClosed(h.arb, "eof");
        assertEquals(List.of("JOIN_FAILED:arbiter lost during join"), h.closed);
    }

    // ================================================================ joiner-side guards

    @Test
    void nonArbiterIgnoresArbiterOnlyFramesAndAnswersLocally() throws Exception {
        Harness h = readyJoiner(2, 1);
        FakeLink l1 = peerLinks.get(1);

        h.line(h.arb, RoomProtocol.wrapControl("chat\thi"));   // control at a non-arbiter
        h.line(h.arb, RoomProtocol.buildPeerDown(1));          // peerdown at a non-arbiter
        h.line(h.arb, "room\tb\tbad");                         // malformed broadcast
        h.line(h.arb, "room\td");                              // malformed direct
        h.line(h.arb, "room\tkick\tzz");                       // malformed kick
        h.line(h.arb, "room\tkick\t99\tx");                    // kick for an unknown uid
        assertTrue(h.session.isOpen());
        assertNull(l1.closeReason);

        // Kick for a linked peer closes that link
        h.line(h.arb, RoomProtocol.buildKick(1, "split links"));
        assertEquals("kicked by arbiter", l1.closeReason);

        // Local record requests with an empty mirror fall back to the local blob
        h.session.sendLine("mpranking\t0");
        assertTrue(h.clientHasPrefix("mpranking\t0\t"));

        // game/spsend with no mirrored self are dropped
        h.session.sendLine("game\tx");
        h.session.sendLine("spsend\t1\tX");

        // A peer game line whose sender is mirrored but self is not: dropped
        h.line(h.arb, RoomProtocol.buildSnapPlayer(playerBlob(5, "Ghost", 0, 0, false)));
        h.line(h.arb, "game\t5\t0\tx");
        assertFalse(h.clientHasPrefix("game\t"));
    }

    // ================================================================ migration

    @Test
    void migrationBuffersControlsAndResendsOnClaim() throws Exception {
        Harness h = readyJoiner(2, 1, 3);
        FakeLink l1 = peerLinks.get(1);

        h.session.onLinkClosed(h.arb, "eof");
        assertEquals(RoomSession.State.MIGRATING, h.session.getState());

        // Local sends while migrating are buffered (no arbiter to route to)
        h.session.sendLine("ping");
        assertTrue(h.client.contains("pong"), "ping answers even while migrating");
        h.session.sendLine("dead\t0");
        h.session.sendLine("racewin\t1\t2");
        h.session.sendLine("ready\ttrue");

        // Peer 3 dies while no arbiter link exists: no peerdown report possible
        h.session.onLinkClosed(peerLinks.get(3), "eof");

        // Claims are only honored on the claimant's own link
        h.line(l1, "room\tarbiter\t99\t9");
        h.line(l1, "room\tarbiter\tzz\t9");
        assertEquals(RoomSession.State.MIGRATING, h.session.getState());

        h.line(l1, RoomProtocol.buildArbiterClaim(1, 9));
        assertEquals(RoomSession.State.READY, h.session.getState());
        assertEquals(List.of("1:false"), h.listener.arbiterChanges);
        assertTrue(l1.sent.contains(RoomProtocol.wrapControl("dead\t0")));
        assertTrue(l1.sent.contains(RoomProtocol.wrapControl("racewin\t1\t2")));
        // Mirror has no self entry, so the ready state cannot be compared/resent
        assertFalse(l1.sentPrefix("room\tc\tready\t"));
    }

    @Test
    void matchingReadyStateIsNotResentAfterMigration() throws Exception {
        Harness h = readyJoiner(2, 1);
        FakeLink l1 = peerLinks.get(1);
        h.line(h.arb, RoomProtocol.buildSnapPlayer(playerBlob(2, "Joiner", 0, 0, false)));

        h.session.sendLine("ready\tfalse");
        assertTrue(h.arb.sent.contains(RoomProtocol.wrapControl("ready\tfalse")));

        h.session.onLinkClosed(h.arb, "eof");
        assertEquals(RoomSession.State.MIGRATING, h.session.getState());
        h.line(l1, RoomProtocol.buildArbiterClaim(1, 9));
        assertEquals(RoomSession.State.READY, h.session.getState());
        assertFalse(l1.sentPrefix("room\tc\tready\t"), "mirror already agrees; no resend");
    }

    @Test
    void arbiterLossWhileAlreadyMigratingRestartsMigration() throws Exception {
        Harness h = readyJoiner(2, 1);
        setField(h.session, "state", RoomSession.State.MIGRATING);
        h.session.onLinkClosed(h.arb, "eof");
        assertEquals(RoomSession.State.MIGRATING, h.session.getState());
        assertEquals(1, getField(h.session, "expectedClaimUid"));
    }

    @Test
    void lowestSurvivorPromotesItself() throws Exception {
        Harness h = readyJoiner(1);
        h.session.onLinkClosed(h.arb, "eof");
        assertTrue(h.session.isArbiter());
        assertEquals(RoomSession.State.READY, h.session.getState());
        assertEquals(List.of("1:true"), h.listener.arbiterChanges);
    }

    @Test
    void emptyRosterMigrationShutsDown() throws Exception {
        Harness h = readyJoiner(2);
        RoomRoster roster = (RoomRoster) getField(h.session, "roster");
        roster.all().clear();
        invoke(h.session, "beginMigration");
        assertEquals(List.of("LAST_PEER"), h.closed);
    }

    // ================================================================ ticks

    @Test
    void joinTimeoutFiresOnlyOncePastTheDeadline() throws Exception {
        // CONNECTING but young: no shutdown
        Harness fresh = joinStub();
        fresh.tick();
        assertTrue(fresh.session.isOpen());
        setField(fresh.session, "joinStartedAt", 1L);
        fresh.tick();
        assertEquals(List.of("JOIN_FAILED:timeout"), fresh.closed);

        // Same deadline applies while JOINING
        Harness joining = joinStub();
        dialArbiter(joining);
        feedWelcome(joining, 2);
        setField(joining.session, "joinStartedAt", 1L);
        joining.tick();
        assertEquals(List.of("JOIN_FAILED:timeout"), joining.closed);

        // joinStartedAt == 0 never times out (created sessions)
        Harness created = arbiter();
        setField(created.session, "state", RoomSession.State.CONNECTING);
        created.tick();
        assertTrue(created.session.isOpen());
        setField(created.session, "state", RoomSession.State.READY);
    }

    @Test
    void claimTimeoutSkipsCandidatesUntilSelfPromotion() throws Exception {
        Harness h = readyJoiner(2, 1, 3);
        FakeLink l1 = peerLinks.get(1);

        // Peer 3's link dies while READY: reported, entry stays (link null)
        h.session.onLinkClosed(peerLinks.get(3), "eof");
        assertTrue(h.arb.sent.contains(RoomProtocol.buildPeerDown(3)));

        h.session.onLinkClosed(h.arb, "eof");
        assertEquals(RoomSession.State.MIGRATING, h.session.getState());

        h.tick();   // deadline is in the future: keep waiting
        assertEquals(RoomSession.State.MIGRATING, h.session.getState());

        setField(h.session, "claimDeadline", 0L);
        h.tick();   // no deadline armed: keep waiting
        assertEquals(RoomSession.State.MIGRATING, h.session.getState());

        // Candidate not in the roster at all
        setField(h.session, "expectedClaimUid", 42);
        setField(h.session, "claimDeadline", 1L);
        h.tick();
        assertEquals(RoomSession.State.MIGRATING, h.session.getState());
        assertEquals(1, getField(h.session, "expectedClaimUid"), "recomputed to the lowest uid");

        // Candidate present but linkless
        setField(h.session, "expectedClaimUid", 3);
        setField(h.session, "claimDeadline", 1L);
        h.tick();
        assertEquals(1, getField(h.session, "expectedClaimUid"));

        // Candidate with a live link: closed, then we are the lowest survivor
        setField(h.session, "claimDeadline", 1L);
        h.tick();
        assertEquals("claim timeout", l1.closeReason);
        assertTrue(h.session.isArbiter());
        assertEquals(List.of("2:true"), h.listener.arbiterChanges);
    }

    @Test
    void idleLinksArePingedThenTimedOut() throws Exception {
        Harness h = arbiter();
        FakeLink member = admitMember(h, "Bob", 7060);
        RoomConfig cfg = (RoomConfig) getField(h.session, "config");

        member.lastInboundMillis = System.currentTimeMillis() - cfg.pingInterval - 50;
        h.tick();
        assertTrue(member.sent.contains(RoomProtocol.LINE_PING));
        assertNull(member.closeReason);

        member.lastInboundMillis = System.currentTimeMillis() - cfg.linkTimeout - 50;
        h.tick();
        assertEquals("timeout", member.closeReason);
    }

    // ================================================================ peerdown arbitration

    @Test
    void peerDownReportsAggregateAndPickTheWorstVictim() throws Exception {
        Harness h = arbiter();
        FakeLink member = admitMember(h, "Bob", 7070);

        h.line(member, RoomProtocol.buildPeerDown(99));    // unknown uid: ignored
        h.line(member, "room\tpeerdown\tzz");              // malformed: ignored
        assertEquals(0L, getField(h.session, "peerdownWindowEnd"));

        h.line(member, RoomProtocol.buildPeerDown(1));
        h.line(member, RoomProtocol.buildPeerDown(1));     // second report increments

        @SuppressWarnings("unchecked")
        Map<Integer, Integer> reports = (Map<Integer, Integer>) getField(h.session, "peerdownReports");
        assertEquals(2, reports.get(1));

        // Window resolves with every reported member already gone: nobody kicked
        reports.clear();
        reports.put(5, 1);
        setField(h.session, "peerdownWindowEnd", 1L);
        h.tick();
        assertFalse(member.sentPrefix("room\tkick\t"));
        assertEquals(0L, getField(h.session, "peerdownWindowEnd"));

        // Tie-break: uid 17 iterates first (same count as 3), keeps the kick;
        // 4 has fewer reports; 5 is no longer a member
        RoomRoster roster = (RoomRoster) getField(h.session, "roster");
        roster.add(new RoomRoster.Entry(17, "X", "10.9.9.17", 7017, null));
        roster.add(new RoomRoster.Entry(3, "Y", "10.9.9.3", 7003, null));
        roster.add(new RoomRoster.Entry(4, "Z", "10.9.9.4", 7004, null));
        reports.put(17, 2);
        reports.put(3, 2);
        reports.put(4, 1);
        reports.put(5, 1);
        setField(h.session, "peerdownWindowEnd", 1L);
        h.tick();
        assertTrue(member.sentPrefix("room\tkick\t17\t"));
        assertNull(roster.get(17), "victim removed from the roster");
        assertNotNull(roster.get(3));
        assertNotNull(roster.get(4));
        assertTrue(h.session.isOpen());
    }

    // ================================================================ broadcast scope + dedup

    @Test
    void broadcastScopeFilterDedupAndRatingCapture() throws Exception {
        Harness h = readyJoiner(2);

        // Scoped line before any mirror self exists: filtered
        h.line(h.arb, RoomProtocol.wrapBroadcast(1, 7, "dead\t9\tx"));
        assertTrue(h.client.isEmpty());

        h.line(h.arb, RoomProtocol.buildSnapPlayer(playerBlob(2, "Joiner", 7, 0, false)));

        h.line(h.arb, RoomProtocol.wrapBroadcast(2, 7, "start\tstuff"));
        h.line(h.arb, RoomProtocol.wrapBroadcast(3, 7, "rating\t9\t1500\t" + NetUtil.urlEncode("Bob") + "\t1600"));
        h.line(h.arb, RoomProtocol.wrapBroadcast(4, 7, "dead\t9\tx"));
        h.line(h.arb, RoomProtocol.wrapBroadcast(5, 7, "dead\t8\tx"));
        h.line(h.arb, RoomProtocol.wrapBroadcast(6, 7, "dead\t8\tx"));      // duplicate: filtered
        h.line(h.arb, RoomProtocol.wrapBroadcast(7, 7, "finish\t9\tx"));
        h.line(h.arb, RoomProtocol.wrapBroadcast(8, 7, "finish\t9\tx"));    // duplicate: filtered
        h.line(h.arb, RoomProtocol.wrapBroadcast(9, 8, "dead\t9\tx"));      // other room: filtered

        assertEquals(List.of(
                "start\tstuff",
                "rating\t9\t1500\t" + NetUtil.urlEncode("Bob") + "\t1600",
                "dead\t9\tx",
                "dead\t8\tx",
                "finish\t9\tx"), h.client);
    }
}
