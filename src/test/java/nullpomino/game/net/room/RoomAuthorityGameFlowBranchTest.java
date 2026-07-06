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
 * Branch-gap tests for {@link RoomAuthority} game-lifecycle paths: the
 * start1p/ready/autostart seat guards, TNET2 and classic auto-start, the
 * auto-start timer's stop/start clauses, map rotation (previous-map retry),
 * dead-message guards and race-win KO filtering.
 */
class RoomAuthorityGameFlowBranchTest {

    /** Random whose nextInt(bound) values are scripted (nextLong stays seeded) */
    private static final class ScriptedRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final int[] script;
        private int index = 0;
        ScriptedRandom(int... script) { super(1); this.script = script; }
        @Override public int nextInt(int bound) { return script[index++] % bound; }
    }

    private static final class RecordingSink implements RoomAuthority.Sink {
        final List<String> broadcasts = new ArrayList<String>();   // scope + "|" + line
        final List<String> directs = new ArrayList<String>();
        final List<String> mapCaches = new ArrayList<String>();

        public void broadcast(int scope, String line, int exceptUid) { broadcasts.add(scope + "|" + line); }
        public void direct(int uid, String line) { directs.add(uid + "|" + line); }
        public void ruleCache(int uid, String checksum, String data) {}
        public void roomRuleCache(int roomId, String data) {}
        public void mapCache(int roomId, String data) { mapCaches.add(roomId + "|" + data); }
        public void authUpdate() {}

        String lastBroadcastStarting(String prefix) {
            for (int i = broadcasts.size() - 1; i >= 0; i--) {
                String line = broadcasts.get(i).split("\\|", 2)[1];
                if (line.startsWith(prefix)) return line;
            }
            return null;
        }
        int countBroadcastsStarting(String prefix) {
            int n = 0;
            for (String entry : broadcasts) {
                if (entry.split("\\|", 2)[1].startsWith(prefix)) n++;
            }
            return n;
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

    private void createRoom(int creatorUid, String name, NetRoomInfo template, String mode) {
        control(creatorUid, "roomcreate\t" + NetUtil.urlEncode(name) + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode(mode));
    }

    private NetRoomInfo template(int maxPlayers, int autoStartSeconds) {
        NetRoomInfo t = new NetRoomInfo();
        t.maxPlayers = maxPlayers;
        t.autoStartSeconds = autoStartSeconds;
        t.strMode = "NET-VS-BATTLE";
        return t;
    }

    // ---------------------------------------------------------------- start1p

    @Test
    void start1pStartsOnlySeatedSingleplayerGames() {
        admit(0, "Alice");
        admit(1, "Bob");

        sink.clear();
        control(1, "start1p");   // lobby: no room
        assertNull(sink.lastBroadcastStarting("start\t"));

        control(0, "singleroomcreate\t" + NetUtil.urlEncode("Solo") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        control(1, "roomjoin\t0\tfalse");   // seatless in a single room

        sink.clear();
        control(1, "start1p");   // seat == -1
        assertNull(sink.lastBroadcastStarting("start\t"));

        control(0, "start1p");   // seated in the singleplayer room
        assertNotNull(sink.lastBroadcastStarting("start\t"));
        assertTrue(auth.getRoom().playing);
    }

    @Test
    void start1pIgnoredInMultiplayerRoom() {
        admit(0, "Alice");
        createRoom(0, "Multi", template(2, 0), "NET-VS-BATTLE");
        sink.clear();
        control(0, "start1p");   // seat != -1 but room is not singleplayer
        assertNull(sink.lastBroadcastStarting("start\t"));
        assertFalse(auth.getRoom().playing);
    }

    // ---------------------------------------------------------------- ready guards

    @Test
    void readyIgnoredInLobbySingleplayerAndAsWatcher() {
        admit(0, "Alice");
        admit(1, "Bob");

        control(0, "ready\ttrue");   // lobby
        assertFalse(auth.getPlayer(0).ready);

        control(0, "singleroomcreate\t" + NetUtil.urlEncode("Solo") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        control(0, "ready\ttrue");   // seated but singleplayer
        assertFalse(auth.getPlayer(0).ready);

        control(0, "roomjoin\t-1\tfalse");
    }

    @Test
    void readyIgnoredForWatcherInMultiplayerRoom() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Multi", template(2, 0), "NET-VS-BATTLE");
        control(1, "roomjoin\t0\ttrue");   // Bob watches

        control(1, "ready\ttrue");
        assertFalse(auth.getPlayer(1).ready);
    }

    // ---------------------------------------------------------------- autostart command

    @Test
    void autoStartIgnoredWithoutRoomSeatOrActiveTimer() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");

        sink.clear();
        control(2, "autostart");   // lobby
        assertNull(sink.lastBroadcastStarting("start\t"));

        createRoom(0, "Timed", template(6, 15), "NET-VS-BATTLE");
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\ttrue");   // Carol watches

        sink.clear();
        control(0, "autostart");   // timer not active yet
        assertNull(sink.lastBroadcastStarting("start\t"));

        control(0, "ready\ttrue"); // 1 of 2 ready -> timer begins
        assertNotNull(sink.lastBroadcastStarting("autostartbegin\t"));

        control(2, "autostart");   // watcher: seat == -1
        assertNull(sink.lastBroadcastStarting("start\t"));
    }

    @Test
    void autoStartIgnoredInSingleplayerRoomEvenIfTimerFlagged() {
        admit(0, "Alice");
        control(0, "singleroomcreate\t" + NetUtil.urlEncode("Solo") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        auth.getRoom().autoStartActive = true;   // cannot happen naturally; force the flag

        sink.clear();
        control(0, "autostart");
        assertNull(sink.lastBroadcastStarting("start\t"));
        assertFalse(auth.getRoom().playing);
    }

    @Test
    void tnet2AutoStartMovesNonReadySeatsToSpectators() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        admit(3, "Dave");
        NetRoomInfo t = template(6, 15);
        t.autoStartTNET2 = true;
        createRoom(0, "TNET2", t, "NET-VS-BATTLE");
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");
        control(3, "roomjoin\t0\tfalse");
        control(3, "roomjoin\t-1\tfalse");   // Dave leaves: null hole in playerSeat

        control(0, "ready\ttrue");           // TNET2 minPlayers=2: no timer yet
        assertNull(sink.lastBroadcastStarting("autostartbegin\t"));
        control(1, "ready\ttrue");           // 2 ready -> timer begins (and stays on Carol's join state)
        assertNotNull(sink.lastBroadcastStarting("autostartbegin\t"));

        sink.clear();
        control(0, "autostart");

        assertNotNull(sink.lastBroadcastStarting("changestatus\twatchonly\t2\t"));
        assertEquals(-1, auth.getPlayer(2).seatID);
        assertNotNull(sink.lastBroadcastStarting("start\t"));
        assertTrue(auth.getRoom().playing);
        assertEquals(2, auth.getRoom().startPlayers);
    }

    @Test
    void classicAutoStartLaunchesWithNonReadyPlayers() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        createRoom(0, "Classic", template(6, 15), "NET-VS-BATTLE");
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");   // timer begins (1 >= 1 and 1 >= 3/2==1)
        assertNotNull(sink.lastBroadcastStarting("autostartbegin\t"));

        sink.clear();
        control(1, "autostart");     // non-TNET2: everyone starts, ready or not

        assertNull(sink.lastBroadcastStarting("changestatus\twatchonly\t"));
        assertNotNull(sink.lastBroadcastStarting("start\t"));
        assertEquals(3, auth.getRoom().startPlayers);
    }

    // ---------------------------------------------------------------- autostart timer clauses

    @Test
    void timerStopsAndUnreadiesWhenSeatsDropToOne() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Timed", template(6, 15), "NET-VS-BATTLE");
        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        assertNotNull(sink.lastBroadcastStarting("autostartbegin\t"));

        sink.clear();
        control(1, "roomjoin\t-1\tfalse");   // seats: [Alice, null]

        assertNotNull(sink.lastBroadcastStarting("autostartstop"));
        assertFalse(auth.getRoom().autoStartActive);
        assertFalse(auth.getPlayer(0).ready, "Sole seated player is unreadied");

        // Another timer check with one non-ready seat exercises the other loop arm
        sink.clear();
        control(0, "ready\tfalse");
        assertNull(sink.lastBroadcastStarting("autostartstop"), "Timer already inactive: silent stop");
    }

    @Test
    void timerStaysSilentlyStoppedWhenTooFewAreReady() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        admit(3, "Dave");
        createRoom(0, "Quorum", template(6, 15), "NET-VS-BATTLE");
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");
        control(3, "roomjoin\t0\tfalse");

        sink.clear();
        control(0, "ready\ttrue");   // 1 ready of 4 seated: 1 < 4/2 -> stop clause, timer never active

        assertNull(sink.lastBroadcastStarting("autostartbegin\t"));
        assertNull(sink.lastBroadcastStarting("autostartstop"));
        assertFalse(auth.getRoom().autoStartActive);
    }

    @Test
    void cancelWithDisableFlagKeepsTimerStopped() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        NetRoomInfo t = template(6, 15);
        t.disableTimerAfterSomeoneCancelled = true;
        createRoom(0, "Strict", t, "NET-VS-BATTLE");
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        assertNotNull(sink.lastBroadcastStarting("autostartbegin\t"));

        sink.clear();
        control(0, "ready\tfalse");   // cancel: someoneCancelled && disable -> stop
        assertNotNull(sink.lastBroadcastStarting("autostartstop"));

        sink.clear();
        control(0, "ready\ttrue");    // still blocked by the sticky cancel flag
        assertNull(sink.lastBroadcastStarting("autostartbegin\t"));
        assertFalse(auth.getRoom().autoStartActive);
    }

    @Test
    void timerRestartsAfterCancelWhenDisableFlagIsOff() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        createRoom(0, "Lenient", template(6, 15), "NET-VS-BATTLE");
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");

        control(0, "ready\ttrue");    // begin
        control(0, "ready\tfalse");   // 0 ready -> stop, isSomeoneCancelled=true
        assertNotNull(sink.lastBroadcastStarting("autostartstop"));

        sink.clear();
        control(0, "ready\ttrue");    // cancelled but disable flag off -> begin again
        assertNotNull(sink.lastBroadcastStarting("autostartbegin\t"));
        assertTrue(auth.getRoom().autoStartActive);
    }

    // ---------------------------------------------------------------- maps

    private void createMapRoom(String name, String... maps) {
        NetRoomInfo t = template(2, 0);
        t.useMap = true;
        String compressed = NetUtil.compressString(String.join("\t", maps));
        control(0, "roomcreate\t" + NetUtil.urlEncode(name) + "\t"
                + NetUtil.urlEncode(t.exportString()) + "\t" + NetUtil.urlEncode("NET-VS-BATTLE")
                + "\t" + compressed);
    }

    @Test
    void mapRoomRetriesRandomDrawThatRepeatsPreviousMap() {
        // nextInt script: round 1 draws 0; round 2 draws 0 (== previous, retried) then 1
        sink = new RecordingSink();
        auth = new RoomAuthority(sink, new ScriptedRandom(0, 0, 1));
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        createMapRoom("Maps", "MAPDATA-A", "MAPDATA-B");

        assertEquals(2, auth.getRoom().mapList.size());
        assertEquals(1, sink.mapCaches.size());

        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\ttrue");   // watcher is sent the map list
        assertNotNull(sink.lastDirectFor(2, "map\t"));

        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        String start1 = sink.lastBroadcastStarting("start\t");
        assertNotNull(start1);
        assertTrue(start1.endsWith("\t0"), "Round 1 uses map 0: " + start1);

        control(1, "dead");   // bare dead: no KO uid
        assertNotNull(sink.lastBroadcastStarting("finish\t"));

        sink.clear();
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        String start2 = sink.lastBroadcastStarting("start\t");
        assertNotNull(start2);
        assertTrue(start2.endsWith("\t1"), "Round 2 redraws away from the previous map: " + start2);
    }

    @Test
    void singleMapRoomAcceptsRepeatDraw() {
        sink = new RecordingSink();
        auth = new RoomAuthority(sink, new ScriptedRandom(0, 0));
        admit(0, "Alice");
        admit(1, "Bob");
        createMapRoom("OneMap", "ONLY-MAP");

        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        control(1, "dead");

        sink.clear();
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        String start2 = sink.lastBroadcastStarting("start\t");
        assertNotNull(start2);
        assertTrue(start2.endsWith("\t0"), "mapMax < 2: repeat draw is fine: " + start2);
    }

    // ---------------------------------------------------------------- dead guards

    @Test
    void deadIsIgnoredForWatchersIdlePlayersAndFinishedRooms() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        createRoom(0, "Guards", template(2, 0), "NET-VS-BATTLE");
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\ttrue");

        sink.clear();
        control(0, "dead");   // seated but game not started: pInfo.playing false
        assertNull(sink.lastBroadcastStarting("dead\t"));

        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        sink.clear();
        control(2, "dead");   // watcher: seatID == -1
        assertNull(sink.lastBroadcastStarting("dead\t"));

        control(1, "dead");   // real death ends the round
        assertNotNull(sink.lastBroadcastStarting("finish\t"));

        sink.clear();
        auth.getPlayer(0).playing = true;   // stale flag after the round ended
        control(0, "dead");   // roomInfo.playing is false
        assertNull(sink.lastBroadcastStarting("dead\t"));
        auth.getPlayer(0).playing = false;
    }

    // ---------------------------------------------------------------- racewin

    private String raceModeName() {
        for (NetMPModeRegistry.Entry entry : NetMPModeRegistry.forStyle(0)) {
            if (entry.isRace()) return entry.name();
        }
        return null;
    }

    @Test
    void raceWinIgnoredFromLobbyWatchersAndBeforeStart() {
        String raceMode = raceModeName();
        assertNotNull(raceMode);
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");

        sink.clear();
        control(2, "racewin\t2");   // lobby: roomID == -1
        assertTrue(sink.broadcasts.isEmpty());

        NetRoomInfo t = template(2, 0);
        t.strMode = raceMode;
        createRoom(0, "Race", t, raceMode);
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\ttrue");

        sink.clear();
        control(2, "racewin\t2");   // watcher: seatID == -1
        assertTrue(sink.broadcasts.isEmpty());

        control(0, "racewin\t0\t1");   // seated, but the game has not started
        assertNull(sink.lastBroadcastStarting("dead\t"));
    }

    @Test
    void raceWinIgnoredInNonRaceMode() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "VS", template(2, 0), "NET-VS-BATTLE");
        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");

        sink.clear();
        control(0, "racewin\t0\t1");   // registry says NET-VS-BATTLE is not a race
        assertNull(sink.lastBroadcastStarting("dead\t"));
        assertTrue(auth.getRoom().playing);
    }

    @Test
    void raceWinIgnoredForUnregisteredMode() {
        admit(0, "Alice");
        admit(1, "Bob");
        NetRoomInfo t = template(2, 0);
        t.strMode = "NOT-A-REGISTERED-MODE";
        createRoom(0, "Odd", t, "NOT-A-REGISTERED-MODE");
        control(1, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");

        sink.clear();
        control(0, "racewin\t0\t1");   // mode lookup exhausts the registry
        assertNull(sink.lastBroadcastStarting("dead\t"));
    }

    @Test
    void raceWinSkipsSelfUnknownAndOutOfRoomKoUids() {
        String raceMode = raceModeName();
        assertNotNull(raceMode);
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        admit(3, "Dave");   // stays in the lobby
        NetRoomInfo t = template(3, 0);
        t.strMode = raceMode;
        createRoom(0, "Race", t, raceMode);
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        control(2, "ready\ttrue");

        sink.clear();
        // KO list contains the winner's own uid, an unknown uid, a lobby uid, and Bob
        control(0, "racewin\t0\t1\t3\t99\t0");

        assertNotNull(sink.lastBroadcastStarting("dead\t1\t"));
        assertNull(sink.lastBroadcastStarting("dead\t0\t"));
        assertNull(sink.lastBroadcastStarting("dead\t3\t"));
        assertNull(sink.lastBroadcastStarting("dead\t99\t"));
    }

    // ---------------------------------------------------------------- reset1p

    @Test
    void reset1pResetsSeatedSinglePlayerAndFinishesTheRun() {
        admit(0, "Alice");
        admit(1, "Bob");
        control(0, "singleroomcreate\t" + NetUtil.urlEncode("Solo") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        control(1, "roomjoin\t0\tfalse");   // Bob seatless in the single room
        control(0, "start1p");
        assertTrue(auth.getRoom().playing);

        sink.clear();
        control(1, "reset1p");   // seat == -1: ignored
        assertNull(sink.lastBroadcastStarting("reset1p"));

        control(0, "reset1p");
        assertNotNull(sink.lastBroadcastStarting("reset1p"));
        assertNotNull(sink.lastBroadcastStarting("finish\t-1\t-1\t\t"));
        assertFalse(auth.getRoom().playing);
        assertFalse(auth.getPlayer(0).playing);
    }
}
