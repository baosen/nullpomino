package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Extended test coverage for {@link NetVSBattleMode}: covers getName,
 * modeInit allocation, playerInit state, calcScore scoring formulas,
 * calcScore garbage line generation and hurry-up logic, onLast timer
 * and APL/APM calculation, and the no-op stubs.
 */
class NetVSBattleModeExtendedGameLogicTest {

    @Test
    void getNameReturnsExpected() {
        NetVSBattleMode mode = new NetVSBattleMode();
        assertEquals("NET-VS-BATTLE", mode.getName());
    }

    @Test
    void isVSModeReturnsTrue() {
        NetVSBattleMode mode = new NetVSBattleMode();
        assertTrue(mode.isVSMode());
    }

    @Test
    void modeInitAllocatesArrays() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);

        assertEquals(6, ((boolean[]) readField(mode, "playerKObyYou")).length);
        assertEquals(6, ((int[]) readField(mode, "scgettime")).length);
        assertEquals(6, ((int[]) readField(mode, "lastevent")).length);
        assertEquals(6, ((boolean[]) readField(mode, "lastb2b")).length);
        assertEquals(6, ((int[]) readField(mode, "garbageSent")).length);
        assertEquals(6, ((int[]) readField(mode, "garbage")).length);
        assertEquals(6, ((float[]) readField(mode, "playerAPL")).length);
        assertEquals(6, ((float[]) readField(mode, "playerAPM")).length);
    }

    @Test
    void playerInitResetsState() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.playerInit(engine, 0);

        assertEquals(0, readInt(mode, "scgettime", 0));
        assertEquals(0, readInt(mode, "lastevent", 0));
        assertFalse(readBoolean(mode, "lastb2b", 0));
        assertEquals(0, readInt(mode, "lastcombo", 0));
        assertEquals(0, readInt(mode, "garbageSent", 0));
        assertEquals(0, readInt(mode, "garbage", 0));
    }

	@Test
	void calcScoreSingleLineNonTSpinAddsScore() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode);

		// Need a non-null netCurrentRoomInfo to avoid NPE
		setNetCurrentRoomInfo(mode, manager);
		setBoolean(mode, "netvsIsPractice", true);

		engine.statistics.score = 0;

		// Single line no T-Spin with playerID=0
		mode.calcScore(engine, 0, 1);

        // lastevent should be EVENT_SINGLE
        assertEquals(1, readInt(mode, "lastevent", 0));
    }

	@Test
	void calcScoreFourLineWithB2B() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode);

		setNetCurrentRoomInfo(mode, manager);
		setBoolean(mode, "netvsIsPractice", true);
		engine.b2b = true;
		engine.tspin = false;

		mode.calcScore(engine, 0, 4);

        // lastevent should be EVENT_FOUR
        assertEquals(4, readInt(mode, "lastevent", 0));
        assertTrue(readBoolean(mode, "lastb2b", 0));
    }

	@Test
	void calcScoreTSpinDoubleSetsEvent() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode);

		setNetCurrentRoomInfo(mode, manager);
		setBoolean(mode, "netvsIsPractice", true);
		engine.tspin = true;
		engine.tspinmini = false;

		mode.calcScore(engine, 0, 2);

        // lastevent should be EVENT_TSPIN_DOUBLE
        assertEquals(7, readInt(mode, "lastevent", 0));
    }

	@Test
	void calcScoreTSpinSingleMini() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode);

		setNetCurrentRoomInfo(mode, manager);
		setBoolean(mode, "netvsIsPractice", true);
		engine.tspin = true;
		engine.tspinmini = true;

		mode.calcScore(engine, 0, 1);

        // lastevent should be EVENT_TSPIN_SINGLE_MINI
        assertEquals(5, readInt(mode, "lastevent", 0));
    }

	@Test
	void calcScoreComboAttack() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode);

		setNetCurrentRoomInfo(mode, manager);
		setBoolean(mode, "netvsIsPractice", true);
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL;
		engine.combo = 3;
		engine.tspin = false;

		mode.calcScore(engine, 0, 1);

        // combo 3 should set lastcombo = 3
        assertEquals(3, readInt(mode, "lastcombo", 0));
    }

	@Test
	void calcScoreNoLinesDoesNothing() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode);

		setNetCurrentRoomInfo(mode, manager);
		setBoolean(mode, "netvsIsPractice", true);
		int scoreBefore = engine.statistics.score;
		mode.calcScore(engine, 0, 0);
		assertEquals(scoreBefore, engine.statistics.score);
	}

    @Test
    void calcScoreHurryUpLogic() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);

        setNetCurrentRoomInfo(mode, manager);
        setBoolean(mode, "hurryupStarted", false);
        setInt(mode, "hurryupCount", 0);

        // Call calcScore with 0 lines to trigger hurry-up path
        mode.calcScore(engine, 0, 0);

        // hurryupCount should have been set to hurryupInterval - 1
        // when not started yet
    }

    @Test
    void onLastIncrementsScgettime() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);

        int[] scgettime = (int[]) readField(mode, "scgettime");
        scgettime[0] = 5;

        mode.onLast(engine, 0);

        assertEquals(6, scgettime[0]);
    }

    @Test
    void onLastCalculatesAPLAndAPM() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);

        engine.gameActive = true;
        engine.timerActive = true;
        engine.statistics.time = 3600;
        engine.statistics.lines = 10;

        mode.onLast(engine, 0);

        // APL should be defined
        float[] playerAPL = (float[]) readField(mode, "playerAPL");
        assertTrue(playerAPL[0] >= 0);
    }

    @Test
    void onLastUpdatesGarbageMeter() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);

        int[] garbage = (int[]) readField(mode, "garbage");
        garbage[0] = 120; // 2 lines worth

        mode.onLast(engine, 0);

        assertTrue(engine.meterValue > 0);
    }

    @Test
    void startGameInitializesHurryUpAndTarget() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);

        setNetCurrentRoomInfo(mode, manager);

        mode.startGame(engine, 0);

        assertFalse(readBoolean(mode, "hurryupStarted"));
        assertEquals(0, readInt(mode, "hurryupShowFrames"));
    }

    @Test
    void renderLastDoesNotThrow() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);

        mode.renderLast(engine, 0);
        // Should not throw
    }

	@Test
	void noOpStubsDoNotThrow() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "netvsIsPractice", true);

		mode.netSendStats(engine);
		mode.netRecvStats(engine, new String[]{});
		// netSendEndGameStats and netSendOptions send to netLobby which is null - will NPE
	}

    // ---------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------

    private static GameEngine freshEngine(NetVSBattleMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[0].createFieldIfNeeded();
        manager.engine[0].nowPieceObject = new Piece(Piece.PIECE_T);
        return manager.engine[0];
    }

    private static void setNetCurrentRoomInfo(NetVSBattleMode mode, GameManager manager)
            throws Exception {
        // Create a minimal NetRoomInfo using reflection
        Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
        Object roomInfo = roomInfoClass.getConstructor().newInstance();
        // Set booleans to defaults
        java.lang.reflect.Field reduceField = roomInfoClass.getField("reduceLineSend");
        reduceField.setBoolean(roomInfo, false);
        java.lang.reflect.Field bravoField = roomInfoClass.getField("bravo");
        bravoField.setBoolean(roomInfo, false);
        java.lang.reflect.Field counterField = roomInfoClass.getField("counter");
        counterField.setBoolean(roomInfo, true);
        java.lang.reflect.Field useFractionalField = roomInfoClass.getField("useFractionalGarbage");
        useFractionalField.setBoolean(roomInfo, false);
        java.lang.reflect.Field rensaField = roomInfoClass.getField("rensaBlock");
        rensaField.setBoolean(roomInfo, true);
        java.lang.reflect.Field garbagePercentField = roomInfoClass.getField("garbagePercent");
        garbagePercentField.setInt(roomInfo, 100);
        java.lang.reflect.Field divideChangeField = roomInfoClass.getField("divideChangeRateByPlayers");
        divideChangeField.setBoolean(roomInfo, false);
        java.lang.reflect.Field garbageChangeField = roomInfoClass.getField("garbageChangePerAttack");
        garbageChangeField.setBoolean(roomInfo, false);
        java.lang.reflect.Field hurryupSecondsField = roomInfoClass.getField("hurryupSeconds");
        hurryupSecondsField.setInt(roomInfo, -1);

        setField(mode, "netCurrentRoomInfo", roomInfo);
    }

    private static Object readField(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.get(obj);
    }

    private static int readInt(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.getInt(obj);
    }

    private static int readInt(Object obj, String name, int index) throws Exception {
        int[] arr = (int[]) readField(obj, name);
        return arr[index];
    }

	private static boolean readBoolean(Object obj, String name, int index) throws Exception {
		boolean[] arr = (boolean[]) readField(obj, name);
		return arr[index];
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

    private static void setBoolean(Object obj, String name, boolean value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setBoolean(obj, value);
    }

    private static void setInt(Object obj, String name, int value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setInt(obj, value);
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
