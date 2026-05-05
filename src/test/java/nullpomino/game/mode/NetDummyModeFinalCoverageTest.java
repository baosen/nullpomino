package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetSPRecord;
import nullpomino.game.net.NetUtil;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers remaining uncovered lines in {@link NetDummyMode}.
 */
class NetDummyModeFinalCoverageTest {

    private NetDummyMode mode;
    private GameManager manager;
    private GameEngine engine;
    private EventReceiver receiver;

    @BeforeEach
    void setUp() throws Exception {
        mode = new NetDummyMode();
        receiver = new EventReceiver();
        manager = new GameManager(receiver);
        manager.mode = mode;
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        engine = manager.engine[0];
        engine.ruleopt.fieldWidth = 10;
        engine.ruleopt.fieldHeight = 20;
        engine.ruleopt.fieldHiddenHeight = 4;
        engine.ruleopt.nextDisplay = 1;
        engine.createFieldIfNeeded();
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 2;
        mode.netRankingRank = new int[2];
        mode.netRankingRank[0] = -1;
        mode.netRankingRank[1] = -1;
        mode.netRankingPlace = new java.util.LinkedList[2];
        mode.netRankingName = new java.util.LinkedList[2];
        mode.netRankingDate = new java.util.LinkedList[2];
        mode.netRankingGamerate = new java.util.LinkedList[2];
        mode.netRankingTime = new java.util.LinkedList[2];
        mode.netRankingScore = new java.util.LinkedList[2];
        mode.netRankingPiece = new java.util.LinkedList[2];
        mode.netRankingPPS = new java.util.LinkedList[2];
        mode.netRankingLines = new java.util.LinkedList[2];
        mode.netRankingSPL = new java.util.LinkedList[2];
        mode.netRankingRollclear = new java.util.LinkedList[2];
    }

    // ────────────────────────────────────────────────────────────────
    // netplayInit NPE path
    // ────────────────────────────────────────────────────────────────
    @Test
    void netplayInitNpeCatch() throws Exception {
        NetLobbyFrame lobby = new NetLobbyFrame();
        lobby.ruleOptPlayer = null;
        mode.netLobby = lobby;
        mode.owner = null;
        mode.netplayInit(lobby);
        assertEquals(mode, lobby.getNetDummyMode());
    }

    // ────────────────────────────────────────────────────────────────
    // netplayUnload with lobby
    // ────────────────────────────────────────────────────────────────
    @Test
    void netplayUnloadWithLobby() throws Exception {
        NetLobbyFrame lobby = new NetLobbyFrame();
        mode.netLobby = lobby;
        mode.netplayUnload(null);
        assertNull(mode.netLobby);
    }

    // ────────────────────────────────────────────────────────────────
    // onGameOver with netplay stats send
    // ────────────────────────────────────────────────────────────────
	@Test
	void onGameOverSendsStats() throws Exception {
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new NetPlayerClient();
		engine.ending = 0;
		engine.statc[0] = 0;
		// Set up pieces to avoid NPE in netSendField
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		mode.owner = manager;
		engine.nextPieceArrayObject = new Piece[6];
		for (int i = 0; i < 6; i++) {
			engine.nextPieceArrayObject[i] = new Piece(Piece.PIECE_O);
		}

		boolean result = mode.onGameOver(engine, 0);
		assertFalse(result);
	}

    // ────────────────────────────────────────────────────────────────
    // onResult with A button
    // ────────────────────────────────────────────────────────────────
    @Test
    void onResultRetryWithSpectators() throws Exception {
        mode.netIsNetPlay = true;
        mode.netIsWatch = false;
        mode.netReplaySendStatus = 2;
        mode.netNumSpectators = 1;
        mode.netLobby = new NetLobbyFrame();
        mode.netLobby.netPlayerClient = new NetPlayerClient();
        mode.owner = manager;

        engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

        boolean result = mode.onResult(engine, 0);
        assertTrue(result);
    }

    // ────────────────────────────────────────────────────────────────
    // updateCursor with net-send
    // ────────────────────────────────────────────────────────────────
    @Test
    void updateCursorSendsNetMessage() throws Exception {
        mode.netIsNetPlay = true;
        mode.netNumSpectators = 1;
        mode.netLobby = new NetLobbyFrame();
        mode.netLobby.netPlayerClient = new NetPlayerClient();

        engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;

        mode.updateCursor(engine, 5, 0);
    }

    @Test
    void updateCursorWatchReturnsZero() throws Exception {
        mode.netIsWatch = true;
        int change = mode.updateCursor(engine, 5, 0);
        assertEquals(0, change);
    }

    // ────────────────────────────────────────────────────────────────
    // netlobbyOnRoomJoin / netlobbyOnDisconnect / netlobbyOnRoomLeave (empty)
    // ────────────────────────────────────────────────────────────────
    @Test
    void netlobbyOnRoomJoinDoesNotThrow() {
        mode.netlobbyOnRoomJoin(null, null, null);
    }

    @Test
    void netlobbyOnDisconnectDoesNotThrow() {
        mode.netlobbyOnDisconnect(null, null, null);
    }

    @Test
    void netlobbyOnRoomLeaveDoesNotThrow() {
        mode.netlobbyOnRoomLeave(null, null);
    }

    // ────────────────────────────────────────────────────────────────
    // netlobbyOnMessage: playerupdate and playerlogout
    // ────────────────────────────────────────────────────────────────
    @Test
    void netlobbyOnMessagePlayerUpdate() throws Exception {
        mode.netlobbyOnMessage(null, null, new String[]{"playerupdate"});
    }

    // ────────────────────────────────────────────────────────────────
    // netlobbyOnMessage: spranking (short message = no data)
    // ────────────────────────────────────────────────────────────────
    @Test
    void netlobbyOnMessageSprankingShort() throws Exception {
        mode.netRankingNoDataFlag = new boolean[]{false, false};
        mode.netRankingReady = new boolean[]{false, false};
        mode.netRankingPlace = new java.util.LinkedList[2];
        mode.netRankingName = new java.util.LinkedList[2];
        mode.netRankingDate = new java.util.LinkedList[2];
        mode.netRankingGamerate = new java.util.LinkedList[2];
        mode.netRankingTime = new java.util.LinkedList[2];
        mode.netRankingScore = new java.util.LinkedList[2];
        mode.netRankingPiece = new java.util.LinkedList[2];
        mode.netRankingPPS = new java.util.LinkedList[2];
        mode.netRankingLines = new java.util.LinkedList[2];
        mode.netRankingSPL = new java.util.LinkedList[2];
        mode.netRankingRollclear = new java.util.LinkedList[2];
        mode.netCurrentRoomInfo = new NetRoomInfo();
        String[] msg = {"spranking", "", "", "", "false"};
        mode.netlobbyOnMessage(null, null, msg);
    }

    // ────────────────────────────────────────────────────────────────
    // netlobbyOnMessage: game field with null field (watch mode)
    // ────────────────────────────────────────────────────────────────
    @Test
    void netlobbyOnMessageGameWatchNullField() throws Exception {
        mode.netIsWatch = true;
        engine.field = null;
        String[] msg = {"game", "", "", "field", "0", "0", "", "false"};
        mode.netlobbyOnMessage(null, null, msg);
        assertNotNull(engine.field);
    }

    // ────────────────────────────────────────────────────────────────
    // netlobbyOnMessage: game option (watch mode)
    // ────────────────────────────────────────────────────────────────
    @Test
    void netlobbyOnMessageGameOption() throws Exception {
        mode.netIsWatch = true;
        engine.field = new nullpomino.game.component.Field();
        String[] msg = {"game", "", "", "option", "0", "0"};
        mode.netlobbyOnMessage(null, null, msg);
    }

    // ────────────────────────────────────────────────────────────────
    // netSendField compressed
    // ────────────────────────────────────────────────────────────────
    @Test
    void netSendFieldCompressed() throws Exception {
        mode.netAlwaysSendFieldAttributes = true;
        mode.netLobby = new NetLobbyFrame();
        mode.netLobby.netPlayerClient = new NetPlayerClient();
        engine.createFieldIfNeeded();
        mode.netSendField(engine);
    }

    // ────────────────────────────────────────────────────────────────
    // netRecvField decompress paths
    // ────────────────────────────────────────────────────────────────
    @Test
    void netRecvFieldDecompressAttr() throws Exception {
        String compressed = NetUtil.compressString("10 20 ");
        String[] msg = {"game", "", "", "fieldattr", "0", compressed, "true"};
        engine.createFieldIfNeeded();
        engine.field.reset();
        mode.netRecvField(engine, msg);
    }

    @Test
    void netRecvFieldDecompressNonAttr() throws Exception {
        String compressed = NetUtil.compressString("10 20 ");
        String[] msg = {"game", "", "", "field", "0", "0", compressed, "true"};
        engine.createFieldIfNeeded();
        engine.field.reset();
        mode.netRecvField(engine, msg);
    }

    // ────────────────────────────────────────────────────────────────
    // netRecvNetPlayRanking: unknown type + cursor/myRank set
    // ────────────────────────────────────────────────────────────────
    @Test
    void netRecvNetPlayRankingUnknownType() throws Exception {
        mode.netPlayerName = "test_player";
        mode.netRecvNetPlayRanking(engine,
            new String[]{"spranking", "", "", "", "false", "99", "1", "0,test_player,2024/01/01,1.0,50000,200,3600"});
    }

    @Test
    void netRecvNetPlayRankingMyRank() throws Exception {
        mode.netPlayerName = "playerme";
        String row = "0,playerme,2024/01/01,1.0,50000,200,3600";
        String[] msg = {"spranking", "", "", "", "false", "0", "1", row};
        mode.netRecvNetPlayRanking(engine, msg);
        assertEquals(0, mode.netRankingCursor[0]);
        assertEquals(0, mode.netRankingMyRank[0]);
    }

    // ────────────────────────────────────────────────────────────────
    // netSendReplay
    // ────────────────────────────────────────────────────────────────
    @Test
    void netSendReplaySends() throws Exception {
        mode.netLobby = new NetLobbyFrame();
        mode.netLobby.netPlayerClient = new NetPlayerClient();
        mode.netIsNetPlay = true;
        mode.owner = manager;
        manager.replayProp = new nullpomino.util.CustomProperties();
        engine.statistics = new nullpomino.game.component.Statistics();
        engine.statistics.score = 50000;
        engine.statistics.lines = 200;

        Method m = findMethod(NetDummyMode.class, "netSendReplay", GameEngine.class);
        m.setAccessible(true);
        m.invoke(mode, engine);
    }

    // ────────────────────────────────────────────────────────────────
    // netOnRenderNetPlayRanking N/A display
    // ────────────────────────────────────────────────────────────────
    @Test
    void netOnRenderNetPlayRankingNA() throws Exception {
        mode.netIsNetRankingDisplayMode = true;
        mode.netRankingPlace[0] = new java.util.LinkedList<>();
        mode.netRankingPlace[0].add(-1);
        mode.netRankingName[0] = new java.util.LinkedList<>();
        mode.netRankingName[0].add("player1");
        mode.netRankingDate[0] = new java.util.LinkedList<>();
        mode.netRankingDate[0].add(java.util.Calendar.getInstance());
        mode.netRankingGamerate[0] = new java.util.LinkedList<>();
        mode.netRankingGamerate[0].add(1.0f);
        mode.netRankingTime[0] = new java.util.LinkedList<>();
        mode.netRankingTime[0].add(3600);
        mode.netRankingScore[0] = new java.util.LinkedList<>();
        mode.netRankingScore[0].add(50000);
        mode.netRankingLines[0] = new java.util.LinkedList<>();
        mode.netRankingLines[0].add(200);
        mode.netRankingPiece[0] = new java.util.LinkedList<>();
        mode.netRankingPiece[0].add(100);
        mode.netRankingPPS[0] = new java.util.LinkedList<>();
        mode.netRankingPPS[0].add(2.0f);
        mode.netRankingSPL[0] = new java.util.LinkedList<>();
        mode.netRankingSPL[0].add(250.0);
        mode.netRankingRollclear[0] = new java.util.LinkedList<>();
        mode.netRankingRollclear[0].add(0);
        mode.netRankingNoDataFlag = new boolean[]{false, false};
        mode.netRankingReady = new boolean[]{true, true};
        mode.netRankingCursor = new int[]{0, 0};
        mode.netRankingMyRank = new int[]{-1, -1};
        mode.netRankingType = NetSPRecord.RANKINGTYPE_GENERIC_SCORE;

        mode.netOnRenderNetPlayRanking(engine, 0, receiver);
    }

    // ────────────────────────────────────────────────────────────────
    // netDrawSpectatorsCount with ranking
    // ────────────────────────────────────────────────────────────────
    @Test
    void netDrawSpectatorsCountSettingWithRanking() throws Exception {
        mode.netIsNetPlay = true;
        mode.netIsWatch = false;
        mode.netNumSpectators = 1;
        mode.netIsNetRankingDisplayMode = false;
        mode.owner = manager;
        engine.stat = GameEngine.Status.SETTING;

        mode.netDrawSpectatorsCount(engine, 0, 0);
    }

    // ────────────────────────────────────────────────────────────────
    // netOnUpdateNetPlayRanking - button D (enter ranking)
    // ────────────────────────────────────────────────────────────────
	@Test
	void netOnUpdateNetPlayRankingEnter() throws Exception {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingNoDataFlag = new boolean[]{false, false};
		mode.netRankingReady = new boolean[]{true, true};
		mode.netRankingPlace[0] = new java.util.LinkedList<>();
		mode.netRankingPlace[0].add(0);
		mode.netRankingName[0] = new java.util.LinkedList<>();
		mode.netRankingName[0].add("player1");
		mode.netRankingCursor = new int[]{0, 0};
		mode.netRankingMyRank = new int[]{0, 0};
		mode.netRankingType = NetSPRecord.RANKINGTYPE_GENERIC_SCORE;
		// Set up netCurrentRoomInfo and netLobby to avoid NPE
		mode.netCurrentRoomInfo = new NetRoomInfo();
		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new NetPlayerClient();

		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.netOnUpdateNetPlayRanking(engine, 0);
		assertFalse(mode.netIsNetRankingDisplayMode);
	}

    // ================================================================
    // Helpers
    // ================================================================

    private static Method findMethod(Class<?> cls, String name, Class<?>... paramTypes) throws NoSuchMethodException {
        Class<?> c = cls;
        while (c != null) {
            try { return c.getDeclaredMethod(name, paramTypes); }
            catch (NoSuchMethodException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchMethodException(name + " in " + cls.getName());
    }
}
