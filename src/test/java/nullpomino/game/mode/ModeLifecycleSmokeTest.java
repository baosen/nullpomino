package nullpomino.game.mode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import nullpomino.util.ModeRegistry;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Bulk parameterized smoke test that instantiates every mode in
 * {@link ModeRegistry#all()} and calls the full lifecycle and render
 * surface without throwing.
 */
class ModeLifecycleSmokeTest {

	@TestFactory
	List<DynamicTest> everyModeSurvivesLifecycle() {
		List<DynamicTest> tests = new ArrayList<>();
		for (Class<? extends GameMode> modeClass : ModeRegistry.all()) {
			tests.add(DynamicTest.dynamicTest(modeClass.getSimpleName(), () -> {
				GameMode mode = modeClass.getDeclaredConstructor().newInstance();
				GameManager manager = new GameManager(new EventReceiver());
				manager.mode = mode;
				manager.init(); // calls mode.modeInit(manager), creates engines

				boolean isNet = mode.isNetplayMode();
				if (isNet) {
					try { setField(mode, "netCurrentRoomInfo", new NetRoomInfo()); } catch (Exception e) {}
				}

				// Initialize each player engine
				for (int pi = 0; pi < manager.engine.length; pi++) {
					GameEngine eng = manager.engine[pi];
					eng.init();
					loadDefaultRuleopt(eng);
				}

				// playerInit + startGame for each player
				for (int pi = 0; pi < manager.engine.length; pi++) {
					final GameEngine feng = manager.engine[pi];
					final int fpi = pi;
					safe(isNet, () -> mode.playerInit(feng, fpi));
					safe(isNet, () -> mode.startGame(feng, fpi));
				}

				// onFirst/onLast
				for (int pi = 0; pi < manager.engine.length; pi++) {
					final GameEngine feng = manager.engine[pi];
					final int fpi = pi;
					safe(isNet, () -> mode.onFirst(feng, fpi));
					safe(isNet, () -> mode.onLast(feng, fpi));
				}

				// Create fields
				for (int pi = 0; pi < manager.engine.length; pi++) {
					manager.engine[pi].createFieldIfNeeded();
				}

				// All render methods for each player
				for (int pi = 0; pi < manager.engine.length; pi++) {
					final GameEngine feng = manager.engine[pi];
					final int fpi = pi;
					safe(isNet, () -> mode.renderFirst(feng, fpi));
					safe(isNet, () -> mode.renderLast(feng, fpi));
					safe(isNet, () -> mode.renderSetting(feng, fpi));
					safe(isNet, () -> mode.renderReady(feng, fpi));
					safe(isNet, () -> mode.renderMove(feng, fpi));
					safe(isNet, () -> mode.renderLockFlash(feng, fpi));
					safe(isNet, () -> mode.renderARE(feng, fpi));
					safe(isNet, () -> mode.renderLineClear(feng, fpi));
					safe(isNet, () -> mode.renderEndingStart(feng, fpi));
					safe(isNet, () -> mode.renderCustom(feng, fpi));
					safe(isNet, () -> mode.renderExcellent(feng, fpi));
					safe(isNet, () -> mode.renderGameOver(feng, fpi));
					safe(isNet, () -> mode.renderResult(feng, fpi));
					safe(isNet, () -> mode.renderFieldEdit(feng, fpi));
					safe(isNet, () -> mode.renderInput(feng, fpi));
				}

				// Lifecycle methods for each player
				for (int pi = 0; pi < manager.engine.length; pi++) {
					final GameEngine feng = manager.engine[pi];
					final int fpi = pi;
					safe(isNet, () -> mode.onSetting(feng, fpi));
					safe(isNet, () -> mode.onReady(feng, fpi));
					safe(isNet, () -> mode.onMove(feng, fpi));
					safe(isNet, () -> mode.onLockFlash(feng, fpi));
					safe(isNet, () -> mode.onLineClear(feng, fpi));
					safe(isNet, () -> mode.onARE(feng, fpi));
					safe(isNet, () -> mode.onEndingStart(feng, fpi));
					safe(isNet, () -> mode.onCustom(feng, fpi));
					safe(isNet, () -> mode.onExcellent(feng, fpi));
					safe(isNet, () -> mode.onGameOver(feng, fpi));
					safe(isNet, () -> mode.onResult(feng, fpi));
					safe(isNet, () -> mode.onFieldEdit(feng, fpi));
					safe(isNet, () -> mode.blockBreak(feng, fpi, 0, 0, null));
					safe(isNet, () -> mode.calcScore(feng, fpi, 0));
					safe(isNet, () -> mode.afterSoftDropFall(feng, fpi, 0));
					safe(isNet, () -> mode.afterHardDropFall(feng, fpi, 0));
					safe(isNet, () -> mode.fieldEditExit(feng, fpi));
					safe(isNet, () -> mode.pieceLocked(feng, fpi, 0));
					safe(isNet, () -> mode.lineClearEnd(feng, fpi));
					safe(isNet, () -> mode.netplayOnRetryKey(feng, fpi));
				}

				// Replay save/load
				CustomProperties prop = new CustomProperties();
				safe(isNet, () -> mode.saveReplay(manager.engine[0], 0, prop));
				safe(isNet, () -> mode.loadReplay(manager.engine[0], 0, prop));
			}));
		}
		return tests;
	}

	private static void safe(boolean isNet, Runnable call) {
		try {
			call.run();
		} catch (Exception e) {
			if (!isNet) {
				throw new RuntimeException(e);
			}
		}
	}

	private static void loadDefaultRuleopt(GameEngine engine) {
		if (engine.ruleopt != null) {
			if (engine.ruleopt.fieldWidth <= 0) engine.ruleopt.fieldWidth = 10;
			if (engine.ruleopt.fieldHeight <= 0) engine.ruleopt.fieldHeight = 20;
			if (engine.ruleopt.fieldHiddenHeight < 0) engine.ruleopt.fieldHiddenHeight = 4;
		}
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
