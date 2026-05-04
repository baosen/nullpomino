package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;

class AvalancheVSDigRaceModeBranchCoverageTest {

	@Test void onReadyOutlineTypeNormal() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setIntArray(mode, "outlineType", 0, 0);
		mode.onReady(e, 0);
		assertEquals(GameEngine.BLOCK_OUTLINE_NORMAL, e.blockOutlineType);
	}
	@Test void onReadyOutlineTypeNone() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setIntArray(mode, "outlineType", 0, 2);
		mode.onReady(e, 0);
		assertEquals(GameEngine.BLOCK_OUTLINE_NONE, e.blockOutlineType);
	}
	@Test void renderLastGameActiveCheck() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode); e.gameActive = false;
		mode.renderLast(e, 0);
	}
	@Test void onLastDecrementsScgettime() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setIntArray(mode, "scgettime", 0, 5);
		mode.onLast(e, 0);
		assertEquals(4, readIntArray(mode, "scgettime", 0));
	}
	@Test void loadOtherSettingDefaults() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		loadSetting(mode, e, new CustomProperties());
		assertEquals(420, readIntArray(mode, "ojamaRate", 0));
		assertEquals(6, readIntArray(mode, "handicapRows", 0));
	}
	@Test void saveReplayWritesHandicap() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine e = freshEngine(mode);
		setIntArray(mode, "handicapRows", 0, 4);
		CustomProperties prop = new CustomProperties();
		mode.saveReplay(e, 0, prop);
		assertEquals(4, prop.getProperty("avalanchevsdigrace.ojamaHandicap.p0", -1));
	}

	private static GameEngine freshEngine(AvalancheVSDigRaceMode mode) {
		GameManager m = new GameManager(new EventReceiver()); m.mode = mode; m.init(); m.engine[0].init(); return m.engine[0];
	}
	private static int readIntArray(Object o, String n, int i) throws Exception {
		return ((int[]) readField(o, n))[i];
	}
	private static void setIntArray(Object o, String n, int i, int v) throws Exception {
		((int[]) readField(o, n))[i] = v;
	}
	private static Object readField(Object o, String n) throws Exception {
		java.lang.reflect.Field f = findField(o.getClass(), n); f.setAccessible(true); return f.get(o);
	}
	private static void loadSetting(AvalancheVSDigRaceMode mode, GameEngine e, CustomProperties p) throws Exception {
		java.lang.reflect.Method m = AvalancheVSDigRaceMode.class.getDeclaredMethod("loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true); m.invoke(mode, e, p);
	}
	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try { return c.getDeclaredField(name); } catch (NoSuchFieldException e) { /* continue */ }
		}
		throw new NoSuchFieldException(name);
	}
}
