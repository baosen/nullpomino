package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;

class AvalancheVSFeverModeBranchCoverageTest {

	@Test void addOjamaHurryupActive() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setIntArray(mode, "hurryupSeconds", 0, 30);
		setIntArray(mode, "ojamaRate", 0, 120);
		e.statistics.time = 3601;
		Method m = AvalancheVSFeverMode.class.getDeclaredMethod("addOjama", GameEngine.class, int.class, int.class);
		m.setAccessible(true); m.invoke(mode, e, 0, 100);
		assertEquals(4, readIntArray(mode, "ojamaSent", 0));
	}
	@Test void getChainColorFeverSizeGreen() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setIntArray(mode, "chainDisplayType", 0, AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE);
		setIntArray(mode, "feverChainDisplay", 0, 6); e.chain = 8;
		assertEquals(EventReceiver.COLOR_GREEN, mode.getChainColor(e, 0));
	}
	@Test void getChainColorFeverSizeRed() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setIntArray(mode, "chainDisplayType", 0, AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE);
		setIntArray(mode, "feverChainDisplay", 0, 6); e.chain = 2;
		assertEquals(EventReceiver.COLOR_RED, mode.getChainColor(e, 0));
	}
	@Test void lineClearEndZenkeshiFever() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		setBooleanArray(mode, "cleared", 0, true);
		setBooleanArray(mode, "zenKeshi", 0, true);
		setIntArray(mode, "zenKeshiType", 0, AvalancheVSDummyMode.ZENKESHI_MODE_FEVER);
		setIntArray(mode, "feverChain", 0, 5);
		setIntArray(mode, "feverChainMin", 0, 3);
		setIntArray(mode, "feverChainMax", 0, 15);
		setIntArray(mode, "ojamaAdd", 1, 0); e.chain = 2;
		mode.lineClearEnd(e, 0);
		assertEquals(5, readIntArray(mode, "feverChain", 0));
	}
	@Test void lineClearEndGameOver() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.createFieldIfNeeded();
		e.field.setBlockColor(2, 0, Block.BLOCK_COLOR_RED);
		setIntArray(mode, "ojama", 0, 0);
		setBooleanArray(mode, "ojamaDrop", 0, false);
		setBooleanArray(mode, "cleared", 0, false);
		setIntArray(mode, "ojamaAdd", 1, 0);
		mode.lineClearEnd(e, 0);
		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}
	@Test void calcChainNewPowerOverflowUsesLast() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		assertEquals(720, mode.calcChainNewPower(freshEngine(mode), 0, 25));
	}
	@Test void saveReplayWritesSettings() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setIntArray(mode, "ojamaHandicap", 0, 500);
		setIntArray(mode, "feverChainStart", 0, 7);
		CustomProperties prop = new CustomProperties();
		mode.saveReplay(e, 0, prop);
		assertEquals(500, prop.getProperty("avalanchevsfever.ojamaHandicap.p0", -1));
	}

	private static GameEngine freshEngine(AvalancheVSFeverMode mode) {
		GameManager m = new GameManager(new EventReceiver()); m.mode = mode; m.init(); m.engine[0].init(); return m.engine[0];
	}
	private static int readIntArray(Object o, String n, int i) throws Exception {
		return ((int[]) readField(o, n))[i];
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
