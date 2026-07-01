package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
 * Branch-coverage tests for {@link AvalancheVSDummyMode} driven through a
 * concrete subclass (so the base-class implementations of {@code readyInit},
 * {@code onLast}, {@code renderLast}, etc. are reached rather than the
 * {@link AvalancheVSMode} overrides).
 *
 * <p>Covers the previously-untaken sides of:
 * <ul>
 *   <li>L352/L515 loadOtherSetting/readyInit feverMapSet bounds (skip branch);</li>
 *   <li>L423 loadMapPreview propMap-non-null &amp; !forceReload skip;</li>
 *   <li>L439 loadMapSetFever propFeverMap-non-null &amp; !forceReload skip;</li>
 *   <li>L496/L497/L502 readyInit cascadeSlow / bigDisplay / outline-NONE;</li>
 *   <li>L532/L535 readyInit P1 useMap-off &amp; random map with mapMaxNo&lt;1;</li>
 *   <li>L667/L670/L674/L684/L690/L697 addOjama enemy/zenkeshi/hurryup/counter;</li>
 *   <li>L717/L721 gameOverCheck big and double-danger-column;</li>
 *   <li>L763/L768/L774 onLast settlement 1P-win / 2P-win / game-ended;</li>
 *   <li>L799/L801 updateOjamaMeter RED / YELLOW;</li>
 *   <li>L823/L824/L826 renderLast chain (&gt;9) &amp; zenkeshi display;</li>
 *   <li>L844/L845 drawX double danger column;</li>
 *   <li>L875/L882 drawScores recent-score display;</li>
 *   <li>L892/L895/L898/L900 drawOjama pending-ojama display.</li>
 * </ul>
 */
class AvalancheVSDummyModeBranchCoverageTest {

	static class ConcreteMode extends AvalancheVSDummyMode {
		@Override public String getName() { return "TestDummyBranch"; }
		@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
	}

	// -----------------------------------------------------------------
	// L352 / L515 - feverMapSet bounds skip loadMapSetFever
	// -----------------------------------------------------------------

	@Test
	void loadOtherSettingSkipsFeverMapWhenSetNegative() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevs.feverMapSet.p0", -1); // < 0 -> first cond false

		invokeLoadOther(mode, e, prop, "");

		assertEquals(-1, getIntArr(mode, "feverMapSet")[0]);
		// loadMapSetFever skipped -> propFeverMap stays null
		assertTrue(((CustomProperties[]) getField(mode, "propFeverMap"))[0] == null);
	}

	@Test
	void loadOtherSettingSkipsFeverMapWhenSetTooLarge() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		// >= FEVER_MAPS.length -> first cond true, second cond false
		prop.setProperty("avalanchevs.feverMapSet.p0", AvalancheVSDummyMode.FEVER_MAPS.length);

		invokeLoadOther(mode, e, prop, "");

		assertEquals(AvalancheVSDummyMode.FEVER_MAPS.length, getIntArr(mode, "feverMapSet")[0]);
		assertTrue(((CustomProperties[]) getField(mode, "propFeverMap"))[0] == null);
	}

	@Test
	void readyInitSkipsFeverMapWhenSetOutOfRange() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBoolArr(mode, "big", 0, false);       // reach the else-if at L515
		setIntArr(mode, "feverMapSet", 0, 99);   // out of range -> skip loadMapSetFever
		setBoolArr(mode, "useMap", 0, false);

		mode.readyInit(e, 0);

		// no fever map loaded
		assertTrue(((CustomProperties[]) getField(mode, "propFeverMap"))[0] == null);
	}

	// -----------------------------------------------------------------
	// L423 - loadMapPreview: propMap non-null AND !forceReload -> skip reload
	// -----------------------------------------------------------------

	@Test
	void loadMapPreviewSkipsReloadWhenPropMapPresent() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		CustomProperties pm = new CustomProperties();
		pm.setProperty("map.maxMapNumber", 4);
		pm.setProperty("map.0", "");
		setArrElem(mode, "propMap", 0, pm);
		setIntArr(mode, "mapMaxNo", 0, 0); // will be overwritten from prop, proving block ran

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("loadMapPreview",
				GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 0, false); // !forceReload + propMap present -> skip reload branch

		// The reload branch (which would reset mapMaxNo to 0 then reload) is skipped;
		// propMap identity is preserved.
		assertSame(pm, ((CustomProperties[]) getField(mode, "propMap"))[0]);
		// second if still runs and reads maxMapNumber from the retained propMap
		assertEquals(4, getIntArr(mode, "mapMaxNo")[0]);
	}

	// -----------------------------------------------------------------
	// L439 - loadMapSetFever: propFeverMap non-null AND !forceReload -> skip
	// -----------------------------------------------------------------

	@Test
	void loadMapSetFeverSkipsReloadWhenAlreadyLoaded() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		CustomProperties fm = new CustomProperties();
		fm.setProperty("sets", "a,b");
		setArrElem(mode, "propFeverMap", 0, fm);
		setIntArr(mode, "feverChainMin", 0, 7);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("loadMapSetFever",
				GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 0, false); // !forceReload + already loaded -> skip block

		// block skipped: propFeverMap kept, feverChainMin untouched
		assertSame(fm, ((CustomProperties[]) getField(mode, "propFeverMap"))[0]);
		assertEquals(7, getIntArr(mode, "feverChainMin")[0]);
	}

	@Test
	void loadMapSetFeverReloadsWhenForced() throws Exception {
		// Confirms the taken side too, using a real map file (read-only).
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("loadMapSetFever",
				GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 0, true); // Fever.map

		assertTrue(((CustomProperties[]) getField(mode, "propFeverMap"))[0] != null);
	}

	// -----------------------------------------------------------------
	// L496 / L497 / L502 - readyInit cascadeSlow / bigDisplay / outline NONE
	// -----------------------------------------------------------------

	@Test
	void readyInitCascadeSlowBigDisplayOutlineNone() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBoolArr(mode, "cascadeSlow", 0, true); // L496 CASCADE_SLOW
		setBool(mode, "bigDisplay", true);         // L497 displaysize = 1
		setIntArr(mode, "outlineType", 0, 2);      // L502 BLOCK_OUTLINE_NONE
		setBoolArr(mode, "big", 0, false);
		setIntArr(mode, "feverMapSet", 0, -1);     // skip fever load
		setBoolArr(mode, "useMap", 0, false);

		mode.readyInit(e, 0);

		assertEquals(GameEngine.LineGravity.CASCADE_SLOW, e.lineGravityType);
		assertEquals(1, e.displaysize);
		assertEquals(GameEngine.BLOCK_OUTLINE_NONE, e.blockOutlineType);
	}

	// -----------------------------------------------------------------
	// L532 - readyInit P1: useMap[0] false -> skip copy, take random branch
	// L535 - random map with mapMaxNo < 1 -> no = 0
	// -----------------------------------------------------------------

	@Test
	void readyInitPlayer1RandomWhenPlayer0MapOff() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e1 = mgr.engine[1];
		mode.playerInit(mgr.engine[0], 0);
		mode.playerInit(e1, 1);
		setBoolArr(mode, "useMap", 0, false);  // second condition false at L532
		setBoolArr(mode, "useMap", 1, true);
		setIntArr(mode, "mapNumber", 1, -1);
		setIntArr(mode, "mapMaxNo", 1, 0);     // L535 mapMaxNo < 1 -> no = 0
		CustomProperties pm = new CustomProperties();
		pm.setProperty("map.0", "");
		setArrElem(mode, "propMap", 1, pm);

		mode.readyInit(e1, 1);

		assertTrue(e1.field != null);
		assertTrue(getFieldArr(mode, "fldBackup")[1] != null);
	}

	// -----------------------------------------------------------------
	// L667 - addOjama enemyID stays 0 when playerID == 1
	// L670 - zenKeshi true but type != ON (false-of-second)
	// L674 - hurryupSeconds > 0 but time <= hurryupSeconds (false-of-second)
	// L697 - ojamaNew == 0 -> no add to enemy
	// -----------------------------------------------------------------

	@Test
	void addOjamaPlayer1ZenkeshiNotOnNoHurryup() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[1];
		mode.playerInit(mgr.engine[0], 0);
		mode.playerInit(e, 1);
		// zenKeshi true but FEVER type -> L670 second condition false
		setBoolArr(mode, "zenKeshi", 1, true);
		setIntArr(mode, "zenKeshiType", 1, AvalancheVSDummyMode.ZENKESHI_MODE_FEVER);
		// hurryup enabled but time not past threshold -> L674 second condition false
		setIntArr(mode, "hurryupSeconds", 1, 100);
		e.statistics.time = 10;
		setIntArr(mode, "ojamaRate", 1, 100);

		int before = getIntArr(mode, "ojamaAdd")[0];

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("addOjama",
				GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 1, 500); // pts=500, rate=100 -> ojamaNew=5

		// enemyID stayed 0 (playerID==1) so enemy 0 gained ojama
		assertTrue(getIntArr(mode, "ojamaAdd")[0] > before);
	}

	@Test
	void addOjamaZeroNewNoEnemyAdd() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		setIntArr(mode, "ojamaRate", 0, 100);
		// counter mode ON, huge pending ojama fully counters the new ojama -> ojamaNew==0
		setIntArr(mode, "ojamaCounterMode", 0, AvalancheVSDummyMode.OJAMA_COUNTER_ON);
		setIntArr(mode, "ojama", 0, 100);   // L684 true
		setIntArr(mode, "ojamaAdd", 0, 100);

		int enemyBefore = getIntArr(mode, "ojamaAdd")[1];

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("addOjama",
				GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 100); // pts=100 rate=100 -> ojamaNew=1, countered to 0

		// L697 ojamaNew == 0 -> enemy unchanged
		assertEquals(enemyBefore, getIntArr(mode, "ojamaAdd")[1]);
	}

	@Test
	void addOjamaCounterOjamaZeroButOjamaAddCounters() throws Exception {
		// ojama[playerID] == 0 -> L684 first cond false; ojamaAdd counter L690 runs.
		ConcreteMode mode = new ConcreteMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		setIntArr(mode, "ojamaRate", 0, 100);
		setIntArr(mode, "ojamaCounterMode", 0, AvalancheVSDummyMode.OJAMA_COUNTER_ON);
		setIntArr(mode, "ojama", 0, 0);     // L684 first cond false
		setIntArr(mode, "ojamaAdd", 0, 3);  // L690 counters part

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("addOjama",
				GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 1000); // ojamaNew = 10, ojamaAdd(3) countered -> 7 to enemy

		assertEquals(0, getIntArr(mode, "ojamaAdd")[0]);
		assertEquals(7, getIntArr(mode, "ojamaAdd")[1]);
	}

	// -----------------------------------------------------------------
	// L717 - gameOverCheck big path: block at (1,0) -> GAMEOVER
	// -----------------------------------------------------------------

	@Test
	void gameOverCheckBigColumnFilled() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBoolArr(mode, "big", 0, true);
		e.field.setBlock(1, 0, new Block(Block.BLOCK_COLOR_RED)); // not empty -> gameover
		e.stat = GameEngine.Status.MOVE;

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("gameOverCheck",
				GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);

		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}

	// -----------------------------------------------------------------
	// L721 - gameOverCheck double danger column: block at (3,0) -> GAMEOVER
	// -----------------------------------------------------------------

	@Test
	void gameOverCheckDoubleDangerColumn() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBoolArr(mode, "big", 0, false);
		setBoolArr(mode, "dangerColumnDouble", 0, true);
		// (2,0) empty so first disjunct false; (3,0) filled -> second disjunct true
		e.field.setBlock(3, 0, new Block(Block.BLOCK_COLOR_RED));
		e.stat = GameEngine.Status.MOVE;

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("gameOverCheck",
				GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);

		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}

	// -----------------------------------------------------------------
	// L763 / L774 - onLast settlement: 1P win (p2Lose && !p1Lose)
	// -----------------------------------------------------------------

	@Test
	void onLastSettlementOnePlayerWins() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameManager mgr = twoPlayerEngine(mode);
		mode.playerInit(mgr.engine[0], 0);
		mode.playerInit(mgr.engine[1], 1);
		mgr.engine[0].gameActive = true;
		mgr.engine[0].stat = GameEngine.Status.MOVE;      // p1 not lose
		mgr.engine[1].stat = GameEngine.Status.GAMEOVER;  // p2 lose

		mode.onLast(mgr.engine[1], 1);

		assertEquals(0, getInt(mode, "winnerID"));
		assertEquals(GameEngine.Status.EXCELLENT, mgr.engine[0].stat);
		assertEquals(GameEngine.Status.GAMEOVER, mgr.engine[1].stat);
	}

	// -----------------------------------------------------------------
	// L768 / L774 - onLast settlement: 2P win (p1Lose && !p2Lose)
	// -----------------------------------------------------------------

	@Test
	void onLastSettlementTwoPlayerWins() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameManager mgr = twoPlayerEngine(mode);
		mode.playerInit(mgr.engine[0], 0);
		mode.playerInit(mgr.engine[1], 1);
		mgr.engine[0].gameActive = true;
		mgr.engine[0].stat = GameEngine.Status.GAMEOVER;  // p1 lose
		mgr.engine[1].stat = GameEngine.Status.MOVE;      // p2 not lose

		mode.onLast(mgr.engine[1], 1);

		assertEquals(1, getInt(mode, "winnerID"));
		assertEquals(GameEngine.Status.GAMEOVER, mgr.engine[0].stat);
		assertEquals(GameEngine.Status.EXCELLENT, mgr.engine[1].stat);
	}

	// -----------------------------------------------------------------
	// L799 - updateOjamaMeter RED (ojama >= 5*width)
	// -----------------------------------------------------------------

	@Test
	void updateOjamaMeterRed() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		int width = e.field.getWidth();
		setIntArr(mode, "ojama", 0, 5 * width); // >= 5*width -> RED

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("updateOjamaMeter",
				GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);

		assertEquals(GameEngine.METER_COLOR_RED, e.meterColor);
	}

	// -----------------------------------------------------------------
	// L801 - updateOjamaMeter YELLOW (1 <= ojama < width)
	// -----------------------------------------------------------------

	@Test
	void updateOjamaMeterYellow() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setIntArr(mode, "ojama", 0, 1); // >= 1 but < width -> YELLOW

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("updateOjamaMeter",
				GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);

		assertEquals(GameEngine.METER_COLOR_YELLOW, e.meterColor);
	}

	// -----------------------------------------------------------------
	// L823 / L824 / L826 - renderLast chain display (chain>9) + zenkeshi
	// -----------------------------------------------------------------

	@Test
	void renderLastChainAndZenkeshiDisplay() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		e.gameActive = true;
		e.chain = 12;                                 // L823 chain>0, L824 chain>9 -> 0
		setIntArr(mode, "chainDisplay", 0, 30);
		setIntArr(mode, "chainDisplayType", 0, AvalancheVSDummyMode.CHAIN_DISPLAY_YELLOW);
		setBoolArr(mode, "zenKeshi", 0, true);        // L826 zenkeshi true

		mode.renderLast(e, 0); // no-op receiver; just execute the branches

		assertTrue(e.chain > 9);
	}

	// -----------------------------------------------------------------
	// L844 / L845 - drawX double danger column, non-big (2 iterations)
	// -----------------------------------------------------------------

	@Test
	void drawXDoubleDangerColumn() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBoolArr(mode, "dangerColumnShowX", 0, true);
		setBoolArr(mode, "dangerColumnDouble", 0, true); // loop 2x
		setBoolArr(mode, "big", 0, false);
		e.displaysize = 0; // default draw path (L851)

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("drawX",
				GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);
	}

	// -----------------------------------------------------------------
	// L875 / L882 - drawScores recent-score display (scgettime path)
	// -----------------------------------------------------------------

	@Test
	void drawScoresRecentScoreDisplay() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setIntArr(mode, "scgettime", 0, 10);
		setIntArr(mode, "lastscore", 0, 100);
		setIntArr(mode, "lastmultiplier", 0, 5);
		setIntArr(mode, "scgettime", 1, 10);
		setIntArr(mode, "lastscore", 1, 200);
		setIntArr(mode, "lastmultiplier", 1, 6);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("drawScores",
				GameEngine.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 0, 0, EventReceiver.COLOR_WHITE);
	}

	// -----------------------------------------------------------------
	// L892 / L895 / L898 / L900 - drawOjama pending-ojama display
	// -----------------------------------------------------------------

	@Test
	void drawOjamaPendingDisplay() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setIntArr(mode, "ojamaAdd", 0, 4); // L892 true -> "(+4)"
		setIntArr(mode, "ojamaAdd", 1, 7); // L895 true -> "(+7)"
		setIntArr(mode, "ojama", 0, 3);    // L898 flash flag true
		setIntArr(mode, "ojama", 1, 9);    // L900 flash flag true

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("drawOjama",
				GameEngine.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 0, 0, EventReceiver.COLOR_WHITE);

		assertNotEquals(0, getIntArr(mode, "ojamaAdd")[0]);
	}

	// -----------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------

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

	private static void invokeLoadOther(AvalancheVSDummyMode mode, GameEngine engine,
			CustomProperties prop, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, name);
	}

	private static Object getField(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.get(o);
	}

	private static int getInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static void setBool(Object o, String n, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setBoolean(o, v);
	}

	private static int[] getIntArr(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return (int[]) f.get(o);
	}

	private static nullpomino.game.component.Field[] getFieldArr(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return (nullpomino.game.component.Field[]) f.get(o);
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
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException ex) { /* keep walking */ }
		throw new NoSuchFieldException(n);
	}
}
