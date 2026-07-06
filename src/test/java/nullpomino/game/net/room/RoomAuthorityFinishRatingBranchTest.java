// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link RoomAuthority} finish detection and ratings:
 * team wins, ELO floor/ceiling clamps, singleplayer finishes with no winner,
 * finish/start re-checks on leave, roomForId mismatches and the
 * defensive guards of the private game helpers (via reflection).
 */
class RoomAuthorityFinishRatingBranchTest {

    private static final class RecordingSink implements RoomAuthority.Sink {
        final List<String> broadcasts = new ArrayList<String>();   // scope + "|" + line
        final List<String> directs = new ArrayList<String>();
        int authUpdates = 0;

        public void broadcast(int scope, String line, int exceptUid) { broadcasts.add(scope + "|" + line); }
        public void direct(int uid, String line) { directs.add(uid + "|" + line); }
        public void ruleCache(int uid, String checksum, String data) {}
        public void roomRuleCache(int roomId, String data) {}
        public void mapCache(int roomId, String data) {}
        public void authUpdate() { authUpdates++; }

        String lastBroadcastStarting(String prefix) {
            for (int i = broadcasts.size() - 1; i >= 0; i--) {
                String line = broadcasts.get(i).split("\\|", 2)[1];
                if (line.startsWith(prefix)) return line;
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

    private void createRoom(int creatorUid, String name, NetRoomInfo template) {
        control(creatorUid, "roomcreate\t" + NetUtil.urlEncode(name) + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode("NET-VS-BATTLE"));
    }

    private NetRoomInfo vsTemplate(int maxPlayers) {
        NetRoomInfo template = new NetRoomInfo();
        template.maxPlayers = maxPlayers;
        template.strMode = "NET-VS-BATTLE";
        return template;
    }

    // ---------------------------------------------------------------- team win

    @Test
    void teamWinFinishesWithTeamNameAndSkipsRating() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        admit(3, "Dave");
        NetRoomInfo template = vsTemplate(6);
        template.rated = true;   // rated flag is ignored for team games
        createRoom(0, "Teams", template);
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");
        control(3, "roomjoin\t0\tfalse");
        control(3, "roomjoin\t-1\tfalse");   // null hole in playerSeat

        control(0, "changeteam\t" + NetUtil.urlEncode("A"));
        control(1, "changeteam\t" + NetUtil.urlEncode("A"));
        control(2, "changeteam\t" + NetUtil.urlEncode("B"));
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        control(2, "ready\ttrue");
        assertTrue(auth.getRoom().playing);
        assertEquals(0, auth.getPlayer(0).playCount[0], "Team game: rated per-style play counts untouched");

        sink.clear();
        control(2, "dead");   // Carol out: team A is the last one standing

        String finish = sink.lastBroadcastStarting("finish\t");
        assertNotNull(finish);
        assertEquals("finish\t-1\t-1\t" + NetUtil.urlEncode("A") + "\ttrue", finish);
        assertNull(sink.lastBroadcastStarting("rating\t"));
        assertEquals(1, auth.getPlayer(0).winCountNow);
        assertEquals(1, auth.getPlayer(1).winCountNow);
        assertEquals(0, auth.getPlayer(2).winCountNow);
        assertFalse(auth.getRoom().playing);
    }

    // ---------------------------------------------------------------- rating clamps

    private void admitRated(int expectedUid, String name, int styleZeroRating) {
        int uid = auth.reserveUid();
        assertEquals(expectedUid, uid);
        auth.admitMember(uid, name, "127.0.0.1",
                new int[] { styleZeroRating }, new int[0], new int[0]);
        auth.getPlayer(uid).ruleOpt = new RuleOptions();
    }

    private void playRatedRound(int winnerUid, int loserUid) {
        NetRoomInfo template = vsTemplate(2);
        template.rated = true;
        createRoom(winnerUid, "Rated", template);
        control(loserUid, "roomjoin\t0\tfalse");
        control(winnerUid, "ready\ttrue");
        control(loserUid, "ready\ttrue");
        control(loserUid, "dead\t" + winnerUid);
    }

    @Test
    void ratingClampsWinnerToFloorAndLoserToCeiling() {
        admitRated(0, "Alice", -5000);    // stays under the floor even after winning
        admitRated(1, "Bob", 150000);     // stays over the ceiling even after losing
        playRatedRound(0, 1);

        assertEquals(RoomRating.RATING_MIN, auth.getPlayer(0).rating[0]);
        assertEquals(RoomRating.RATING_MAX, auth.getPlayer(1).rating[0]);
        assertNotNull(sink.lastBroadcastStarting("rating\t0\t"));
        assertNotNull(sink.lastBroadcastStarting("rating\t1\t"));
    }

    @Test
    void ratingClampsWinnerToCeilingAndLoserToFloor() {
        admitRated(0, "Alice", 200000);   // sure win gains ~0: clamp from above
        admitRated(1, "Bob", -200000);    // sure loss loses ~0: clamp from below
        playRatedRound(0, 1);

        assertEquals(RoomRating.RATING_MAX, auth.getPlayer(0).rating[0]);
        assertEquals(RoomRating.RATING_MIN, auth.getPlayer(1).rating[0]);
    }

    // ---------------------------------------------------------------- singleplayer finishes

    @Test
    void singleplayerDeathFinishesWithNoWinner() {
        admit(0, "Alice");
        control(0, "singleroomcreate\t" + NetUtil.urlEncode("Solo") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        control(0, "start1p");
        assertTrue(auth.getRoom().playing);

        sink.clear();
        control(0, "dead");

        String finish = sink.lastBroadcastStarting("finish\t");
        assertNotNull(finish);
        assertEquals("finish\t-1\t-1\t\tfalse", finish);
        assertFalse(auth.getRoom().playing);
    }

    @Test
    void watcherLeavingDuringSingleplayerGameDoesNotFinishIt() {
        admit(0, "Alice");
        admit(1, "Bob");
        control(0, "singleroomcreate\t" + NetUtil.urlEncode("Solo") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        control(1, "roomjoin\t0\tfalse");   // seatless in the single room
        control(0, "start1p");

        sink.clear();
        auth.onMemberGone(1, true);   // startPlayers==1: the (startPlayers>=2) finish arm is false

        assertNull(sink.lastBroadcastStarting("finish\t"));
        assertTrue(auth.getRoom().playing, "Alice's run keeps going");
        assertNotNull(sink.lastBroadcastStarting("playerlogout\t"));
    }

    @Test
    void singleplayerFlaggedRoomWithTwoSeatsFinishesWithNoWinnerLine() {
        // A room blob can claim singleplayer with maxPlayers>1; force two seats
        // to reach the "winner exists but room is singleplayer" finish arm.
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        NetRoomInfo template = vsTemplate(2);
        template.singleplayer = true;
        createRoom(0, "Odd", template);
        control(1, "roomjoin\t0\tfalse");    // singleplayer room: no seat handed out
        control(2, "roomjoin\t0\ttrue");

        NetRoomInfo room = auth.getRoom();
        NetPlayerInfo bob = auth.getPlayer(1);
        bob.seatID = room.joinSeat(bob);     // force the second seat
        assertEquals(1, bob.seatID);
        auth.getPlayer(0).ready = true;      // onReady refuses singleplayer rooms; set directly
        bob.ready = true;

        control(2, "roomjoin\t-1\tfalse");   // Carol leaves -> start re-check fires
        assertTrue(room.playing);
        assertEquals(2, room.startPlayers);

        sink.clear();
        control(1, "dead\t0");

        String finish = sink.lastBroadcastStarting("finish\t");
        assertNotNull(finish);
        assertEquals("finish\t-1\t-1\t\tfalse", finish, "Winner is discarded in singleplayer rooms");
        assertEquals(0, auth.getPlayer(0).winCountNow);
    }

    // ---------------------------------------------------------------- leave re-checks

    @Test
    void nonReadyPlayerLeavingViaRoomJoinStartsTheGame() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        createRoom(0, "Leave", vsTemplate(6));
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        assertFalse(auth.getRoom().playing);

        sink.clear();
        control(2, "roomjoin\t-1\tfalse");   // the only non-ready seat leaves

        assertNotNull(sink.lastBroadcastStarting("start\t"));
        assertTrue(auth.getRoom().playing);
    }

    @Test
    void nonReadyMemberGoneStartsTheGame() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        createRoom(0, "Gone", vsTemplate(6));
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");

        sink.clear();
        auth.onMemberGone(2, false);

        assertNotNull(sink.lastBroadcastStarting("start\t"));
        assertTrue(auth.getRoom().playing);
    }

    @Test
    void memberGoneWithDesyncedSeatFieldStillFinishesTheGame() {
        // playerDead() keys on pInfo.seatID; if it is stale the death is skipped
        // and the finish must come from the post-leave re-check instead.
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Desync", vsTemplate(2));
        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        assertTrue(auth.getRoom().playing);

        auth.getPlayer(1).seatID = -1;   // desync: still in playerSeat, field cleared
        sink.clear();
        auth.onMemberGone(1, false);

        assertNull(sink.lastBroadcastStarting("dead\t1\t"));
        assertNotNull(sink.lastBroadcastStarting("finish\t0\t"));
        assertFalse(auth.getRoom().playing);
    }

    @Test
    void leaveViaRoomJoinWithDesyncedSeatFieldStillFinishesTheGame() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Desync2", vsTemplate(2));
        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");

        auth.getPlayer(1).seatID = -1;
        sink.clear();
        control(1, "roomjoin\t-1\tfalse");

        assertNull(sink.lastBroadcastStarting("dead\t1\t"));
        assertNotNull(sink.lastBroadcastStarting("finish\t0\t"));
        assertFalse(auth.getRoom().playing);
    }

    @Test
    void spectatorSwitchOfLastNonReadySeatStartsTheGame() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        createRoom(0, "Switch", vsTemplate(6));
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");

        sink.clear();
        control(2, "changestatus\ttrue");   // Carol steps out: remaining seats all ready

        assertNotNull(sink.lastBroadcastStarting("start\t"));
        assertTrue(auth.getRoom().playing);
    }

    // ---------------------------------------------------------------- roomForId / resync

    @Test
    void staleRoomIdFieldsAreTreatedAsNoRoom() {
        admit(0, "Alice");
        sink.clear();
        auth.getPlayer(0).roomID = 0;   // claims a room that was never created
        control(0, "chat\t" + NetUtil.urlEncode("hello"));
        control(0, "changestatus\tfalse");
        control(0, "ready\ttrue");
        assertTrue(sink.broadcasts.isEmpty());
        auth.getPlayer(0).roomID = -1;

        admit(1, "Bob");
        createRoom(0, "Real", vsTemplate(2));
        control(1, "roomjoin\t0\tfalse");
        sink.clear();
        auth.getPlayer(1).roomID = 7;   // exists, but the session room has id 0
        control(1, "chat\t" + NetUtil.urlEncode("hello"));
        control(1, "racewin\t7\t0");
        assertTrue(sink.broadcasts.isEmpty());
        auth.getPlayer(1).roomID = 0;
    }

    @Test
    void resyncAllWithoutRoomOnlyRebroadcastsPlayers() {
        admit(0, "Alice");
        sink.clear();
        int updatesBefore = sink.authUpdates;
        auth.resyncAll();
        assertNotNull(sink.lastBroadcastStarting("playerupdate\t"));
        assertNull(sink.lastBroadcastStarting("roomupdate\t"));
        assertEquals(updatesBefore + 1, sink.authUpdates);
    }

    // ---------------------------------------------------------------- private guards (reflection)

    private Object invokePrivate(String name, Class<?> paramType, Object arg) throws Exception {
        Method m = RoomAuthority.class.getDeclaredMethod(name, paramType);
        m.setAccessible(true);
        return m.invoke(auth, arg);
    }

    @Test
    void privateGameHelpersGuardAgainstNullAndEmptyRooms() throws Exception {
        assertEquals(Boolean.FALSE, invokePrivate("gameStartIfPossible", NetRoomInfo.class, null));

        sink.clear();
        invokePrivate("gameStart", NetRoomInfo.class, null);
        assertTrue(sink.broadcasts.isEmpty());

        NetRoomInfo empty = new NetRoomInfo();
        invokePrivate("gameStart", NetRoomInfo.class, empty);   // nobody seated
        assertTrue(sink.broadcasts.isEmpty());
        assertFalse(empty.playing);

        NetRoomInfo lone = new NetRoomInfo();
        NetPlayerInfo p = new NetPlayerInfo();
        p.uid = 42;
        lone.playerSeat.add(p);
        invokePrivate("gameStart", NetRoomInfo.class, lone);    // 1 seat, not singleplayer
        assertTrue(sink.broadcasts.isEmpty());
        assertFalse(lone.playing);
    }

    @Test
    void deleteRoomGuardsNullAndForeignRooms() throws Exception {
        assertEquals(Boolean.FALSE, invokePrivate("deleteRoom", NetRoomInfo.class, null));

        NetRoomInfo foreign = new NetRoomInfo();   // empty room that is not the session's
        sink.clear();
        assertEquals(Boolean.TRUE, invokePrivate("deleteRoom", NetRoomInfo.class, foreign));
        assertNotNull(sink.lastBroadcastStarting("roomdelete\t"));
        assertNull(auth.getRoom());
    }
}
