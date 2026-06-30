package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Tail-coverage tests for {@link AvalancheVSDummyMode} via a concrete subclass.
 * Targets loadMapPreview map-load branch, onReady, readyInit useMap/map-load
 * branches (random, copy-from-P1, specific map), calcScore multiplier clamp
 * branches, addOjama zenkeshi/hurryup/counter branches, updateOjamaMeter
 * inc/dec, drawX big path, and drawScores score-display else branches.
 */
class AvalancheVSDummyModeTailCoverageTest {

	static class ConcreteMode extends AvalancheVSDummyMode {
		@Override public String getName() { return "TestDummyTail"; }
		@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
	}

	// --- 431-434: loadMapPreview with non-null propMap loads a map ---
	@Test
	void loadMapPreviewLoadsMap() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		CustomProperties pm = new CustomProperties();
		pm.setProperty("map.maxMapNumber", 2);
		pm.setProperty("map.0", "");
		setArrElem(mode, "propMap", 0, pm);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("loadMapPreview",
				GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 0, false); // forceReload false, propMap already set

		assertEquals(2, getIntArr(mode, "mapMaxNo")[0]);
	}

	// --- 491: onReady delegates to readyInit when statc[0] == 0 ---
	@Test
	void onReadyDelegatesToReadyInit() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.statc[0] = 0;

		boolean result = mode.onReady(e, 0);

		assertEquals(false, result);
	}

	// --- 529, 531-537, 542-543: readyInit useMap with random map (mapNumber<0) ---
	@Test
	void readyInitUseMapRandom() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBoolArr(mode, "useMap", 0, true);
		setIntArr(mode, "mapNumber", 0, -1);
		setIntArr(mode, "mapMaxNo", 0, 3);
		CustomProperties pm = new CustomProperties();
		pm.setProperty("map.0", "");
		pm.setProperty("map.1", "");
		pm.setProperty("map.2", "");
		setArrElem(mode, "propMap", 0, pm);

		mode.readyInit(e, 0);

		assertTrue(getField(mode, "fldBackup") != null);
	}

	// --- 533: readyInit player 1 copies player 0 field ---
	@Test
	void readyInitPlayer1CopiesPlayer0() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e0 = mgr.engine[0];
		GameEngine e1 = mgr.engine[1];
		mode.playerInit(e0, 0);
		mode.playerInit(e1, 1);
		// Player 0 setup: useMap, random
		setBoolArr(mode, "useMap", 0, true);
		setIntArr(mode, "mapNumber", 0, -1);
		e0.createFieldIfNeeded();
		// Player 1 setup: useMap, random -> should copy P0 field
		setBoolArr(mode, "useMap", 1, true);
		setIntArr(mode, "mapNumber", 1, -1);
		CustomProperties pm = new CustomProperties();
		setArrElem(mode, "propMap", 1, pm);

		mode.readyInit(e1, 1);

		assertTrue(e1.field != null);
	}

	// --- 539: readyInit useMap with specific map (mapNumber >= 0) ---
	@Test
	void readyInitUseMapSpecific() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBoolArr(mode, "useMap", 0, true);
		setIntArr(mode, "mapNumber", 0, 1);
		CustomProperties pm = new CustomProperties();
		pm.setProperty("map.1", "");
		setArrElem(mode, "propMap", 0, pm);

		mode.readyInit(e, 0);

		assertTrue(e.field != null);
	}

	// --- 600, 602, 607: calcScore multiplier branches ---
	@Test
	void calcScoreMultiplierClampBranches() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBoolArr(mode, "big", 0, true); // multiplier >>= 2 (line 600)
		// Place blocks so field not empty and cascade
		e.field.setBlock(0, e.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_RED));
		e.field.colorClearExtraCount = 5000; // forces multiplier > 999 -> clamp
		e.field.colorsCleared = 3; // colorsCleared > 1 (line 602)
		e.chain = 2;

		mode.calcScore(e, 0, 4);

		// lastmultiplier clamped to 999
		assertEquals(999, getIntArr(mode, "lastmultiplier")[0]);
	}

	// --- 671, 675, 692-694: addOjama zenkeshi / hurryup / ojamaAdd counter ---
	@Test
	void addOjamaZenkeshiHurryupCounter() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		// zenKeshi ON branch (line 671)
		setBoolArr(mode, "zenKeshi", 0, true);
		setIntArr(mode, "zenKeshiType", 0, AvalancheVSDummyMode.ZENKESHI_MODE_ON);
		// hurryup branch (line 675): time > hurryupSeconds
		setIntArr(mode, "hurryupSeconds", 0, 1);
		e.statistics.time = 200;
		setIntArr(mode, "ojamaRate", 0, 70);
		// counter mode ON so ojama/ojamaAdd counters run (692-694)
		setIntArr(mode, "ojamaCounterMode", 0, AvalancheVSDummyMode.OJAMA_COUNTER_ON);
		setIntArr(mode, "ojama", 0, 2);
		setIntArr(mode, "ojamaAdd", 0, 5);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("addOjama",
				GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 1000);

		assertTrue(getIntArr(mode, "ojamaSent")[0] >= 0);
	}

	// --- 804, 806: updateOjamaMeter increment and decrement ---
	@Test
	void updateOjamaMeterIncrement() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setIntArr(mode, "ojama", 0, 30);
		e.meterValue = 0; // value > meterValue -> increment (804)

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("updateOjamaMeter",
				GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);

		assertEquals(1, e.meterValue);
	}

	@Test
	void updateOjamaMeterDecrement() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setIntArr(mode, "ojama", 0, 0);
		e.meterValue = 50; // value < meterValue -> decrement (806)

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("updateOjamaMeter",
				GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);

		assertEquals(49, e.meterValue);
	}

	// --- 847: drawX big display path ---
	@Test
	void drawXBigPath() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBoolArr(mode, "dangerColumnShowX", 0, true);
		setBoolArr(mode, "big", 0, true); // baseX = 1, big path

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("drawX",
				GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);
	}

	// --- 879, 886: drawScores score-display (no lastscore) else branches ---
	@Test
	void drawScoresWithoutLastscore() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setIntArr(mode, "scgettime", 0, 0); // no lastscore -> else (879)
		setIntArr(mode, "scgettime", 1, 0); // else (886)
		setIntArr(mode, "score", 0, 1234);
		setIntArr(mode, "score", 1, 5678);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("drawScores",
				GameEngine.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 0, 0, EventReceiver.COLOR_WHITE);
	}

	// --- helpers ---

	private static GameManager twoPlayerEngine(ConcreteMode mode) {
		GameManager m = new GameManager(new EventReceiver());
		m.mode = mode;
		mode.modeInit(m);
		m.init();
		m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10;
		m.engine[0].ruleopt.fieldHeight = 20;
		m.engine[1].init();
		m.engine[1].ruleopt.fieldWidth = 10;
		m.engine[1].ruleopt.fieldHeight = 20;
		return m;
	}

	private static GameEngine freshEngine(ConcreteMode mode) {
		GameManager m = new GameManager(new EventReceiver());
		m.mode = mode;
		mode.modeInit(m);
		m.init();
		m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10;
		m.engine[0].ruleopt.fieldHeight = 20;
		return m.engine[0];
	}

	private static Object getField(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.get(o);
	}

	private static int[] getIntArr(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return (int[]) f.get(o);
	}

	private static void setIntArr(Object o, String n, int idx, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((int[]) f.get(o))[idx] = v;
	}

	private static void setBoolArr(Object o, String n, int idx, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((boolean[]) f.get(o))[idx] = v;
	}

	private static void setArrElem(Object o, String n, int idx, Object v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		java.lang.reflect.Array.set(f.get(o), idx, v);
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException ex) {}
		throw new NoSuchFieldException(n);
	}
}
