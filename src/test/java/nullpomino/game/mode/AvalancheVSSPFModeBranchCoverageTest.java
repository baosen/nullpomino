package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch coverage for uncovered paths in {@link AvalancheVSSPFMode}.
 * Targets renderLast countdown/hard block display, lineClearEnd
 * with countdown decrement, drop patterns, attack/defend multipliers,
 * loadOtherSetting, saveReplay, onLineClear ojama conversion,
 * and the readyInit wiring.
 */
class AvalancheVSSPFModeBranchCoverageTest {

	@Test
	void renderLastCountdownBlockDisplay() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		e.gameStarted = true;
		Block b = new Block(Block.BLOCK_COLOR_GRAY);
		b.countdown = 5;
		b.secondaryColor = Block.BLOCK_COLOR_BLUE;
		e.field.setBlock(2, 3, b);
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastHardBlockDisplay() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		e.gameStarted = true;
		Block b = new Block(Block.BLOCK_COLOR_GRAY);
		b.hard = 3;
		e.field.setBlock(2, 3, b);
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastOjamaAddDisplay() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		e.gameStarted = true;
		setIntArray(mode, "ojama", 0, 5);
		setIntArray(mode, "ojamaAdd", 0, 8);
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastScgetTimePositive() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		e.gameStarted = true;
		setIntArray(mode, "lastscore", 0, 100);
		setIntArray(mode, "lastmultiplier", 0, 2);
		setIntArray(mode, "scgettime", 0, 30);
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastDisplaysizeOne() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		e.gameStarted = true;
		e.displaysize = 1;
		mode.renderLast(e, 0);
	}

	@Test
	void lineClearEndCountdownDecrement() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		setIntArray(mode, "ojama", 0, 0);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		setIntArray(mode, "ojamaAdd", 1, 0);
		// Must set ojamaCountdown != 10 for countdown decrement to run
		setIntArray(mode, "ojamaCountdown", 0, 5);
		setBooleanArray(mode, "countdownDecremented", 0, false);
		Block b = new Block(Block.BLOCK_COLOR_GRAY);
		b.countdown = 3;
		e.field.setBlock(4, 3, b);
		mode.lineClearEnd(e, 0);
		assertEquals(2, e.field.getBlock(4, 3).countdown);
	}

	@Test
	void lineClearEndCountdownExpire() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		setIntArray(mode, "ojama", 0, 0);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		setIntArray(mode, "ojamaAdd", 1, 0);
		// Must set ojamaCountdown != 10 for countdown decrement to run
		setIntArray(mode, "ojamaCountdown", 0, 5);
		setBooleanArray(mode, "countdownDecremented", 0, false);
		Block b = new Block(Block.BLOCK_COLOR_GRAY);
		b.countdown = 1;
		b.secondaryColor = Block.BLOCK_COLOR_RED;
		b.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		b.hard = 4;
		e.field.setBlock(4, 3, b);
		mode.lineClearEnd(e, 0);
		assertEquals(0, e.field.getBlock(4, 3).countdown);
	}

	@Test
	void lineClearEndZenkeshiFever() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		setBooleanArray(mode, "zenKeshi", 0, true);
		setIntArray(mode, "zenKeshiType", 0, AvalancheVSDummyMode.ZENKESHI_MODE_FEVER);
		setIntArray(mode, "ojama", 0, 0);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		setIntArray(mode, "ojamaAdd", 1, 0);
		mode.lineClearEnd(e, 0);
		assertFalse(readBooleanArray(mode, "zenKeshi", 0));
	}

	@Test
	void lineClearEndOjamaDropPattern() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		setIntArray(mode, "ojama", 0, 20);
		setBooleanArray(mode, "ojamaDrop", 0, false);
		setBooleanArray(mode, "cleared", 0, false);
		setIntArray(mode, "maxAttack", 0, 2);
		setIntArray(mode, "ojamaHard", 0, 4);
		setIntArray(mode, "ojamaAdd", 1, 0);
		// Need dropPattern[enemyID] initialized (enemyID=1 when playerID=0)
		mode.readyInit(e, 0); // sets dropPattern[0]
		mode.readyInit(e, 1); // sets dropPattern[1]
		mode.lineClearEnd(e, 0);
		assertTrue(readBooleanArray(mode, "ojamaDrop", 0));
	}

	@Test
	void lineClearEndGameOverDoubleColumn() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		setIntArray(mode, "ojama", 0, 0);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		setIntArray(mode, "ojamaAdd", 1, 0);
		setBooleanArray(mode, "dangerColumnDouble", 0, true);
		e.field.setBlockColor(2, 0, Block.BLOCK_COLOR_RED);
		e.field.setBlockColor(3, 0, Block.BLOCK_COLOR_RED);
		mode.lineClearEnd(e, 0);
		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}

	@Test
	void lineClearEndOjamaAddTransfer() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		setIntArray(mode, "ojama", 0, 5);
		setIntArray(mode, "ojamaAdd", 1, 3);
		setIntArray(mode, "ojama", 1, 10);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		mode.lineClearEnd(e, 0);
		assertEquals(13, readIntArray(mode, "ojama", 1));
		assertEquals(0, readIntArray(mode, "ojamaAdd", 1));
	}

	@Test
	void onLineClearOjamaConversion() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		Block b = new Block(Block.BLOCK_COLOR_GRAY, 0,
				Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE);
		b.hard = 3;
		b.secondaryColor = Block.BLOCK_COLOR_RED;
		b.countdown = 5;
		e.field.setBlock(0, 0, b);
		mode.onLineClear(e, 0);
		Block after = e.field.getBlock(0, 0);
		assertEquals(0, after.hard);
		assertEquals(Block.BLOCK_COLOR_RED, after.color);
		assertEquals(0, after.countdown);
		assertFalse(after.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE));
	}

	@Test
	void onLineClearAlreadyCheckedSkips() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setBooleanArray(mode, "ojamaChecked", 0, true);
		assertFalse(mode.onLineClear(e, 0));
	}

	@Test
	void loadOtherSettingDefaults() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		Method m = AvalancheVSSPFMode.class.getDeclaredMethod("loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, e, new CustomProperties());
		assertEquals(120, readIntArray(mode, "ojamaRate", 0));
		assertEquals(3, readIntArray(mode, "ojamaCountdown", 0));
		assertEquals(4, readIntArray(mode, "ojamaHard", 0));
	}

	@Test
	void saveReplayWritesSettings() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setIntArray(mode, "ojamaCountdown", 0, 7);
		// saveReplay saves to owner.replayProp, not the parameter prop
		CustomProperties prop = new CustomProperties();
		mode.owner.replayProp = prop;
		mode.saveReplay(e, 0, prop);
		assertEquals(7, prop.getProperty("avalanchevsspf.ojamaCountdown.p0", -1));
	}

	@Test
	void readyInitWiresDropPattern() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded();
		mode.readyInit(e, 0);
		assertEquals(4, e.numColors);
	}

	@Test
	void getAttackMultiplierNormal() {
		double mult = AvalancheVSSPFMode.getAttackMultiplier(0, 0);
		assertEquals(1.0, mult, 0.001);
	}

	@Test
	void getAttackMultiplierOutOfBounds() {
		double mult = AvalancheVSSPFMode.getAttackMultiplier(99, 99);
		assertEquals(1.0, mult, 0.001);
	}

	@Test
	void getDefendMultiplierNormal() {
		double mult = AvalancheVSSPFMode.getDefendMultiplier(0, 0);
		assertEquals(1.0, mult, 0.001);
	}

	@Test
	void getDefendMultiplierOutOfBounds() {
		double mult = AvalancheVSSPFMode.getDefendMultiplier(99, 99);
		assertEquals(1.0, mult, 0.001);
	}

	@Test
	void renderSettingWaitState() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.statc[4] = 1;
		mode.renderSetting(e, 0);
	}

	@Test
	void onMoveResetsFlags() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setBooleanArray(mode, "cleared", 0, true);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		setBooleanArray(mode, "countdownDecremented", 0, true);
		mode.onMove(e, 0);
		assertFalse(readBooleanArray(mode, "cleared", 0));
		assertFalse(readBooleanArray(mode, "ojamaDrop", 0));
		assertFalse(readBooleanArray(mode, "countdownDecremented", 0));
	}

	@Test
	void onClearResetsOjamaChecked() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setBooleanArray(mode, "ojamaChecked", 0, true);
		mode.onClear(e, 0);
		assertFalse(readBooleanArray(mode, "ojamaChecked", 0));
	}

	// ---- helper ----

	private static GameEngine freshEngine(AvalancheVSSPFMode mode) {
		GameManager m = new GameManager(new EventReceiver());
		m.mode = mode;
		m.mode = mode;
		mode.modeInit(m);
		m.init();
		m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10;
		m.engine[0].ruleopt.fieldHeight = 20;
		m.engine[0].ruleopt.fieldHiddenHeight = 4;
		return m.engine[0];
	}

	private static int readIntArray(Object o, String n, int i) throws Exception {
		return ((int[]) findField(o.getClass(), n).get(o))[i];
	}

	private static boolean readBooleanArray(Object o, String n, int i) throws Exception {
		return ((boolean[]) findField(o.getClass(), n).get(o))[i];
	}

	private static void setIntArray(Object o, String n, int i, int v) throws Exception {
		((int[]) findField(o.getClass(), n).get(o))[i] = v;
	}

	private static void setBooleanArray(Object o, String n, int i, boolean v) throws Exception {
		((boolean[]) findField(o.getClass(), n).get(o))[i] = v;
	}

	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws Exception {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try { java.lang.reflect.Field f = c.getDeclaredField(name); f.setAccessible(true); return f; } catch (NoSuchFieldException e) { /* continue */ }
		}
		throw new NoSuchFieldException(name);
	}
}
