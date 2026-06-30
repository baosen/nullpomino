package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedList;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.Test;

/**
 * Additional branch-coverage tests for {@link NetVSBattleMode}.
 *
 * <p>Targets previously-uncovered branch outcomes across the netplay message
 * handlers ({@code netlobbyOnMessage} dead/attack/hurryup, {@code netRecvStats},
 * {@code netvsRecvEndGameStats}) and several false/true sides of conditions in
 * {@code calcScore} and {@code onLast} that the existing suites did not exercise.
 *
 * <p>The mode is wired to a <em>disconnected</em> {@link NetLobbyFrame}/
 * {@link NetPlayerClient}: {@code send(..)} swallows the IOException and
 * {@code getYourPlayerInfo()} returns null so {@code netvsIsWatch()} returns
 * false (its NPE is caught).  {@code getPlayerUID()} returns 0.
 */
class NetVSBattleModeBranchCoverageTest3 {

    // =====================================================================
    // netlobbyOnMessage – "dead" handler (lines 936-947)
    // =====================================================================

    /**
     * "dead" message whose koUID (message[5]) matches our own UID (0) takes the
     * true side of line 943 → playerKObyYou set and currentKO incremented.
     * Pre-mark the dead player so the super handler's NPE-prone branch is skipped.
     */
    @Test
    void deadMessageWithMatchingKoUidIncrementsKo() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        buildEngine(mode);

        // seatID 2 -> playerID 2 (identity mapping when myseat == -1).
        setBooleanArray(mode, "netvsPlayerDead", 2, true); // skip super's body
        setIntField(mode, "currentKO", 0);

        // dead\t<uid>\t<?>\t<seatID=2>\t<place=1>\t<koUID=0(=our UID)>
        String[] msg = {"dead", "10", "x", "2", "1", "0"};
        mode.netlobbyOnMessage(lobby(mode), client(mode), msg);

        assertTrue(readBooleanArray(mode, "playerKObyYou", 2),
                "matching koUID should flag player as KO'd by you");
        assertEquals(1, readInt(mode, "currentKO"), "KO count should increment");
    }

    /**
     * "dead" message with NO koUID field (message.length <= 5) leaves koUID at
     * the -1 default → false side of line 943; also covers line 940 false side.
     */
    @Test
    void deadMessageWithoutKoUidDoesNotIncrementKo() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        buildEngine(mode);

        setBooleanArray(mode, "netvsPlayerDead", 1, true);
        setIntField(mode, "currentKO", 0);

        // Only 5 fields -> message.length == 5, so the koUID parse is skipped.
        String[] msg = {"dead", "10", "x", "1", "2"};
        mode.netlobbyOnMessage(lobby(mode), client(mode), msg);

        assertFalse(readBooleanArray(mode, "playerKObyYou", 1),
                "no koUID -> player not flagged");
        assertEquals(0, readInt(mode, "currentKO"), "KO count unchanged");
    }

    /**
     * "dead" message whose koUID does NOT match our UID -> false side of line 943
     * with line 940 true side (koUID is parsed but mismatched).
     */
    @Test
    void deadMessageWithNonMatchingKoUidDoesNotIncrementKo() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        buildEngine(mode);

        setBooleanArray(mode, "netvsPlayerDead", 1, true);
        setIntField(mode, "currentKO", 0);

        // koUID = 999, our getPlayerUID() == 0 -> mismatch.
        String[] msg = {"dead", "10", "x", "1", "2", "999"};
        mode.netlobbyOnMessage(lobby(mode), client(mode), msg);

        assertFalse(readBooleanArray(mode, "playerKObyYou", 1));
        assertEquals(0, readInt(mode, "currentKO"));
    }

    // =====================================================================
    // netlobbyOnMessage – "game"/"attack" handler (lines 949-994)
    // =====================================================================

    /**
     * Drive the "game"/"attack" branch so the big guard on line 973 is taken on
     * its true side: not watching, timerActive, sumPts > 0, not practice, not
     * newcomer, targetSeatID == -1, and the sender is attackable.  The received
     * attack is enqueued as a garbage entry.
     */
    @Test
    void gameAttackMessageEnqueuesGarbage() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        room.isTarget = false;
        room.b2bChunk = false;
        setField(mode, "netCurrentRoomInfo", room);

        engine.timerActive = true;
        setField(mode, "netvsIsPractice", false);
        setField(mode, "netvsIsNewcomer", false);
        makeAttackable(mode, 1); // seatID 1 -> playerID 1

        // game\t<uid>\t<seatID=1>\tattack\t<6 pts>\t<gap>\t<event>\t<b2b>\t<combo>\t<garbage>\t<piece>\t<targetSeat=-1>
        // The sender appends an extra tab after the 6 pts (see calcScore), so the
        // post-points fields start at index ATTACK_CATEGORIES + 5 == 11.
        String[] msg = {
            "game", "10", "1", "attack",
            "120", "0", "0", "0", "0", "0", // pts[0..5] (indices 4-9): sumPts = 120
            "",    // index 10: trailing-tab gap
            "1",   // lastevent
            "false", // lastb2b
            "0",   // lastcombo
            "0",   // garbage
            "0",   // lastpiece
            "-1"   // targetSeatID
        };
        mode.netlobbyOnMessage(lobby(mode), client(mode), msg);

        LinkedList<?> entries = (LinkedList<?>) getField(mode, "garbageEntries");
        assertEquals(1, entries.size(), "received attack should enqueue one garbage entry");
        assertEquals(0, readIntArray(mode, "lastcombo", 1), "fields parsed from message");
        assertEquals(1, readIntArray(mode, "lastevent", 1));
    }

    /**
     * Same attack message but with {@code b2bChunk = true} and a non-zero B2B
     * point category, so the {@code secondAdd > 0} branch (line 985) splits the
     * attack into TWO garbage entries.
     */
    @Test
    void gameAttackMessageWithB2bChunkEnqueuesTwoEntries() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        room.isTarget = false;
        room.b2bChunk = true;
        setField(mode, "netCurrentRoomInfo", room);

        engine.timerActive = true;
        setField(mode, "netvsIsPractice", false);
        setField(mode, "netvsIsNewcomer", false);
        makeAttackable(mode, 1);

        // pts[ATTACK_CATEGORY_B2B] = pts[1] = 60 -> secondAdd = 60 > 0
        String[] msg = {
            "game", "10", "1", "attack",
            "120", "60", "0", "0", "0", "0", // sumPts = 180, b2b=60
            "", // trailing-tab gap
            "1", "true", "0", "0", "0", "-1"
        };
        mode.netlobbyOnMessage(lobby(mode), client(mode), msg);

        LinkedList<?> entries = (LinkedList<?>) getField(mode, "garbageEntries");
        assertEquals(2, entries.size(),
                "b2bChunk with non-zero B2B points should split into two entries");
    }

    /**
     * "game"/"attack" with {@code sumPts == 0} fails the {@code (sumPts > 0)}
     * conjunct of line 973 (false side) -> nothing enqueued.
     */
    @Test
    void gameAttackMessageZeroPtsEnqueuesNothing() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        setField(mode, "netCurrentRoomInfo", room);
        engine.timerActive = true;
        makeAttackable(mode, 1);

        String[] msg = {
            "game", "10", "1", "attack",
            "0", "0", "0", "0", "0", "0", // sumPts = 0
            "", // trailing-tab gap
            "1", "false", "0", "0", "0", "-1"
        };
        mode.netlobbyOnMessage(lobby(mode), client(mode), msg);

        LinkedList<?> entries = (LinkedList<?>) getField(mode, "garbageEntries");
        assertEquals(0, entries.size(), "zero-point attack should not enqueue garbage");
    }

    // =====================================================================
    // netlobbyOnMessage – "game"/"hurryup" handler (lines 996-1004)
    // =====================================================================

    /**
     * "game"/"hurryup" with hurryup not yet started and hurryupSeconds > 0 takes
     * the true side of line 997 -> hurryupStarted becomes true and the show
     * timer is armed.
     */
    @Test
    void gameHurryupMessageStartsHurryup() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        room.hurryupSeconds = 30;
        setField(mode, "netCurrentRoomInfo", room);
        setField(mode, "hurryupStarted", false);
        engine.timerActive = true;

        String[] msg = {"game", "10", "1", "hurryup"};
        mode.netlobbyOnMessage(lobby(mode), client(mode), msg);

        assertTrue(readBoolean(mode, "hurryupStarted"), "hurryup should be started");
        assertEquals(60 * 5, readInt(mode, "hurryupShowFrames"));
    }

    /**
     * "game"/"hurryup" when hurryup already started takes the false side of
     * line 997 -> show timer left at its previous (zero) value.
     */
    @Test
    void gameHurryupMessageAlreadyStartedDoesNothing() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        buildEngine(mode);

        NetRoomInfo room = makeRoom();
        room.hurryupSeconds = 30;
        setField(mode, "netCurrentRoomInfo", room);
        setField(mode, "hurryupStarted", true);
        setIntField(mode, "hurryupShowFrames", 0);

        String[] msg = {"game", "10", "1", "hurryup"};
        mode.netlobbyOnMessage(lobby(mode), client(mode), msg);

        assertEquals(0, readInt(mode, "hurryupShowFrames"),
                "already-started hurryup should not re-arm the show timer");
    }

    // =====================================================================
    // netRecvStats (lines 880-884)
    // =====================================================================

    @Test
    void netRecvStatsParsesGarbageWhenLongEnough() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);
        engine.playerID = 0;

        // message[4] present -> garbage[0] parsed (true side of length>4 guard).
        String[] msg = {"game", "10", "0", "stats", "123"};
        invokeProtected(mode, "netRecvStats",
                new Class<?>[]{GameEngine.class, String[].class},
                engine, msg);

        assertEquals(123, readIntArray(mode, "garbage", 0));
    }

    @Test
    void netRecvStatsShortMessageLeavesGarbageUnchanged() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);
        engine.playerID = 0;
        setIntArray(mode, "garbage", 7, 0);

        // length == 4 -> guard false -> garbage unchanged.
        String[] msg = {"game", "10", "0", "stats"};
        invokeProtected(mode, "netRecvStats",
                new Class<?>[]{GameEngine.class, String[].class},
                engine, msg);

        assertEquals(7, readIntArray(mode, "garbage", 0), "short stats message must not alter garbage");
    }

    // =====================================================================
    // netvsRecvEndGameStats (lines 906-926)
    // =====================================================================

    /**
     * End-of-game stats for an opponent (playerID != 0) take the true side of
     * line 910 (first disjunct) and populate that player's stats arrays.
     */
    @Test
    void netvsRecvEndGameStatsForOpponentPopulatesStats() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        buildEngine(mode);

        // seatID 1 -> playerID 1 (!= 0) -> first disjunct true.
        String[] msg = {
            "gstat", "x", "1",  // [2] = seatID
            "1",                 // [3] place (unused here)
            "x",                 // [4]
            "2.0",               // [5] garbageSend -> *60 = 120
            "1.5",               // [6] APL
            "30.0",              // [7] APM
            "10",                // [8] lines
            "40.0",              // [9] lpm
            "25",                // [10] totalPieceLocked
            "2.0",               // [11] pps
            "3600"               // [12] time
        };
        invokeProtected(mode, "netvsRecvEndGameStats",
                new Class<?>[]{String[].class}, (Object) msg);

        assertEquals(120, readIntArray(mode, "garbageSent", 1),
                "garbageSend 2.0 * GARBAGE_DENOMINATOR(60) = 120");
        assertEquals(1.5f, readFloatArray(mode, "playerAPL", 1), 0.001f);
        assertEquals(30.0f, readFloatArray(mode, "playerAPM", 1), 0.001f);
        assertEquals(10, owner(mode).engine[1].statistics.lines);
        assertTrue(readBooleanArray(mode, "netvsPlayerResultReceived", 1));
    }

    // =====================================================================
    // calcScore – false/true sides not covered by existing suites
    // =====================================================================

    /**
     * {@code reduceLineSend = true} with 3 alive teams takes the FALSE side of
     * line 290 (so attackNumPlayerIndex = numAliveTeams - 2 = 1).  A normal
     * double then maps to LINE_ATTACK_TABLE[DOUBLE=1][1] = 1 * 60 = 60.
     */
    @Test
    void calcScoreReduceLineSendUsesTeamCountIndex() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        room.reduceLineSend = true; // !reduceLineSend == false
        room.bravo = false;
        setField(mode, "netCurrentRoomInfo", room);
        // Line 290 false side needs practice==false AND reduceLineSend==true, so
        // attackNumPlayerIndex keeps its computed (numAliveTeams - 2) value.
        setField(mode, "netvsIsPractice", false);
        makeThreeTeamsAlive(mode, engine);
        fillFloorToBlockBravo(engine);

        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        mode.calcScore(engine, 0, 2); // double

        // numAliveTeams = 3 -> attackNumPlayerIndex = 1 -> LINE_ATTACK_TABLE[1][1] = 1 -> *60
        assertEquals(60, readIntArray(mode, "garbageSent", 0),
                "double with 3 alive teams & reduceLineSend -> index 1 -> 1*60");
    }

    /**
     * {@code bravo = false} with an empty field -> false side of the all-clear
     * guard at line 385: no bravo bonus is added.  A normal single (table 0)
     * yields zero attack, proving the bravo bonus did NOT fire.
     */
    @Test
    void calcScoreBravoDisabledNoAllClearBonus() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        room.bravo = false; // false side of line 385
        setField(mode, "netCurrentRoomInfo", room);
        setField(mode, "netvsIsPractice", true);

        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        // empty field -> isEmpty() true, but bravo=false short-circuits.
        mode.calcScore(engine, 0, 1); // single -> table 0

        assertEquals(0, readIntArray(mode, "garbageSent", 0),
                "bravo disabled -> single all-clear gives no attack");
    }

    /**
     * {@code counter = true} with pending garbage and points -> true side of the
     * counter guard at line 414.  A normal double (60 pts) fully cancels a 60-line
     * garbage entry, removing it from the queue.
     */
    @Test
    void calcScoreCounterCancelsGarbage() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        room.counter = true; // true side of line 414
        room.bravo = false;
        room.useFractionalGarbage = false;
        setField(mode, "netCurrentRoomInfo", room);
        setField(mode, "netvsIsPractice", true); // skip send + delivery
        fillFloorToBlockBravo(engine);

        addGarbageEntry(mode, 60); // exactly one full line of garbage

        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        mode.calcScore(engine, 0, 2); // double -> 60 pts cancels the 60 garbage

        LinkedList<?> entries = (LinkedList<?>) getField(mode, "garbageEntries");
        assertEquals(0, entries.size(), "60-pt counter should fully consume the 60-line entry");
    }

    /**
     * Fractional garbage with 3+ alive teams -> true side of line 399, dividing
     * the points among (numAliveTeams - 1) opponents.
     */
    @Test
    void calcScoreFractionalGarbageDividesByOpponents() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        room.useFractionalGarbage = true; // line 398 true
        room.reduceLineSend = false;      // index forced to 0 regardless
        room.counter = false;
        room.bravo = false;
        setField(mode, "netCurrentRoomInfo", room);
        setField(mode, "netvsIsPractice", false); // line 398 requires !practice
        makeThreeTeamsAlive(mode, engine);
        fillFloorToBlockBravo(engine);

        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        // double -> LINE_ATTACK_TABLE[1][0] = 1 -> *60 = 60, then /(3-1)=2 -> 30.
        mode.calcScore(engine, 0, 2);

        assertEquals(30, readIntArray(mode, "garbageSent", 0),
                "60 points divided by (3 alive teams - 1) = 30");
    }

    // =====================================================================
    // onLast – HURRY UP start path (lines 564-573)
    // =====================================================================

    /**
     * onLast at the exact hurryup trigger frame, not watching and not practice,
     * takes the inner true side of line 567 -> hurryupStarted set and timer armed.
     */
    @Test
    void onLastTriggersHurryupAtThreshold() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        room.hurryupSeconds = 1; // trigger frame = 60
        setField(mode, "netCurrentRoomInfo", room);
        setField(mode, "hurryupStarted", false);
        setField(mode, "netvsIsPractice", false);
        engine.timerActive = true;
        setIntField(mode, "netvsPlayTimer", 60); // == hurryupSeconds * 60

        mode.onLast(engine, 0);

        assertTrue(readBoolean(mode, "hurryupStarted"), "onLast should start hurryup at threshold");
        assertEquals(60 * 5, readInt(mode, "hurryupShowFrames"));
    }

    /**
     * onLast Target rotation: with a valid target setup the targetTimer advances
     * and, on reaching the room targetTimer, resets to 0 (line 614 true side).
     */
    @Test
    void onLastTargetTimerResetsAndPicksNewTarget() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        room.isTarget = true;
        room.targetTimer = 1; // so targetTimer (->1) >= 1 immediately
        setField(mode, "netCurrentRoomInfo", room);
        setField(mode, "netvsIsPractice", false);

        engine.gameActive = true;
        engine.timerActive = true;
        setField(mode, "netvsPlayTimerActive", true);
        makeAttackable(mode, 1); // one possible target
        setIntField(mode, "targetID", 1);
        setIntField(mode, "targetTimer", 0);

        mode.onLast(engine, 0);

        assertEquals(0, readInt(mode, "targetTimer"),
                "targetTimer should reset to 0 after reaching room targetTimer");
    }

    // =====================================================================
    // renderLast / renderResult – render branches (no assertions, low conf.)
    // =====================================================================

    /**
     * renderLast with fractional garbage present and not in RESULT exercises the
     * garbage-count font-color thresholds (lines 633-637) plus the K.O. branch.
     */
    @Test
    void renderLastFractionalGarbageAndKoBranches() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        room.useFractionalGarbage = true;
        room.isTarget = false;
        setField(mode, "netCurrentRoomInfo", room);

        setBooleanArray(mode, "netvsPlayerExist", 0, true);
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;
        engine.displaysize = 0;
        setIntArray(mode, "garbage", 250, 0); // >= 60*4 -> red color path (lines 637-639)
        setBooleanArray(mode, "playerKObyYou", 0, true); // K.O. branch

        mode.renderLast(engine, 0); // must not throw
        assertNotNull(engine.field);
    }

    /**
     * renderLast for an invisible (empty) seat: netvsPlayerExist false / not
     * visible -> the player-field blocks are skipped (false side of 631/682).
     */
    @Test
    void renderLastInvisibleSeatSkipsFieldBlocks() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        NetRoomInfo room = makeRoom();
        setField(mode, "netCurrentRoomInfo", room);
        setBooleanArray(mode, "netvsPlayerExist", 0, false);
        engine.isVisible = false;

        mode.renderLast(engine, 0);
        assertNotNull(engine.field);
    }

    /**
     * renderResult returns early when the engine is invisible (line 850 true side).
     */
    @Test
    void renderResultInvisibleReturnsEarly() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);
        setField(mode, "netCurrentRoomInfo", makeRoom());
        engine.isVisible = false;

        mode.renderResult(engine, 0); // early return, no throw
        assertNotNull(engine.field);
    }

    /**
     * renderResult for a visible seat paints the extra stats panel (false side
     * of line 850) at half scale (displaysize == -1 -> line 853 true side).
     */
    @Test
    void renderResultVisibleDrawsStats() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);
        setField(mode, "netCurrentRoomInfo", makeRoom());
        engine.isVisible = true;
        engine.displaysize = -1; // half scale path

        mode.renderResult(engine, 0);
        assertNotNull(engine.field);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static GameEngine buildEngine(NetVSBattleMode mode) throws Exception {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        for (int i = 0; i < manager.engine.length; i++) {
            manager.engine[i].init();
            manager.engine[i].playerID = i;
            manager.engine[i].nowPieceObject = new Piece(Piece.PIECE_T);
        }
        mode.modeInit(manager);
        mode.playerInit(manager.engine[0], 0);
        manager.engine[0].createFieldIfNeeded();

        // Disconnected NetLobbyFrame + NetPlayerClient: send() swallows IOException.
        NetLobbyFrame lobby = new NetLobbyFrame();
        lobby.netPlayerClient = new NetPlayerClient();
        setField(mode, "netLobby", lobby);

        return manager.engine[0];
    }

    /** Fresh default room with sane fields for these tests. */
    private static NetRoomInfo makeRoom() {
        NetRoomInfo room = new NetRoomInfo();
        room.reduceLineSend = false;
        room.rensaBlock = false;
        room.bravo = false;
        room.useFractionalGarbage = false;
        room.counter = false;
        room.garbagePercent = 100;
        room.divideChangeRateByPlayers = false;
        room.garbageChangePerAttack = false;
        room.hurryupSeconds = -1;
        room.hurryupInterval = 5;
        room.isTarget = false;
        room.b2bChunk = false;
        room.targetTimer = 60;
        return room;
    }

    /** Make playerID attackable: exists, not dead, active, no shared team. */
    private static void makeAttackable(NetVSBattleMode mode, int playerID) throws Exception {
        setBooleanArray(mode, "netvsPlayerExist", playerID, true);
        setBooleanArray(mode, "netvsPlayerDead", playerID, false);
        setBooleanArray(mode, "netvsPlayerActive", playerID, true);
        setStringArray(mode, "netvsPlayerTeam", 0, "");
        setStringArray(mode, "netvsPlayerTeam", playerID, "");
        setIntArray(mode, "netvsPlayerSeatID", playerID, playerID);
    }

    /**
     * Make 3 alive teams: players 0,1,2 exist, not dead, active, gameActive and
     * teamless (so netvsGetNumberOfTeamsAlive() counts 3).
     */
    private static void makeThreeTeamsAlive(NetVSBattleMode mode, GameEngine engine) throws Exception {
        GameManager owner = owner(mode);
        for (int i = 0; i < 3; i++) {
            setBooleanArray(mode, "netvsPlayerExist", i, true);
            setBooleanArray(mode, "netvsPlayerDead", i, false);
            setBooleanArray(mode, "netvsPlayerActive", i, true);
            setStringArray(mode, "netvsPlayerTeam", i, "");
            owner.engine[i].gameActive = true;
        }
    }

    /** Place a block on the bottom-left so the field is not empty (no bravo). */
    private static void fillFloorToBlockBravo(GameEngine engine) {
        engine.field.setBlock(0, engine.field.getHeight() - 1,
                new Block(Block.BLOCK_COLOR_GRAY));
    }

    @SuppressWarnings("unchecked")
    private static void addGarbageEntry(NetVSBattleMode mode, int lines) throws Exception {
        Field f = findField(NetVSBattleMode.class, "garbageEntries");
        f.setAccessible(true);
        LinkedList<Object> entries = (LinkedList<Object>) f.get(mode);
        if (entries == null) {
            entries = new LinkedList<>();
            f.set(mode, entries);
        }
        Class<?> geClass = null;
        for (Class<?> c : NetVSBattleMode.class.getDeclaredClasses()) {
            if (c.getSimpleName().equals("GarbageEntry")) {
                geClass = c;
                break;
            }
        }
        assertNotNull(geClass, "GarbageEntry inner class not found");
        Constructor<?> ctor = geClass.getDeclaredConstructor(NetVSBattleMode.class, int.class);
        ctor.setAccessible(true);
        entries.add(ctor.newInstance(mode, lines));
    }

    private static NetLobbyFrame lobby(NetVSBattleMode mode) throws Exception {
        return (NetLobbyFrame) getField(mode, "netLobby");
    }

    private static NetPlayerClient client(NetVSBattleMode mode) throws Exception {
        return lobby(mode).netPlayerClient;
    }

    private static GameManager owner(NetVSBattleMode mode) throws Exception {
        return (GameManager) getField(mode, "owner");
    }

    private static void invokeProtected(Object target, String name, Class<?>[] sig, Object... args)
            throws Exception {
        java.lang.reflect.Method m = null;
        Class<?> c = target.getClass();
        while (c != null && m == null) {
            try { m = c.getDeclaredMethod(name, sig); }
            catch (NoSuchMethodException e) { c = c.getSuperclass(); }
        }
        assertNotNull(m, "method not found: " + name);
        m.setAccessible(true);
        m.invoke(target, args);
    }

    // ---- reflection field accessors ----

    private static int readInt(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.getInt(obj);
    }

    private static boolean readBoolean(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.getBoolean(obj);
    }

    private static int readIntArray(Object obj, String name, int index) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return ((int[]) f.get(obj))[index];
    }

    private static boolean readBooleanArray(Object obj, String name, int index) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return ((boolean[]) f.get(obj))[index];
    }

    private static float readFloatArray(Object obj, String name, int index) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return ((float[]) f.get(obj))[index];
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static void setField(Object obj, String name, boolean value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setBoolean(obj, value);
    }

    private static void setIntField(Object obj, String name, int value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setInt(obj, value);
    }

    private static void setIntArray(Object obj, String name, int value, int index) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        ((int[]) f.get(obj))[index] = value;
    }

    private static void setBooleanArray(Object obj, String name, int index, boolean value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        ((boolean[]) f.get(obj))[index] = value;
    }

    private static void setStringArray(Object obj, String name, int index, String value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        ((String[]) f.get(obj))[index] = value;
    }

    private static Object getField(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.get(obj);
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
}
