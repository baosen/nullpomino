// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetMPModeRegistry;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Golden-line tests for {@link RoomAuthority}: the emitted broadcast/direct
 * lines must match NetServer's formats byte-for-byte, with correct room
 * scoping, across the create/join/queue/ready/start/dead/finish/race/leave
 * lifecycle.
 */
class RoomAuthorityTest {

    private static final class Emitted {
        final int scope;
        final String line;
        final int exceptUid;
        Emitted(int scope, String line, int exceptUid) {
            this.scope = scope; this.line = line; this.exceptUid = exceptUid;
        }
    }

    private static final class RecordingSink implements RoomAuthority.Sink {
        final List<Emitted> broadcasts = new ArrayList<Emitted>();
        final List<String> directs = new ArrayList<String>();      // "uid|line"
        int authUpdates = 0;

        public void broadcast(int scope, String line, int exceptUid) {
            broadcasts.add(new Emitted(scope, line, exceptUid));
        }
        public void direct(int uid, String line) { directs.add(uid + "|" + line); }
        public void ruleCache(int uid, String checksum, String data) {}
        public void roomRuleCache(int roomId, String data) {}
        public void mapCache(int roomId, String data) {}
        public void authUpdate() { authUpdates++; }

        Emitted lastBroadcastStarting(String prefix) {
            for (int i = broadcasts.size() - 1; i >= 0; i--) {
                if (broadcasts.get(i).line.startsWith(prefix)) return broadcasts.get(i);
            }
            return null;
        }
        String lastDirectFor(int uid, String prefix) {
            for (int i = directs.size() - 1; i >= 0; i--) {
                String[] f = directs.get(i).split("\\|", 2);
                if (Integer.parseInt(f[0]) == uid && f[1].startsWith(prefix)) return f[1];
            }
            return null;
        }
        void clear() { broadcasts.clear(); directs.clear(); }
    }

    private RecordingSink sink;
    private RoomAuthority auth;

    @BeforeEach
    void setUp() {
        sink = new RecordingSink();
        auth = new RoomAuthority(sink, new Random(42));
    }

    private void admit(int expectedUid, String name) {
        int uid = auth.reserveUid();
        assertEquals(expectedUid, uid);
        auth.admitMember(uid, name, "127.0.0.1");
        auth.getPlayer(uid).ruleOpt = new RuleOptions();
    }

    private void control(int uid, String line) {
        auth.handleControl(uid, line.split("\t", -1));
    }

    /** roomcreate line for a plain 2-6 player VS room */
    private void createRoom(int creatorUid, String name, int maxPlayers, int autoStartSeconds) {
        NetRoomInfo template = new NetRoomInfo();
        template.maxPlayers = maxPlayers;
        template.autoStartSeconds = autoStartSeconds;
        template.strMode = "NET-VS-BATTLE";
        control(creatorUid, "roomcreate\t" + NetUtil.urlEncode(name) + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode("NET-VS-BATTLE"));
    }

    @Test
    void roomCreateEmitsCreateSequence() {
        admit(0, "Alice");
        createRoom(0, "My Room", 2, 0);

        assertEquals("roomcreatesuccess\t0\t0\t-1", sink.lastDirectFor(0, "roomcreatesuccess"));
        Emitted create = sink.lastBroadcastStarting("roomcreate\t");
        assertNotNull(create);
        assertEquals(RoomProtocol.SCOPE_GLOBAL, create.scope);
        assertNotNull(sink.lastBroadcastStarting("playerupdate\t"));
        assertTrue(sink.authUpdates > 0);
        assertNotNull(auth.getRoom());
        assertEquals(0, auth.getPlayer(0).seatID);
    }

    @Test
    void fullRoomQueuesThenPromotesInFifoOrder() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        createRoom(0, "Two Seats", 2, 0);

        control(1, "roomjoin\t0\tfalse");
        assertEquals("roomjoinsuccess\t0\t1\t-1", sink.lastDirectFor(1, "roomjoinsuccess"));

        control(2, "roomjoin\t0\tfalse");
        assertEquals("roomjoinsuccess\t0\t-1\t0", sink.lastDirectFor(2, "roomjoinsuccess"));

        sink.clear();
        control(1, "roomjoin\t-1\tfalse");   // Bob leaves; Carol takes the freed seat 1

        Emitted promo = sink.lastBroadcastStarting("changestatus\tjoinseat\t2\t");
        assertNotNull(promo);
        assertEquals(0, promo.scope);
        assertTrue(promo.line.endsWith("\t1"), "Promoted into freed seat 1: " + promo.line);
        assertEquals("roomjoinsuccess\t-1\t-1\t-1", sink.lastDirectFor(1, "roomjoinsuccess"));
    }

    @Test
    void allReadyStartsRoundWithSeedAndFrozenRoster() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Round", 2, 0);
        control(1, "roomjoin\t0\tfalse");

        sink.clear();
        control(0, "ready\ttrue");
        assertNull(sink.lastBroadcastStarting("start\t"), "One ready of two must not start");

        control(1, "ready\ttrue");
        Emitted start = sink.lastBroadcastStarting("start\t");
        assertNotNull(start);
        assertEquals(0, start.scope);
        String[] f = start.line.split("\t");
        assertEquals(4, f.length);
        Long.parseLong(f[1], 16);       // seed parses as base-16
        assertEquals("2", f[2]);        // startPlayers
        assertEquals("0", f[3]);        // mapNo
        assertTrue(auth.getRoom().playing);
        assertTrue(auth.getPlayer(0).playing);

        Emitted roomUpd = sink.lastBroadcastStarting("roomupdate\t");
        assertNotNull(roomUpd);
        assertEquals(RoomProtocol.SCOPE_GLOBAL, roomUpd.scope);
    }

    @Test
    void deathPlaceAndFinishFollowServerMath() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Round", 2, 0);
        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");

        sink.clear();
        control(1, "dead\t0");   // Bob dies, KO credited to Alice

        Emitted dead = sink.lastBroadcastStarting("dead\t");
        assertNotNull(dead);
        assertEquals(0, dead.scope);
        assertEquals("dead\t1\t" + NetUtil.urlEncode("Bob") + "\t1\t2\t0\t" + NetUtil.urlEncode("Alice"), dead.line);

        Emitted finish = sink.lastBroadcastStarting("finish\t");
        assertNotNull(finish);
        assertEquals(0, finish.scope);
        assertEquals("finish\t0\t0\t" + NetUtil.urlEncode("Alice") + "\tfalse", finish.line);

        assertFalse(auth.getRoom().playing);
        assertEquals(1, auth.getPlayer(0).winCountNow);

        int deadIdx = -1, finishIdx = -1;
        for (int i = 0; i < sink.broadcasts.size(); i++) {
            if (sink.broadcasts.get(i).line.startsWith("dead\t")) deadIdx = i;
            if (sink.broadcasts.get(i).line.startsWith("finish\t")) finishIdx = i;
        }
        assertTrue(deadIdx < finishIdx, "dead must precede finish");
    }

    @Test
    void leaveMidGameCountsAsDeathAndFinishes() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Round", 2, 0);
        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");

        sink.clear();
        control(1, "roomjoin\t-1\tfalse");

        assertNotNull(sink.lastBroadcastStarting("dead\t1\t"));
        Emitted finish = sink.lastBroadcastStarting("finish\t");
        assertNotNull(finish);
        assertTrue(finish.line.startsWith("finish\t0\t0\t"));

        Emitted leave = sink.lastBroadcastStarting("playerleave\t1\t");
        assertNotNull(leave);
        assertEquals(1, leave.exceptUid);
    }

    @Test
    void memberGoneMidGameRunsLogoutSequence() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Round", 2, 0);
        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");

        sink.clear();
        auth.onMemberGone(1, false);

        assertNotNull(sink.lastBroadcastStarting("dead\t1\t"));
        assertNotNull(sink.lastBroadcastStarting("finish\t0\t0\t"));
        assertNotNull(sink.lastBroadcastStarting("playerlogout\t"));
        assertNull(auth.getPlayer(1));
    }

    @Test
    void lastLeaverDeletesRoom() {
        admit(0, "Alice");
        createRoom(0, "Doomed", 2, 0);

        sink.clear();
        control(0, "roomjoin\t-1\tfalse");

        Emitted del = sink.lastBroadcastStarting("roomdelete\t");
        assertNotNull(del);
        assertEquals(RoomProtocol.SCOPE_GLOBAL, del.scope);
        assertNull(auth.getRoom());
    }

    @Test
    void autoStartTimerBeginsAndStopsOnCancel() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Timed", 6, 15);
        control(1, "roomjoin\t0\tfalse");

        sink.clear();
        control(0, "ready\ttrue");
        Emitted begin = sink.lastBroadcastStarting("autostartbegin\t");
        assertNotNull(begin);
        assertEquals("autostartbegin\t15", begin.line);
        assertEquals(0, begin.scope);

        control(0, "ready\tfalse");
        assertNotNull(sink.lastBroadcastStarting("autostartstop"));
        assertTrue(auth.getRoom().isSomeoneCancelled);
    }

    @Test
    void racewinKillsLosersWithKoCreditAndFinishes() {
        String raceMode = null;
        for (NetMPModeRegistry.Entry entry : NetMPModeRegistry.forStyle(0)) {
            if (entry.isRace()) { raceMode = entry.name(); break; }
        }
        assertNotNull(raceMode, "Registry must contain a race mode for style 0");

        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        NetRoomInfo template = new NetRoomInfo();
        template.maxPlayers = 3;
        template.strMode = raceMode;
        control(0, "roomcreate\t" + NetUtil.urlEncode("Race") + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode(raceMode));
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        control(2, "ready\ttrue");

        sink.clear();
        control(0, "racewin\t0\t1\t2");   // Alice declares: herself 1st, Bob 2nd, Carol 3rd

        Emitted carolDead = sink.lastBroadcastStarting("dead\t2\t");
        assertNotNull(carolDead);
        assertTrue(carolDead.line.contains("\t3\t0\t"), "Carol places 3rd, KO by Alice: " + carolDead.line);
        Emitted bobDead = sink.lastBroadcastStarting("dead\t1\t");
        assertNotNull(bobDead);
        assertTrue(bobDead.line.contains("\t2\t0\t"), "Bob places 2nd, KO by Alice: " + bobDead.line);
        assertNotNull(sink.lastBroadcastStarting("finish\t0\t0\t"));
    }

    @Test
    void chatIsRoomScopedAndReplayedToJoiners() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Chatty", 2, 0);

        sink.clear();
        control(0, "chat\t" + NetUtil.urlEncode("hello there"));
        Emitted chat = sink.lastBroadcastStarting("chat\t0\t");
        assertNotNull(chat);
        assertEquals(0, chat.scope);
        assertTrue(chat.line.endsWith("\t" + NetUtil.urlEncode("hello there")));

        control(1, "roomjoin\t0\tfalse");
        String chath = sink.lastDirectFor(1, "chath\t");
        assertNotNull(chath);
        assertTrue(chath.endsWith("\t" + NetUtil.urlEncode("hello there")));
    }

    @Test
    void secondRoomCreateIsIgnored() {
        // One session = one room: once the room exists, further roomcreate
        // requests (even from members still in the lobby phase) are ignored
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "The Room", 2, 0);

        sink.clear();
        createRoom(1, "Second Room", 2, 0);

        assertNull(sink.lastDirectFor(1, "roomcreatesuccess"));
        assertNull(sink.lastBroadcastStarting("roomcreate\t"));
        assertEquals(-1, auth.getPlayer(1).roomID);
        assertEquals("The Room", auth.getRoom().strName);
    }

    @Test
    void ratedRoundEmitsEloRatingLines() {
        admit(0, "Alice");
        admit(1, "Bob");
        NetRoomInfo template = new NetRoomInfo();
        template.maxPlayers = 2;
        template.rated = true;
        template.strMode = "NET-VS-BATTLE";
        control(0, "roomcreate\t" + NetUtil.urlEncode("Rated") + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode("NET-VS-BATTLE"));
        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");

        sink.clear();
        control(1, "dead\t0");

        // First game between two 1500s: maxDelta(1)=116, expected 0.5 -> winner +58.
        // NetServer's pairwise loop updates the winner BEFORE computing the loser's
        // delta, so the loser is scored against 1558 and only drops 48 - pinned here.
        Emitted winnerRating = sink.lastBroadcastStarting("rating\t0\t");
        assertNotNull(winnerRating);
        assertEquals("rating\t0\t0\t" + NetUtil.urlEncode("Alice") + "\t1558\t58", winnerRating.line);
        assertEquals(0, winnerRating.scope);
        Emitted loserRating = sink.lastBroadcastStarting("rating\t1\t");
        assertNotNull(loserRating);
        assertEquals("rating\t1\t1\t" + NetUtil.urlEncode("Bob") + "\t1452\t-48", loserRating.line);

        assertEquals(1, auth.getPlayer(0).playCount[0]);
        assertEquals(1, auth.getPlayer(0).winCount[0]);
        assertEquals(1, auth.getPlayer(1).playCount[0]);
        assertEquals(0, auth.getPlayer(1).winCount[0]);
    }

    @Test
    void unratedRoundEmitsNoRatingLines() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Casual", 2, 0);
        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");

        sink.clear();
        control(1, "dead\t0");

        assertNull(sink.lastBroadcastStarting("rating\t"));
        assertEquals(0, auth.getPlayer(0).playCount[0]);
    }

    @Test
    void changeNameBroadcastsRenameAndPlayerUpdate() {
        admit(0, "Alice");
        admit(1, "Bob");

        sink.clear();
        control(1, "changename\t" + NetUtil.urlEncode("Bobby"));

        Emitted rename = sink.lastBroadcastStarting("changename\t");
        assertNotNull(rename);
        assertEquals("changename\t1\t" + NetUtil.urlEncode("Bob") + "\t" + NetUtil.urlEncode("Bobby"), rename.line);
        assertEquals(RoomProtocol.SCOPE_GLOBAL, rename.scope);
        assertEquals("Bobby", auth.getPlayer(1).strName);
        assertNotNull(sink.lastBroadcastStarting("playerupdate\t"));
    }

    @Test
    void changeNameRejectsDuplicateEmptyAndPlaying() {
        admit(0, "Alice");
        admit(1, "Bob");

        control(1, "changename\t" + NetUtil.urlEncode("Alice"));
        assertEquals("changenamefail\tDUPLICATE", sink.lastDirectFor(1, "changenamefail"));

        control(1, "changename\t" + NetUtil.urlEncode("  "));
        assertEquals("changenamefail\tEMPTY", sink.lastDirectFor(1, "changenamefail"));

        // Mid-game renames are refused (names are frozen into the round)
        createRoom(0, "Round", 2, 0);
        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        sink.clear();
        control(1, "changename\t" + NetUtil.urlEncode("Bobby"));
        assertEquals("changenamefail\tPLAYING", sink.lastDirectFor(1, "changenamefail"));
        assertEquals("Bob", auth.getPlayer(1).strName);
    }

    @Test
    void changeNameToSameNameIsANoOp() {
        admit(0, "Alice");
        sink.clear();
        control(0, "changename\t" + NetUtil.urlEncode("Alice"));
        assertNull(sink.lastBroadcastStarting("changename\t"));
        assertNull(sink.lastDirectFor(0, "changenamefail"));
    }

    @Test
    void watchJoinTakesNoSeat() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Spectate", 2, 0);

        control(1, "roomjoin\t0\ttrue");
        assertEquals("roomjoinsuccess\t0\t-1\t-1", sink.lastDirectFor(1, "roomjoinsuccess"));

        // and can take a seat via changestatus
        sink.clear();
        control(1, "changestatus\tfalse");
        Emitted joinSeat = sink.lastBroadcastStarting("changestatus\tjoinseat\t1\t");
        assertNotNull(joinSeat);
        assertTrue(joinSeat.line.endsWith("\t1"));
    }
}
