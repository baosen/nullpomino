package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;

class AvalancheVSSPFModeBranchCoverageTest {

	@Test void getAttackMultiplierOutOfBoundsReturnsOne() {
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(99, 99), 0.001);
	}
	@Test void getDefendMultiplierOutOfBoundsReturnsOne() {
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(99, 99), 0.001);
	}
	@Test void onLineClearConvertsGarbageBlocks() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		Block g = new Block(Block.BLOCK_COLOR_GRAY);
		g.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true); g.hard = 3; g.secondaryColor = Block.BLOCK_COLOR_RED;
		e.field.setBlock(2, 0, g);
		mode.onLineClear(e, 0);
		assertFalse(e.field.getBlock(2, 0).getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE));
	}
	@Test void onLineClearNullFieldSkips() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.field = null;
		assertFalse(mode.onLineClear(e, 0));
	}
	@Test void lineClearEndCountdownDecrements() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		setIntArray(mode, "ojamaCountdown", 0, 3);
		setBooleanArray(mode, "countdownDecremented", 0, false);
		setIntArray(mode, "ojama", 0, 0);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		setIntArray(mode, "ojamaAdd", 1, 0);
		Block b = new Block(Block.BLOCK_COLOR_GRAY); b.countdown = 3;
		b.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true); b.hard = 4; b.secondaryColor = Block.BLOCK_COLOR_RED;
		e.field.setBlock(2, 0, b);
		mode.lineClearEnd(e, 0);
		assertEquals(2, e.field.getBlock(2, 0).countdown);
	}
	@Test void lineClearEndOjamaDrop() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		setIntArray(mode, "ojama", 0, 30);
		setBooleanArray(mode, "ojamaDrop", 0, false);
		setBooleanArray(mode, "cleared", 0, false);
		setIntArray(mode, "maxAttack", 0, 10);
		setIntArray(mode, "ojamaCountdown", 0, 3);
		setIntArray(mode, "ojamaAdd", 1, 0);
		setBooleanArray(mode, "countdownDecremented", 0, true);
		int[][] pat = new int[][] {{1, 2, 3, 4}};
		((int[][][]) readField(mode, "dropPattern"))[1] = pat;
		assertTrue(mode.lineClearEnd(e, 0));
		assertEquals(20, readIntArray(mode, "ojama", 0));
	}
	@Test void lineClearEndGameOver() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		setBooleanArray(mode, "countdownDecremented", 0, true);
		setIntArray(mode, "ojama", 0, 0);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		setIntArray(mode, "ojamaAdd", 1, 0);
		e.field.setBlockColor(2, 0, Block.BLOCK_COLOR_RED);
		mode.lineClearEnd(e, 0);
		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}
	@Test void onMoveResetsFlags() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setBooleanArray(mode, "cleared", 0, true);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		assertFalse(mode.onMove(e, 0));
		assertFalse(readBooleanArray(mode, "cleared", 0));
		assertFalse(readBooleanArray(mode, "ojamaDrop", 0));
	}
	@Test void onClearResetsOjamaChecked() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setBooleanArray(mode, "ojamaChecked", 0, true);
		Method m = AvalancheVSSPFMode.class.getDeclaredMethod("onClear", GameEngine.class, int.class);
		m.setAccessible(true); m.invoke(mode, e, 0);
		assertFalse(readBooleanArray(mode, "ojamaChecked", 0));
	}
	@Test void loadDropMapPreviewNullPattern() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		Method m = AvalancheVSSPFMode.class.getDeclaredMethod("loadDropMapPreview", GameEngine.class, int.class, int[][].class);
		m.setAccessible(true); m.invoke(mode, e, 0, (int[][]) null);
	}
	@Test void saveReplayWritesSettings() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		e.owner.replayProp = new CustomProperties();
		setIntArray(mode, "dropSet", 0, 2);
		setIntArray(mode, "dropMap", 0, 3);
		setIntArray(mode, "ojamaCountdown", 0, 7);
		mode.saveReplay(e, 0, new CustomProperties());
		assertEquals(2, e.owner.replayProp.getProperty("avalanchevsspf.dropSet.p0", -1));
		assertEquals(3, e.owner.replayProp.getProperty("avalanchevsspf.dropMap.p0", -1));
		assertEquals(7, e.owner.replayProp.getProperty("avalanchevsspf.ojamaCountdown.p0", -1));
	}

	private static GameEngine freshEngine(AvalancheVSSPFMode mode) {
		GameManager m = new GameManager(new EventReceiver()); m.mode = mode; m.init(); m.engine[0].init(); return m.engine[0];
	}
	private static int readIntArray(Object o, String n, int i) throws Exception {
		return ((int[]) readField(o, n))[i];
	}
	private static boolean readBooleanArray(Object o, String n, int i) throws Exception {
		return ((boolean[]) readField(o, n))[i];
	}
	private static void setIntArray(Object o, String n, int i, int v) throws Exception {
		((int[]) readField(o, n))[i] = v;
	}
	private static void setBooleanArray(Object o, String n, int i, boolean v) throws Exception {
		((boolean[]) readField(o, n))[i] = v;
	}
	private static Object readField(Object o, String n) throws Exception {
		java.lang.reflect.Field f = findField(o.getClass(), n); f.setAccessible(true); return f.get(o);
	}
	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try { return c.getDeclaredField(name); } catch (NoSuchFieldException e) { /* continue */ }
		}
		throw new NoSuchFieldException(name);
	}
}
