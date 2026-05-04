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

class AvalancheVSBombBattleModeBranchCoverageTest {

	@Test void renderLastHardBlockDisplay() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded(); e.gameStarted = true;
		Block b = new Block(Block.BLOCK_COLOR_GRAY); b.hard = 3;
		e.field.setBlock(2, 3, b);
		mode.renderLast(e, 0);
	}
	@Test void renderLastCountdownBlockDisplay() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded(); e.gameStarted = true;
		Block b = new Block(Block.BLOCK_COLOR_GRAY); b.countdown = 5;
		e.field.setBlock(3, 4, b);
		mode.renderLast(e, 0);
	}
	@Test void renderLastOjamaAddDisplay() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setIntArray(mode, "ojama", 0, 5); setIntArray(mode, "ojamaAdd", 0, 8);
		e.gameStarted = true;
		mode.renderLast(e, 0);
	}
	@Test void lineClearEndBombExplosion() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		setIntArray(mode, "ojama", 0, 0);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		setIntArray(mode, "ojamaAdd", 1, 0);
		Block b = new Block(Block.BLOCK_COLOR_GRAY); b.countdown = 1;
		e.field.setBlock(4, 3, b);
		mode.lineClearEnd(e, 0);
	}
	@Test void lineClearEndCountdownDecrement() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		setIntArray(mode, "ojama", 0, 0);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		setIntArray(mode, "ojamaAdd", 1, 0);
		Block b = new Block(Block.BLOCK_COLOR_GRAY); b.countdown = 3;
		e.field.setBlock(4, 3, b);
		mode.lineClearEnd(e, 0);
		assertEquals(2, e.field.getBlock(4, 3).countdown);
	}
	@Test void lineClearEndZenkeshiFever() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		setBooleanArray(mode, "zenKeshi", 0, true);
		setIntArray(mode, "zenKeshiType", 0, AvalancheVSDummyMode.ZENKESHI_MODE_FEVER);
		setIntArray(mode, "ojama", 0, 0);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		setIntArray(mode, "ojamaAdd", 1, 0);
		mode.lineClearEnd(e, 0);
		assertFalse(readBooleanArray(mode, "zenKeshi", 0));
	}
	@Test void lineClearEndOjamaDrop() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		setIntArray(mode, "ojama", 0, 20);
		setBooleanArray(mode, "ojamaDrop", 0, false);
		setBooleanArray(mode, "cleared", 0, false);
		setIntArray(mode, "maxAttack", 0, 2);
		setIntArray(mode, "ojamaHard", 0, 0);
		setIntArray(mode, "ojamaAdd", 1, 0);
		assertTrue(mode.lineClearEnd(e, 0));
		assertEquals(8, readIntArray(mode, "ojama", 0));
	}
	@Test void lineClearEndGameOver() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		setIntArray(mode, "ojama", 0, 0);
		setBooleanArray(mode, "ojamaDrop", 0, true);
		setIntArray(mode, "ojamaAdd", 1, 0);
		e.field.setBlockColor(2, 0, Block.BLOCK_COLOR_RED);
		mode.lineClearEnd(e, 0);
		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}
	@Test void explodeChainReaction() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		Block b = new Block(Block.BLOCK_COLOR_GRAY); b.countdown = 0;
		e.field.setBlock(4, 3, b);
		Method m = AvalancheVSBombBattleMode.class.getDeclaredMethod("explode", GameEngine.class, int.class, int.class, int.class);
		m.setAccessible(true); m.invoke(mode, e, 0, 4, 3);
	}
	@Test void explodeNullBlock() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		Method m = AvalancheVSBombBattleMode.class.getDeclaredMethod("explode", GameEngine.class, int.class, int.class, int.class);
		m.setAccessible(true); m.invoke(mode, e, 0, -1, -1);
	}
	@Test void loadOtherSettingDefaults() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		Method m = AvalancheVSBombBattleMode.class.getDeclaredMethod("loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true); m.invoke(mode, e, new CustomProperties());
		assertEquals(60, readIntArray(mode, "ojamaRate", 0));
		assertEquals(5, readIntArray(mode, "ojamaCountdown", 0));
	}
	@Test void saveReplayWritesSettings() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setBooleanArray(mode, "newChainPower", 0, true);
		setIntArray(mode, "ojamaCountdown", 0, 7);
		CustomProperties prop = new CustomProperties();
		mode.saveReplay(e, 0, prop);
		assertTrue(prop.getProperty("avalanchevsbombbattle.newChainPower.p0", false));
		assertEquals(7, prop.getProperty("avalanchevsbombbattle.ojamaCountdown.p0", -1));
	}
	@Test void updateOjamaMeterNullField() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.field = null;
		setIntArray(mode, "ojama", 0, 36);
		Method m = AvalancheVSBombBattleMode.class.getDeclaredMethod("updateOjamaMeter", GameEngine.class, int.class);
		m.setAccessible(true); m.invoke(mode, e, 0);
	}

	private static GameEngine freshEngine(AvalancheVSBombBattleMode mode) {
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
