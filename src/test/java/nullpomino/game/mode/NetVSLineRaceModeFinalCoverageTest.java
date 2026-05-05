package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers remaining uncovered lines in {@link NetVSLineRaceMode}:
 * calcScore race-win, renderLast string-length/place/game-count branches,
 * netSendStats, netSendEndGameStats, netvsRecvEndGameStats.
 */
class NetVSLineRaceModeFinalCoverageTest {

    private NetVSLineRaceMode mode;
    private GameManager manager;
    private GameEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        mode = new NetVSLineRaceMode();
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
    // calcScore race-win (non-practice)
    // ────────────────────────────────────────────────────────────────
    @Test
    void calcScoreRaceWinSendsMessage() throws Exception {
        mode.netvsIsPractice = false;
        set(mode, "goalLines", 5);
        engine.playerID = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();
        engine.statistics.lines = 5;

        mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
        mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();

        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsPlayerDead", new boolean[]{false, false, false, false, false, false});
        set(mode, "netvsPlayerUID", new int[]{100, -1, -1, -1, -1, -1});

        mode.calcScore(engine, 0, 1);
        assertEquals(GameEngine.Status.NOTHING, engine.stat);
    }

    // ────────────────────────────────────────────────────────────────
    // renderLast: string length display branches
    // ────────────────────────────────────────────────────────────────
    @Test
    void renderLastStringLength1Menu() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "goalLines", 40);
        engine.displaysize = 0;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;
        engine.statistics.lines = 35;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastStringLength2Menu() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "goalLines", 40);
        engine.displaysize = 0;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;
        engine.statistics.lines = 30;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastStringLength3Menu() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "goalLines", 200);
        engine.displaysize = 0;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;
        engine.statistics.lines = 100;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastStringLength1Direct() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "goalLines", 40);
        engine.displaysize = -1;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;
        engine.statistics.lines = 35;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastStringLength2Direct() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "goalLines", 40);
        engine.displaysize = -1;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;
        engine.statistics.lines = 30;

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastStringLength3Direct() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        set(mode, "goalLines", 200);
        engine.displaysize = -1;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;
        engine.statistics.lines = 100;

        mode.renderLast(engine, 0);
    }

    // ────────────────────────────────────────────────────────────────
    // renderLast: place rendering (menu)
    // ────────────────────────────────────────────────────────────────
    @Test
    void renderLastPlaceMenuAllRanks() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        engine.displaysize = 0;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        // Place 0 - 1ST
        engine.statistics.lines = 30;
        mode.renderLast(engine, 0);

        // Place 1 - 2ND (need another player with more lines)
        set(mode, "netvsPlayerExist", new boolean[]{true, true, false, false, false, false});
        set(mode, "netvsPlayerDead", new boolean[]{false, true, false, false, false, false});
        set(mode, "netvsPlayerPlace", new int[]{1, 0, 0, 0, 0, 0});
        engine.statistics.lines = 20;
        mode.renderLast(engine, 0);

        // Place 2 - 3RD
        set(mode, "netvsPlayerPlace", new int[]{2, 0, 0, 0, 0, 0});
        mode.renderLast(engine, 0);

        // Place 3 - 4TH
        set(mode, "netvsPlayerPlace", new int[]{3, 0, 0, 0, 0, 0});
        mode.renderLast(engine, 0);

        // Place 4 - 5TH
        set(mode, "netvsPlayerPlace", new int[]{4, 0, 0, 0, 0, 0});
        mode.renderLast(engine, 0);

        // Place 5 - 6TH
        set(mode, "netvsPlayerPlace", new int[]{5, 0, 0, 0, 0, 0});
        mode.renderLast(engine, 0);
    }

    // ────────────────────────────────────────────────────────────────
    // renderLast: place rendering (direct)
    // ────────────────────────────────────────────────────────────────
    @Test
    void renderLastPlaceDirectAllRanks() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
        set(mode, "netvsIsGameActive", true);
        engine.displaysize = -1;
        engine.isVisible = true;
        engine.stat = GameEngine.Status.MOVE;

        for (int place = 0; place <= 5; place++) {
            set(mode, "netvsPlayerPlace", new int[]{place, 0, 0, 0, 0, 0});
            engine.statistics.lines = 30 - place;
            mode.renderLast(engine, 0);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // renderLast: game count (else branch)
    // ────────────────────────────────────────────────────────────────
    @Test
    void renderLastGameCountMenu() throws Exception {
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
    void renderLastGameCountResult() throws Exception {
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
        mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
        mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();
        engine.statistics.lines = 30;
        engine.statistics.pps = 2.0f;
        engine.statistics.lpm = 60.0f;

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
        mode.owner = manager;
        mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
        mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();

        invoke(mode, "netSendEndGameStats", engine);
    }

    // ────────────────────────────────────────────────────────────────
    // netvsRecvEndGameStats
    // ────────────────────────────────────────────────────────────────
	@Test
	void netvsRecvEndGameStatsParses() throws Exception {
		set(mode, "netvsPlayerResultReceived", new boolean[]{false, false, false, false, false, false});

		manager.engine[1].init();
		manager.engine[1].createFieldIfNeeded();
		manager.engine[1].statistics.lines = 0;

		set(mode, "netvsPlayerSeatID", new int[]{0, 1, -1, -1, -1, -1});
		mode.netIsWatch = true;

		String[] msg = {"gstat", "", "1", "0", "0", "0", "200", "50.0", "400", "2.5", "1200", "0", "3", "5"};
		java.lang.reflect.Method m = NetVSLineRaceMode.class.getDeclaredMethod("netvsRecvEndGameStats", String[].class);
		m.setAccessible(true);
		m.invoke(mode, new Object[]{msg});
	}

    // ────────────────────────────────────────────────────────────────
    // getNowPlayerPlace with tie-breaking
    // ────────────────────────────────────────────────────────────────
    @Test
    void getNowPlayerPlaceTieBreakByPps() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, true, false, false, false, false});
        set(mode, "netvsPlayerDead", new boolean[]{false, false, false, false, false, false});
        manager.engine[0].statistics.lines = 20;
        manager.engine[0].statistics.pps = 1.5f;
        manager.engine[0].statistics.lpm = 50.0f;
        manager.engine[1].init();
        manager.engine[1].createFieldIfNeeded();
        manager.engine[1].statistics.lines = 20;
        manager.engine[1].statistics.pps = 2.0f;
        manager.engine[1].statistics.lpm = 60.0f;

        int place = invokeInt(mode, "getNowPlayerPlace", manager.engine[0], 0);
        // Engine 0 has same lines but lower pps, so place > 0
        assertTrue(place > 0, "Should be behind due to lower PPS");
    }

    @Test
    void getNowPlayerPlaceTieBreakByLpm() throws Exception {
        set(mode, "netvsPlayerExist", new boolean[]{true, true, false, false, false, false});
        set(mode, "netvsPlayerDead", new boolean[]{false, false, false, false, false, false});
        manager.engine[0].statistics.lines = 20;
        manager.engine[0].statistics.pps = 2.0f;
        manager.engine[0].statistics.lpm = 50.0f;
        manager.engine[1].init();
        manager.engine[1].createFieldIfNeeded();
        manager.engine[1].statistics.lines = 20;
        manager.engine[1].statistics.pps = 2.0f;
        manager.engine[1].statistics.lpm = 60.0f;

        int place = invokeInt(mode, "getNowPlayerPlace", manager.engine[0], 0);
        assertTrue(place > 0, "Should be behind due to lower LPM");
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

    private static int invokeInt(Object obj, String name, Object... args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
        for (int i = 0; i < args.length; i++) {
            if (types[i] == Integer.class) types[i] = int.class;
            else if (types[i] == Boolean.class) types[i] = boolean.class;
        }
        Method m = findMethod(obj.getClass(), name, types);
        m.setAccessible(true);
        return (int) m.invoke(obj, args);
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
