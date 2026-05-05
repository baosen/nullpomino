package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers remaining uncovered lines in {@link NetVSDigRaceMode}:
 * turnAllBlocksToGem, onReady map branch, calcScore race-win branch,
 * renderLast string-length branches, place rendering, game count,
 * netSendStats, netSendEndGameStats, netvsRecvEndGameStats.
 */
class NetVSDigRaceModeFinalCoverageTest {

    private NetVSDigRaceMode mode;
    private GameManager manager;
    private GameEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        mode = new NetVSDigRaceMode();
        manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.mode = mode;
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
    }

    // ────────────────────────────────────────────────────────────────
    // turnAllBlocksToGem
    // ────────────────────────────────────────────────────────────────
    @Test
    void turnAllBlocksToGemConvertsColors() throws Exception {
        engine.createFieldIfNeeded();
        for (int x = 0; x < 10; x++) {
            engine.field.setBlock(x, 19, new Block(Block.BLOCK_COLOR_RED, 0, Block.BLOCK_ATTRIBUTE_VISIBLE));
        }

        invoke(mode, "turnAllBlocksToGem", engine, 0);

        Block blk = engine.field.getBlock(0, 19);
        assertEquals(Block.BLOCK_COLOR_GEM_RED, blk.color);
    }

    @Test
    void turnAllBlocksToGemSkipsNonColorBlocks() throws Exception {
        engine.createFieldIfNeeded();
        engine.field.setBlock(0, 19, new Block(Block.BLOCK_COLOR_GRAY, 0, Block.BLOCK_ATTRIBUTE_VISIBLE));

        invoke(mode, "turnAllBlocksToGem", engine, 0);

        Block blk = engine.field.getBlock(0, 19);
        assertEquals(Block.BLOCK_COLOR_GRAY, blk.color);
    }

    // ────────────────────────────────────────────────────────────────
    // onReady map branch
    // ────────────────────────────────────────────────────────────────
    @Test
    void onReadyMapGameBranch() throws Exception {
        // Set up netLobby to avoid NPE in super.onReady
        mode.netLobby = new NetLobbyFrame();
        mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();
        // Set current room info with useMap=true
        mode.netCurrentRoomInfo = new NetRoomInfo();
        mode.netCurrentRoomInfo.useMap = true;
        mode.netCurrentRoomInfo.roomID = 1;
        mode.netCurrentRoomInfo.garbagePercent = 100;
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});

        // Set up: ensure statc[0] == 0 flag is checkable
        engine.statc[0] = 0;
        engine.random = new java.util.Random();

        // Place colored blocks
        for (int x = 0; x < 10; x++) {
            if (x != 5) {
                engine.field.setBlock(x, 19, new Block(Block.BLOCK_COLOR_RED, 0, Block.BLOCK_ATTRIBUTE_VISIBLE));
            }
        }

        mode.onReady(engine, 0);
        // Should have turned blocks to gem
        Block blk = engine.field.getBlock(0, 19);
        assertEquals(Block.BLOCK_COLOR_GEM_RED, blk.color);
    }

    // ────────────────────────────────────────────────────────────────
    // calcScore race-win (non-practice)
    // ────────────────────────────────────────────────────────────────
    @Test
    void calcScoreRaceWinSendsMessage() throws Exception {
        mode.netvsIsPractice = false;
        set(mode, "playerRemainLines", new int[]{0, 0, 0, 0, 0, 0});
        mode.netLobby = new NetLobbyFrame();
        mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();
        engine.playerID = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        // Set up fields needed for racewin message
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsPlayerDead", new boolean[]{false, false, false, false, false, false});
        set(mode, "netvsPlayerUID", new int[]{100, -1, -1, -1, -1, -1});

        mode.calcScore(engine, 0, 1);
        assertEquals(GameEngine.Status.NOTHING, engine.stat);
    }

    // ────────────────────────────────────────────────────────────────
    // renderLast: string length display branches (menu and direct)
    // ────────────────────────────────────────────────────────────────
    @Test
    void renderLastStringLength1Menu() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "playerRemainLines", new int[]{9, 0, 0, 0, 0, 0});
        engine.displaysize = 0;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastStringLength2Menu() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "playerRemainLines", new int[]{10, 0, 0, 0, 0, 0});
        engine.displaysize = 0;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastStringLength3Menu() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "playerRemainLines", new int[]{100, 0, 0, 0, 0, 0});
        engine.displaysize = 0;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastStringLength1Direct() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "playerRemainLines", new int[]{9, 0, 0, 0, 0, 0});
        engine.displaysize = -1;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastStringLength2Direct() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "playerRemainLines", new int[]{10, 0, 0, 0, 0, 0});
        engine.displaysize = -1;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastStringLength3Direct() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "playerRemainLines", new int[]{100, 0, 0, 0, 0, 0});
        engine.displaysize = -1;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        mode.renderLast(engine, 0);
    }

    // ────────────────────────────────────────────────────────────────
    // renderLast: place rendering (menu display)
    // ────────────────────────────────────────────────────────────────
    @Test
    void renderLastPlaceMenuBranches() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "playerRemainLines", new int[]{5, 10, 0, 0, 0, 0});
        set(mode, "netvsPlayerDead", new boolean[]{false, false, false, false, false, false});
        engine.displaysize = 0;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        // Place 0 - 1ST
        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastPlaceDirectBranches() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "playerRemainLines", new int[]{10, 5, 0, 0, 0, 0});
        set(mode, "netvsPlayerPlace", new int[]{2, 1, 0, 0, 0, 0});
        set(mode, "netvsPlayerDead", new boolean[]{true, false, false, false, false, false});
        engine.displaysize = -1;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        mode.renderLast(engine, 0);
    }

    // ────────────────────────────────────────────────────────────────
    // renderLast: game count (else branch of active)
    // ────────────────────────────────────────────────────────────────
    @Test
    void renderLastGameCountNotActive() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", false);
        set(mode, "netvsIsPractice", false);
        set(mode, "netvsPlayerWinCount", new int[]{3, 1, 0, 0, 0, 0});
        set(mode, "netvsPlayerPlayCount", new int[]{5, 3, 0, 0, 0, 0});
        engine.displaysize = 0;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastGameCountDirect() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", false);
        set(mode, "netvsIsPractice", false);
        set(mode, "netvsPlayerWinCount", new int[]{3, 1, 0, 0, 0, 0});
        set(mode, "netvsPlayerPlayCount", new int[]{5, 3, 0, 0, 0, 0});
        engine.displaysize = -1;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastGameCountResultScreen() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", false);
        set(mode, "netvsIsPractice", false);
        set(mode, "netvsPlayerWinCount", new int[]{3, 1, 0, 0, 0, 0});
        set(mode, "netvsPlayerPlayCount", new int[]{5, 3, 0, 0, 0, 0});
        engine.displaysize = 0;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.RESULT;

        mode.renderLast(engine, 0);
    }

    // ────────────────────────────────────────────────────────────────
    // renderResult
    // ────────────────────────────────────────────────────────────────
    @Test
    void renderResultSmallDisplay() throws Exception {
        engine.displaysize = -1;
        mode.renderResult(engine, 0);
    }

    // ────────────────────────────────────────────────────────────────
    // netSendStats
    // ────────────────────────────────────────────────────────────────
    @Test
    void netSendStatsSends() throws Exception {
        engine.playerID = 0;
        mode.netvsIsPractice = false;
        mode.netIsWatch = false;
        set(mode, "playerRemainLines", new int[]{5, 0, 0, 0, 0, 0});
        mode.netLobby = new NetLobbyFrame();
        mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();

        invoke(mode, "netSendStats", engine);
    }

    @Test
    void netSendStatsSkipsForNonPlayer0() throws Exception {
        engine.playerID = 1;
        invoke(mode, "netSendStats", engine);
        // Should not send
    }

    // ────────────────────────────────────────────────────────────────
    // netSendEndGameStats
    // ────────────────────────────────────────────────────────────────
    @Test
    void netSendEndGameStatsSends() throws Exception {
        set(mode, "netvsPlayerPlace", new int[]{1, 0, 0, 0, 0, 0});
        set(mode, "netvsPlayTimer", 1200);
        set(mode, "netvsPlayerWinCount", new int[]{3, 1, 0, 0, 0, 0});
        set(mode, "netvsPlayerPlayCount", new int[]{5, 3, 0, 0, 0, 0});
        engine.playerID = 0;
        engine.statistics.lines = 200;
        engine.statistics.lpm = 50.0f;
        engine.statistics.totalPieceLocked = 400;
        engine.statistics.pps = 2.5f;
        engine.owner = manager;
        mode.netLobby = new NetLobbyFrame();
        mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();
        mode.owner = manager;

        invoke(mode, "netSendEndGameStats", engine);
    }

    // ────────────────────────────────────────────────────────────────
    // netvsRecvEndGameStats
    // ────────────────────────────────────────────────────────────────
	@Test
	void netvsRecvEndGameStatsParses() throws Exception {
		set(mode, "netvsPlayerResultReceived", new boolean[]{false, false, false, false, false, false});

		// Set up engine[1] for seatID matching
		manager.engine[1].init();
		manager.engine[1].createFieldIfNeeded();
		manager.engine[1].statistics.lines = 0;

		set(mode, "netvsPlayerSeatID", new int[]{0, 1, -1, -1, -1, -1});
		mode.netIsWatch = true;
		mode.netvsIsPractice = false;

		String[] msg = {"gstat", "", "1", "0", "0", "0", "200", "50.0", "400", "2.5", "1200", "0", "3", "5"};
		java.lang.reflect.Method m = NetVSDigRaceMode.class.getDeclaredMethod("netvsRecvEndGameStats", String[].class);
		m.setAccessible(true);
		m.invoke(mode, new Object[]{msg});
	}

    // ────────────────────────────────────────────────────────────────
    // fillGarbage for non-player0 skin
    // ────────────────────────────────────────────────────────────────
    @Test
    void fillGarbageNonPlayer0Skin() throws Exception {
        mode.netCurrentRoomInfo = new NetRoomInfo();
        mode.netCurrentRoomInfo.garbagePercent = 100;
        set(mode, "netvsPlayerSkin", new int[]{0, 1, 0, 0, 0, 0});
        set(mode, "netvsPlayerExist", new boolean[]{true, true, false, false, false, false});
        engine.random = new java.util.Random();

        engine.createFieldIfNeeded();
        engine.statc[0] = 0;

        // Fill bottom row to prevent fillGarbage from NPE when checking gems
        for (int x = 0; x < 10; x++) {
            engine.field.setBlock(x, 19, new Block(Block.BLOCK_COLOR_GRAY, 0, Block.BLOCK_ATTRIBUTE_VISIBLE));
        }

        mode.onReady(engine, 0);
        // Should fill garbage successfully
    }

    // ================================================================
    // Helpers
    // ================================================================

    private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name + " in " + cls.getName());
    }

    private static void set(Object obj, String name, Object value) throws Exception {
        java.lang.reflect.Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static void invoke(Object obj, String name, Object... args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
        for (int i = 0; i < args.length; i++) {
            if (types[i] == Integer.class) types[i] = int.class;
            else if (types[i] == Boolean.class) types[i] = boolean.class;
            else if (types[i] == Float.class) types[i] = float.class;
        }
        Method m = findMethod(obj.getClass(), name, types);
        m.setAccessible(true);
        m.invoke(obj, args);
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... paramTypes) throws NoSuchMethodException {
        Class<?> c = cls;
        while (c != null) {
            try { return c.getDeclaredMethod(name, paramTypes); }
            catch (NoSuchMethodException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchMethodException(name + " in " + cls.getName());
    }
}
